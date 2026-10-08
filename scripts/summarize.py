#!/usr/bin/env python3
"""results/runs.jsonl → results/summary.md (케이스 × arm 집계).

- 유효(valid) 행만 집계한다. 무효·러너 오류 행은 건수만 따로 보인다.
- 총 토큰 = input + output + cache_creation + cache_read (캐시 읽기 포함 — 컨텍스트로 처리한 양).
- 중앙값 기준 A/B 비율(A÷B)을 함께 낸다: 1 미만이면 아카이브 쪽이 적게 썼다.
"""
import json
import statistics as st
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RUNS = ROOT / "results" / "runs.jsonl"
OUT = ROOT / "results" / "summary.md"


def total_tokens(r):
    return sum(r.get(k) or 0 for k in ("input_tokens", "output_tokens",
                                       "cache_creation_input_tokens", "cache_read_input_tokens"))


def med(xs):
    return st.median(xs) if xs else None


def fmt(x, d=0):
    if x is None:
        return "-"
    return f"{x:,.{d}f}"


def ratio(a, b):
    return f"{a / b:.2f}" if a is not None and b else "-"


def main():
    rows = [json.loads(l) for l in RUNS.read_text().splitlines() if l.strip()]
    valid = [r for r in rows if r.get("valid")]
    invalid = [r for r in rows if not r.get("valid")]
    cases = sorted({r["case"] for r in rows})

    lines = ["# 토큰 비교 결과 — A(아카이브, 스스로 찾기) · F(아카이브 우선 — 추가 실험) · B(직접 조사)", "",
             f"- 전체 행 {len(rows)} · 유효 {len(valid)} · 무효/오류 {len(invalid)}",
             "- 수치 = 유효 실행의 중앙값. 총 토큰 = input + output + cache_creation + cache_read. 비율 = A÷B (1 미만이면 A가 적게 씀).",
             "", "## 케이스별", "",
             "| 케이스 | arm | n | 성공 | 총 토큰 | 출력 토큰 | 비용 $ | 턴 | 도구 호출 | 웹 검색+fetch | 아카이브 호출 | 카드 열람 실행 |",
             "|---|---|---|---|---|---|---|---|---|---|---|---|"]
    arms = [a for a in ("A", "F", "B") if any(r["arm"] == a for r in rows)]
    agg = {a: [] for a in arms}
    ratio_rows = []
    for c in cases:
        per = {}
        for arm in arms:
            rs = [r for r in valid if r["case"] == c and r["arm"] == arm]
            agg[arm] += rs
            m = {
                "n": len(rs),
                "succ": sum(1 for r in rs if r.get("success")),
                "tok": med([total_tokens(r) for r in rs]),
                "out": med([r.get("output_tokens") or 0 for r in rs]),
                "cost": med([r.get("total_cost_usd") or 0 for r in rs]),
                "turns": med([r.get("num_turns") or 0 for r in rs]),
                "tools": med([r.get("tool_calls") or 0 for r in rs]),
                "web": med([(r.get("web_search_requests") or 0) + (r.get("web_fetch_requests") or 0) for r in rs]),
                "arch": med([r.get("archive_tool_calls") or 0 for r in rs]),
                "cards": sum(1 for r in rs if (r.get("card_tool_calls") or 0) > 0),
            }
            per[arm] = m
            lines.append(f"| {c} | {arm} | {m['n']} | {m['succ']}/{m['n']} | {fmt(m['tok'])} | {fmt(m['out'])} | "
                         f"{fmt(m['cost'], 3)} | {fmt(m['turns'])} | {fmt(m['tools'])} | {fmt(m['web'])} | {fmt(m['arch'])} | {m['cards']}/{m['n']} |")
        ratio_rows.append((c, [(a, ratio(per[a]["tok"], per["B"]["tok"]), ratio(per[a]["cost"], per["B"]["cost"]),
                                ratio(per[a]["turns"], per["B"]["turns"])) for a in arms if a != "B"]))

    lines += ["", "## 케이스별 ÷B 비율 (중앙값 기준, A=스스로 찾기 · F=아카이브 우선)", "",
              "| 케이스 | arm | 총 토큰 | 비용 | 턴 |", "|---|---|---|---|---|"]
    lines += [f"| {c} | {a}÷B | {t} | {co} | {tu} |" for c, rs in ratio_rows for a, t, co, tu in rs]

    lines += ["", "## 전체", "", "| arm | n | 성공률 | 총 토큰 합 | 총 토큰 중앙값 | 비용 합 $ | 비용 중앙값 $ |", "|---|---|---|---|---|---|---|"]
    for arm in arms:
        rs = agg[arm]
        n = len(rs)
        succ = sum(1 for r in rs if r.get("success"))
        lines.append(f"| {arm} | {n} | {succ}/{n} | {fmt(sum(total_tokens(r) for r in rs))} | "
                     f"{fmt(med([total_tokens(r) for r in rs]))} | {fmt(sum(r.get('total_cost_usd') or 0 for r in rs), 2)} | "
                     f"{fmt(med([r.get('total_cost_usd') or 0 for r in rs]), 3)} |")

    if invalid:
        lines += ["", "## 무효·오류 실행", "", "| case | arm | rep | 사유 |", "|---|---|---|---|"]
        for r in invalid:
            why = r.get("runner_error") or (",".join(r.get("audit_violations") or []) or "result 없음")
            lines.append(f"| {r.get('case')} | {r.get('arm')} | {r.get('rep')} | {why} |")

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())
