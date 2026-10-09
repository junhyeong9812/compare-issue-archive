#!/usr/bin/env python3
"""5차-a G2(카드 형식 방어) 변환: 아카이브 사본 + 요약 사본에 '유효 버전 — 확인 명령'을 얹는다.

사용: python3 build_g2.py <src-archive> <dst-archive> <oracle-dir> <src-summary> <dst-summary>
- 관련 카드 10장: 2-summary.md 제목 줄 바로 뒤에 `> 유효: <버전> — 확인: `<도구> --version``.
- 인덱스: '기준 버전' 열 추가 — 22행 전부(관련 행만 버전이 있으면 단서가 되므로 미끼에도 붙인다).
- L 요약: 첫 줄 `유효: <버전> — 확인: <도구> --version`.
"""
import re, shutil, sys
from pathlib import Path

DECOY_VER = {"bizmsg": "bizmsg 5.1.2", "cronhub": "cronhub 2.7.0", "dbsnap": "dbsnap 1.9.4", "edgecache": "edgecache 3.2.1",
             "filegw": "filegw 4.0.3", "flagkit": "flagkit 2.14.0", "logfwd": "logfwd 1.12.3", "mlgsettle": "mlgsettle 2.2.0",
             "mtragent": "mtragent 6.3.1", "ocrid": "ocrid 3.5.0", "ssogw": "ssogw 1.18.2", "vanrelay": "vanrelay 4.4.1"}

src, dst, oracle, ssum, dsum = sys.argv[1:6]
src, dst, oracle = Path(src), Path(dst), Path(oracle)
if dst.exists():
    sys.exit(f"이미 있음: {dst}")
shutil.copytree(src, dst)
issue = dst / "issue"
ver_by_path = {}
for case in sorted(p for p in oracle.iterdir() if p.is_dir()):
    path = (case / "card.txt").read_text().splitlines()[0].strip().removeprefix("issue/").strip("/")
    card = issue / path
    ver = re.search(r"^\| 벤더 버전 \| (.+?) \|$", (card / "metadata.md").read_text(), re.M).group(1)
    tool = ver.split()[0]
    line = f"> 유효: {ver} — 확인: `{tool} --version`"
    s = (card / "2-summary.md").read_text().split("\n")
    assert s[0].startswith("# "), card
    (card / "2-summary.md").write_text("\n".join([s[0], "", line] + s[1:]))
    summ = (case / ssum).read_text().strip()
    (case / dsum).write_text(f"유효: {ver} — 확인: {tool} --version\n{summ}\n")
    ver_by_path[path + "/"] = ver
out = []
for l in (issue / "README.md").read_text().split("\n"):
    if l.startswith("| 도구/시스템 |"):
        l = l + " 기준 버전 |"
    elif l.startswith("|---|"):
        l = l + "---|"
    elif l.startswith("| "):
        p = re.search(r"`([^`]+/)`", l).group(1)
        v = ver_by_path.get(p) or DECOY_VER[p.split("/")[0]]
        l = l + f" {v} |"
    out.append(l)
(issue / "README.md").write_text("\n".join(out))
assert len(ver_by_path) == 10, ver_by_path
print(dst, len(ver_by_path), "cards")
