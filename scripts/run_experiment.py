#!/usr/bin/env python3
"""A/B 토큰 실험 러너.

케이스마다 A(issue 아카이브 사본 제공) / B(제공 없음)로 `claude -p`를 격리 HOME(하네스 없음)으로 실행하고,
결과(usage·비용·판정·감사)를 results/runs.jsonl 에 한 줄씩 append 한다.

사용:
  python3 scripts/run_experiment.py --archive-src /home/jun/project/study-note/issue \
      [--cases 01,02] [--arms A,B] [--reps 1,2,3] [--parallel 5] [--model sonnet]
이미 runs.jsonl 에 (case, arm, rep) 유효 행이 있으면 건너뛴다(재개 가능). 무효(감사 위반) 행은 재실행 대상.
"""
import argparse
import concurrent.futures as cf
import fcntl
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CASES = ROOT / "cases"
ORACLE = ROOT / "oracle"
RESULTS = ROOT / "results"
RAW = RESULTS / "raw"
RUNS_FILE = RESULTS / "runs.jsonl"


def set_dirs(cases: str, oracle: str, results: str) -> None:
    """2차 실험(cases-v2 등)용으로 입출력 디렉터리를 바꾼다."""
    global CASES, ORACLE, RESULTS, RAW, RUNS_FILE
    CASES, ORACLE, RESULTS = ROOT / cases, ROOT / oracle, ROOT / results
    RAW, RUNS_FILE = RESULTS / "raw", RESULTS / "runs.jsonl"
HOST_RUNS = Path("/tmp/cia-runs")
REAL_HOME = Path.home()
CREDS = REAL_HOME / ".claude" / ".credentials.json"

# 실행 경로: 작업 폴더는 실행마다 /tmp/cia-runs/<run_id>/work, A 아카이브 사본은 /tmp/cia-lib/issue
# (bwrap 은 AppArmor 비특권 userns 제한으로 불가 → 권한 거부 규칙 + 로그 감사로 통제)
ARCHIVE_DIR = Path("/tmp/cia-lib")  # 3차는 --archive-dir /tmp/cia-lib3 (가상 회사 아카이브)
SB_ARCHIVE = str(ARCHIVE_DIR / "issue")


def set_archive(d: str) -> None:
    global ARCHIVE_DIR, SB_ARCHIVE
    ARCHIVE_DIR = Path(d)
    SB_ARCHIVE = str(ARCHIVE_DIR / "issue")

PROMPT_BASE = (
    "현재 디렉터리의 프로젝트에 버그 보고가 들어왔다. TASK.md 를 읽고 문제의 근본 원인을 찾아 고쳐라.\n"
    "완료 기준: 현재 디렉터리에서 `bash test.sh` 가 통과한다. 테스트 파일과 test.sh 는 수정하지 마라.\n"
    "작업은 현재 디렉터리 안에서만 하라. 끝나면 원인과 수정 내용을 3줄 이내로 요약하라."
)
PROMPT_ARCHIVE = (
    "\n\n참고 자료: 과거 실전 이슈의 원인과 해결책을 정리한 아카이브가 {archive} 에 있다"
    " (README.md 가 인덱스). 관련 카드를 스스로 찾아 활용해도 된다."
)
# 추가 실험 arm F(archive-first): 아카이브를 먼저 찾고, 없거나 맞지 않을 때만 직접 조사
PROMPT_ARCHIVE_FIRST = (
    "\n\n작업 규칙: 과거 실전 이슈의 원인과 해결책을 정리한 아카이브가 {archive} 에 있다 (README.md 가 인덱스)."
    " 코드를 고치기 전에 **먼저 이 아카이브에서 관련 카드를 찾아 읽어라.** 관련 카드가 없거나 맞지 않을 때만 직접 조사하라."
)
# 2차 arm P(A′): 정답 카드 경로를 직접 준다 (찾기 비용 0 — 카드 내용 가치의 상한)
PROMPT_CARD = (
    "\n\n작업 규칙: 이 문제와 관련된 과거 실전 이슈 카드가 {card_dir} 에 있다"
    " (1-question.md, 2-summary.md, 3-answer.md). 코드를 고치기 전에 **먼저 이 카드를 읽어라.**"
)
# 추가 실험 arm L(P-lite): 검색·요약이 완벽한 MCP의 상한 — 정답 카드 결론 2~3줄만 준다 (아카이브 접근은 B와 같이 금지)
PROMPT_SUMMARY = "\n\n참고: 이 문제와 관련될 수 있는 과거 실전 이슈의 결론 요약이다.\n---\n{summary}\n---"
ARM_PROMPTS = {"A": PROMPT_ARCHIVE, "F": PROMPT_ARCHIVE_FIRST, "B": ""}


def card_path(case: str) -> str:
    """oracle card.txt 의 첫 issue/ 경로 → 아카이브 사본 안 카드 폴더 (존재 확인 — 없으면 예외)."""
    m = re.search(r"issue/([\w/.-]+)", (ORACLE / case / "card.txt").read_text())
    if not m:
        raise ValueError(f"{case}: card.txt 에 issue/ 경로 없음")
    d = ARCHIVE_DIR / "issue" / m.group(1).strip("/.")
    if not (d / "3-answer.md").is_file():
        raise ValueError(f"{case}: 카드 폴더 없음 {d}")
    return str(d)


def arm_prompt(arm: str, case: str) -> str:
    if arm == "P":
        return PROMPT_CARD.format(card_dir=card_path(case))
    if arm == "L":
        summary = (ORACLE / case / "summary.txt").read_text().strip()
        if not summary:
            raise ValueError(f"{case}: summary.txt 비어 있음")
        return PROMPT_SUMMARY.format(summary=summary)
    return ARM_PROMPTS[arm].format(archive=SB_ARCHIVE)
CARD_FILE = re.compile(r"cia-lib\d*/issue/\S*(1-question|2-summary|3-answer)\.md")

# 감사: 실행 로그에 나타나면 무효로 처리할 패턴 (원본 아카이브·정답·웹 우회)
AUDIT_COMMON = [r"study-note", r"junhyeong9812", r"oracle/", r"compare-issue-archive", r"fix\.patch", r"hidden\.sh",
                re.escape(str(REAL_HOME / "project")), r"/tmp/claude-\d+"]
AUDIT_B_ONLY = [r"cia-lib"]
DENY_RULES = [f"{tool}(/{REAL_HOME}/project/**)" for tool in ("Read", "Edit", "Write", "Grep", "Glob")] + \
             [f"{tool}(//tmp/claude-*/**)" for tool in ("Read", "Grep", "Glob")]

PROTECTED = re.compile(r"(^|/)(TASK\.md|test\.sh|tsconfig\.json|test_[^/]*\.py|[^/]*\.test\.ts)$|(^|/)(tests?|fixtures)/")

_lock_path = HOST_RUNS / ".lock"


def sha(p: Path) -> str:
    return hashlib.sha256(p.read_bytes()).hexdigest()


def creds_expiry(p: Path) -> float:
    try:
        return json.loads(p.read_text())["claudeAiOauth"]["expiresAt"] / 1000
    except Exception:
        return 0.0


def wait_fresh_creds(min_left_s: int = 45 * 60) -> None:
    """원본 토큰 만료가 가까우면 샌드박스가 갱신(회전)하지 않도록 원본이 갱신될 때까지 기다린다."""
    while creds_expiry(CREDS) - time.time() < min_left_s:
        left = creds_expiry(CREDS) - time.time()
        print(f"[creds] 만료까지 {left/60:.0f}분 — 원본 갱신 대기(일반 claude 세션이 갱신)", flush=True)
        if left < 0:
            # 만료됨: 일반 HOME 으로 짧은 호출을 보내 갱신을 유도한다
            subprocess.run(["claude", "-p", "ok", "--model", "haiku", "--setting-sources", "project"],
                           cwd=tempfile.gettempdir(), capture_output=True, timeout=120)
        time.sleep(60)


def writeback_creds(sb_creds: Path) -> bool:
    """샌드박스 안에서 토큰이 갱신됐으면(만료가 더 늦으면) 원본에 원자적으로 되쓴다."""
    with open(_lock_path, "w") as lk:
        fcntl.flock(lk, fcntl.LOCK_EX)
        if creds_expiry(sb_creds) > creds_expiry(CREDS):
            tmp = CREDS.with_suffix(".tmp-cia")
            shutil.copy2(sb_creds, tmp)
            os.chmod(tmp, 0o600)
            os.replace(tmp, CREDS)
            return True
    return False


def claude_cmd(prompt: str, model: str, budget: str) -> list:
    return [
        str(REAL_HOME / ".local" / "bin" / "claude"), "-p", prompt,
        "--model", model,
        "--output-format", "stream-json", "--verbose",
        "--max-budget-usd", budget,
        "--permission-mode", "bypassPermissions",
    ]


def sandbox_env(sb_home: Path) -> dict:
    env = {k: v for k, v in os.environ.items()
           if not k.startswith(("CLAUDE", "ANTHROPIC")) and k not in ("OLDPWD", "PWD")}
    env.update({"HOME": str(sb_home), "DISABLE_AUTOUPDATER": "1"})
    return env


def audit(log_text: str, arm: str, run_id: str = "") -> list:
    # 다른 실행의 작업 폴더를 들여다본 흔적도 위반(동시 실행 간 오염)
    # 실행 ID 형태(NN-ARM-rN-ts)의 다른 실행 폴더만 위반. 공유 상위 폴더에 임시 파일을 쓰는 것(예: /tmp/cia-runs/run.log)은 열람이 아님
    other_runs = (rf"cia-runs/(?!{re.escape(run_id)}\b)\d\d-[A-Z]-r\d+-\d+" if run_id
                  else r"cia-runs/\d\d-[A-Z]-r\d+-\d+")
    pats = AUDIT_COMMON + [other_runs] + (AUDIT_B_ONLY if arm in ("B", "L") else [])
    # Claude Code 권한 가드의 거부 메시지가 deny 규칙 문자열을 인용하는 것은 접근 시도가 아니다(오탐 제거)
    log_text = re.sub(r"a deny rule \(//[^)]*\)", "a deny rule (<rule>)", log_text)
    # 실행 안 Claude Code가 자기 백그라운드 작업 출력을 두는 경로(/tmp/claude-<uid>/-tmp-cia-runs-<자기 run_id>-work/...)는 열람이 아님
    if run_id:
        log_text = re.sub(rf"/tmp/claude-\d+/-tmp-cia-runs-{re.escape(run_id)}-work/", "<own-task-dir>/", log_text)
    return sorted({p for p in pats if re.search(p, log_text)})


def parse_log(log_path: Path) -> dict:
    out = {"result": None, "tool_calls": 0, "tools": {}, "archive_reads": 0, "card_reads": 0,
           "first_edit_turn": None, "archive_result_bytes": 0}
    turn = 0
    archive_ids = set()
    seen_msgs = set()
    for line in log_path.read_text(errors="replace").splitlines():
        try:
            ev = json.loads(line)
        except json.JSONDecodeError:
            continue
        if ev.get("type") == "assistant":
            # stream-json 은 한 응답의 content 블록마다 assistant 이벤트를 내므로 message id 단위로 턴을 센다
            mid = ev.get("message", {}).get("id")
            if mid not in seen_msgs:
                seen_msgs.add(mid)
                turn += 1
            for blk in ev.get("message", {}).get("content", []) or []:
                if blk.get("type") in ("tool_use", "server_tool_use"):
                    out["tool_calls"] += 1
                    name = blk.get("name", "?")
                    out["tools"][name] = out["tools"].get(name, 0) + 1
                    inp = json.dumps(blk.get("input", {}), ensure_ascii=False)
                    if str(ARCHIVE_DIR) in inp:
                        out["archive_reads"] += 1
                        archive_ids.add(blk.get("id"))
                    edit_like = name in ("Edit", "Write", "MultiEdit") or (
                        name.lower() == "bash" and re.search(r"\bsed -i|\bpatch\b|> *[\w./-]+\.(java|py|ts|sh)\b|python3? - <<", inp))
                    if edit_like and out["first_edit_turn"] is None:
                        out["first_edit_turn"] = turn
                    if CARD_FILE.search(inp):
                        out["card_reads"] += 1
        elif ev.get("type") == "user":
            content = ev.get("message", {}).get("content", [])
            for blk in content if isinstance(content, list) else []:
                if blk.get("type") == "tool_result" and blk.get("tool_use_id") in archive_ids:
                    out["archive_result_bytes"] += len(json.dumps(blk.get("content"), ensure_ascii=False).encode())
        elif ev.get("type") == "result":
            out["result"] = ev
    return out


def judge(case: str, work: Path) -> dict:
    """변조 검사 + hidden 덮어쓰기 후 test.sh·hidden.sh 판정 (작업 폴더 사본에서)."""
    orig = CASES / case
    tampered = []
    for f in orig.rglob("*"):
        if f.is_file():
            rel = f.relative_to(orig).as_posix()
            if PROTECTED.search(rel):
                w = work / rel
                if not w.is_file() or sha(w) != sha(f):
                    tampered.append(rel)
    jd = Path(tempfile.mkdtemp(prefix="judge-", dir=HOST_RUNS))
    try:
        shutil.copytree(work, jd, dirs_exist_ok=True)
        # 변조 여부와 무관하게 원본 보호 파일로 복원한 뒤 판정한다
        for rel in tampered:
            src = orig / rel
            if src.is_file():
                (jd / rel).parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, jd / rel)
        shutil.copytree(ORACLE / case / "hidden", jd, dirs_exist_ok=True)
        t = subprocess.run(["bash", "test.sh"], cwd=jd, capture_output=True, timeout=600)
        h = subprocess.run(["bash", "hidden.sh"], cwd=jd, capture_output=True, timeout=600)
        return {"tampered": tampered, "test_pass": t.returncode == 0, "hidden_pass": h.returncode == 0,
                "hidden_tail": h.stdout.decode(errors="replace")[-400:] if h.returncode else ""}
    finally:
        shutil.rmtree(jd, ignore_errors=True)


def append_row(row: dict) -> None:
    with open(_lock_path, "w") as lk:
        fcntl.flock(lk, fcntl.LOCK_EX)
        with open(RUNS_FILE, "a", encoding="utf-8") as f:
            f.write(json.dumps(row, ensure_ascii=False) + "\n")


def run_one(case: str, arm: str, rep: int, model: str, budget: str, timeout_s: int) -> dict:
    run_id = f"{case[:2]}-{arm}-r{rep}-{int(time.time())}"
    run_dir = HOST_RUNS / run_id
    sb_home = run_dir / "home"
    (sb_home / ".claude").mkdir(parents=True)
    (sb_home / ".sdkman").symlink_to(REAL_HOME / ".sdkman")  # 케이스 test.sh 가 $HOME/.sdkman 의 JDK 를 쓴다
    (sb_home / ".claude" / "settings.json").write_text(json.dumps({"permissions": {"deny": DENY_RULES}}))
    shutil.copytree(CASES / case, run_dir / "work")
    wait_fresh_creds()
    sb_creds = run_dir / "home" / ".claude" / ".credentials.json"
    shutil.copy2(CREDS, sb_creds)
    os.chmod(sb_creds, 0o600)

    prompt = PROMPT_BASE + arm_prompt(arm, case)
    log_path = RAW / f"{run_id}.jsonl"
    t0 = time.time()
    timed_out = False
    with open(log_path, "wb") as lf:
        try:
            p = subprocess.run(claude_cmd(prompt, model, budget), cwd=run_dir / "work", env=sandbox_env(sb_home),
                               stdin=subprocess.DEVNULL, stdout=lf, stderr=subprocess.STDOUT, timeout=timeout_s)
            rc = p.returncode
        except subprocess.TimeoutExpired:
            rc, timed_out = -1, True
    wall = time.time() - t0
    creds_written_back = writeback_creds(sb_creds)

    parsed = parse_log(log_path)
    res = parsed["result"] or {}
    usage = res.get("usage") or {}
    violations = audit(log_path.read_text(errors="replace"), arm, run_id)
    j = judge(case, run_dir / "work")
    row = {
        "run_id": run_id, "case": case, "arm": arm, "rep": rep, "model": model,
        "rc": rc, "timed_out": timed_out, "wall_s": round(wall, 1),
        "result_subtype": res.get("subtype"), "is_error": res.get("is_error"),
        "num_turns": res.get("num_turns"), "duration_ms": res.get("duration_ms"),
        "total_cost_usd": res.get("total_cost_usd"),
        "input_tokens": usage.get("input_tokens"), "output_tokens": usage.get("output_tokens"),
        "cache_creation_input_tokens": usage.get("cache_creation_input_tokens"),
        "cache_read_input_tokens": usage.get("cache_read_input_tokens"),
        "web_search_requests": (usage.get("server_tool_use") or {}).get("web_search_requests"),
        "web_fetch_requests": (usage.get("server_tool_use") or {}).get("web_fetch_requests"),
        "tool_calls": parsed["tool_calls"], "tools": parsed["tools"], "archive_tool_calls": parsed["archive_reads"], "card_tool_calls": parsed["card_reads"],
        "first_edit_turn": parsed["first_edit_turn"], "archive_result_bytes": parsed["archive_result_bytes"],
        "audit_violations": violations, "valid": not violations and parsed["result"] is not None,
        "tampered": j["tampered"], "test_pass": j["test_pass"], "hidden_pass": j["hidden_pass"],
        "success": j["test_pass"] and j["hidden_pass"] and not j["tampered"],
        "hidden_tail": j["hidden_tail"], "creds_written_back": creds_written_back,
        "log": str(log_path.relative_to(ROOT)),
    }
    append_row(row)
    shutil.rmtree(run_dir, ignore_errors=True)
    return row


def done_keys() -> set:
    keys = set()
    if RUNS_FILE.exists():
        for line in RUNS_FILE.read_text().splitlines():
            r = json.loads(line)
            if r.get("valid"):
                keys.add((r["case"], r["arm"], r["rep"]))
    return keys


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--archive-src", required=True)
    ap.add_argument("--cases", default="")
    ap.add_argument("--arms", default="A,B")
    ap.add_argument("--reps", default="1,2,3")
    ap.add_argument("--parallel", type=int, default=5)
    ap.add_argument("--model", default="sonnet")
    ap.add_argument("--budget", default="2")
    ap.add_argument("--timeout", type=int, default=1800)
    ap.add_argument("--cases-dir", default="cases")
    ap.add_argument("--oracle-dir", default="oracle")
    ap.add_argument("--results-dir", default="results")
    ap.add_argument("--archive-dir", default=str(ARCHIVE_DIR))
    a = ap.parse_args()
    set_dirs(a.cases_dir, a.oracle_dir, a.results_dir)
    set_archive(a.archive_dir)

    HOST_RUNS.mkdir(exist_ok=True)
    RAW.mkdir(parents=True, exist_ok=True)
    if not (ARCHIVE_DIR / "issue").exists():
        shutil.copytree(a.archive_src, ARCHIVE_DIR / "issue")
        src_repo = Path(a.archive_src).resolve().parent
        head = subprocess.run(["git", "-C", str(src_repo), "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip()
        (RESULTS / "archive-source.txt").write_text(f"{a.archive_src} @ {head} (copied {time.strftime('%F %T')})\n")

    cases = sorted(d.name for d in CASES.iterdir() if d.is_dir())
    if a.cases:
        want = a.cases.split(",")
        cases = [c for c in cases if c[:2] in want]
    arms = a.arms.split(",")
    reps = [int(r) for r in a.reps.split(",")]
    done = done_keys()
    jobs = []
    for rep in reps:
        for i, c in enumerate(cases):
            order = arms if (i + rep) % 2 == 0 else list(reversed(arms))  # A/B 교대
            for arm in order:
                if (c, arm, rep) not in done:
                    jobs.append((c, arm, rep))
    print(f"jobs={len(jobs)} parallel={a.parallel}", flush=True)
    fails = 0
    with cf.ThreadPoolExecutor(max_workers=a.parallel) as ex:
        futs = {ex.submit(run_one, c, arm, rep, a.model, a.budget, a.timeout): (c, arm, rep)
                for c, arm, rep in jobs}
        for f in cf.as_completed(futs):
            c, arm, rep = futs[f]
            try:
                r = f.result()
                print(f"{c} {arm} r{rep}: success={r['success']} valid={r['valid']} cost={r['total_cost_usd']} "
                      f"turns={r['num_turns']} viol={r['audit_violations']}", flush=True)
            except Exception as e:  # 러너 오류도 숨기지 않는다
                fails += 1
                print(f"{c} {arm} r{rep}: RUNNER ERROR {e!r}", flush=True)
                append_row({"case": c, "arm": arm, "rep": rep, "valid": False, "runner_error": repr(e)})
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
