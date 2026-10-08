#!/usr/bin/env bash
# Hidden judge: names/memos must come out readable on every output path
# (report, CSV export, partner lookup) for a different day's feed and master.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
failures=0
check() { if eval "$2"; then echo "ok   - $1"; else echo "FAIL - $1"; failures=$((failures + 1)); fi; }

# --- fixture builders (legacy code page, fixed width, byte-cut like the ERP) ---
ek() {
  local t b
  t=$(mktemp); printf '%s' "$1" | iconv -f UTF-8 -t EUC-KR | head -c "$2" > "$t"
  b=$(wc -c < "$t"); cat "$t"; rm -f "$t"
  [ "$b" -lt "$2" ] && printf '%*s' $(( $2 - b )) ''
  return 0
}
mkfeed() {
  local date=$1 out=$2 code name amount memo n=0 total=0
  {
    printf 'H%s%-16s%39s\n' "$date" "LEGACYERP-IF07" ''
    while IFS='|' read -r code name amount memo; do
      n=$((n+1)); total=$((total+amount))
      printf 'D%s%05d%-6s' "$date" "$n" "$code"; ek "$name" 20
      printf '%012d' "$amount"; ek "$memo" 12; printf '\n'
    done
    printf 'T%s%06d%015d%34s\n' "$date" "$n" "$total" ''
  } > "$out"
}
mkmaster() {
  local code name region status
  while IFS='|' read -r code name region status; do
    printf '%s|' "$code"; printf '%s' "$name" | iconv -f UTF-8 -t EUC-KR | head -c 20
    printf '|%s|%s\n' "$region" "$status"
  done > "$1"
}

mkdir -p "$WORK/feeds"
mkmaster "$WORK/master.dat" <<'M'
Q20001|1등급한우직판유통조합|SEOUL|A
Q20002|Blue Ocean 수산|BUSAN|A
Q20003|가온누리문화콘텐츠산업|DAEJEON|A
Q20004|J&K한국정밀주물제작소|CHANGWON|A
M
mkfeed 20260412 "$WORK/feeds/feed-20260412.dat" <<'F'
Q20001|1등급한우직판유통조합|410000|3월 정산분 차액
Q20002|Blue Ocean 수산|77000|A급 냉동육 운송
Q20003|가온누리문화콘텐츠산업|1500000|공연 대관
Q20004|J&K한국정밀주물제작소|2640000|금형 수리
Q20005|새벽배송1번지물류센터|93000|새벽 배송비
Q20001|1등급한우직판유통조합|25000|부대비
F

export SETTLE_FEED_DIR="$WORK/feeds" SETTLE_MASTER_FILE="$WORK/master.dat"
export SETTLE_WORK_DIR="$WORK/work" SETTLE_OUT_DIR="$WORK/out"
S="$HERE/bin/settle"

"$S" run 20260412 2>"$WORK/run.err"; rc=$?
check "run completes (got $rc)" '[ "$rc" -eq 0 ]'

report=$("$S" report 20260412 2>/dev/null)
csv=$("$S" export 20260412 2>/dev/null)
utf8() { printf '%s' "$1" | iconv -f UTF-8 -t UTF-8 >/dev/null 2>&1; }
row()  { printf '%s\n' "$report" | grep -a "^$1 "; }
crow() { printf '%s\n' "$csv" | grep -a "^2026-04-12,$1,"; }

check "report is valid UTF-8" 'utf8 "$report"'
check "csv is valid UTF-8"    'utf8 "$csv"'

check "report Q20001 name"               '[[ "$(row Q20001)" == *"1등급한우직판유통조"* ]]'
check "report Q20002 name (clean)"       '[[ "$(row Q20002)" == *"Blue Ocean 수산"* ]]'
check "report Q20003 name (clean cut)"   '[[ "$(row Q20003)" == *"가온누리문화콘텐츠산"* ]]'
check "report Q20004 name"               '[[ "$(row Q20004)" == *"J&K한국정밀주물제작"* ]]'
check "report Q20005 uses feed name"     '[[ "$(row Q20005)" == *"새벽배송1번지물류센"* ]]'
check "report Q20001 total"              '[[ "$(row Q20001)" == *"435,000"* ]]'
check "report grand total"               '[[ "$report" == *"4,745,000"* ]]'

check "csv seq1 name+memo" '[[ "$(crow 1)" == "2026-04-12,1,Q20001,1등급한우직판유통조,410000,3월 정산분"* ]]'
check "csv seq2 memo"      '[[ "$(crow 2)" == "2026-04-12,2,Q20002,Blue Ocean 수산,77000,A급 냉동육"* ]]'
check "csv seq3 clean"     '[ "$(crow 3)" = "2026-04-12,3,Q20003,가온누리문화콘텐츠산,1500000,공연 대관" ]'
check "csv seq4 name"      '[ "$(crow 4)" = "2026-04-12,4,Q20004,J&K한국정밀주물제작,2640000,금형 수리" ]'
check "csv seq5 name"      '[ "$(crow 5)" = "2026-04-12,5,Q20005,새벽배송1번지물류센,93000,새벽 배송비" ]'
check "csv row count"      '[ "$(printf "%s\n" "$csv" | wc -l)" -eq 7 ]'

lookup=$("$S" partner Q20004 2>/dev/null)
check "partner lookup Q20004 readable" '[[ "$lookup" == *"name:   J&K한국정밀주물제작"* ]] && utf8 "$lookup"'
lookup=$("$S" partner Q20001 2>/dev/null)
check "partner lookup Q20001 readable" '[[ "$lookup" == *"name:   1등급한우직판유통조"* ]] && utf8 "$lookup"'

# original sample day still fine
unset SETTLE_FEED_DIR SETTLE_MASTER_FILE
export SETTLE_WORK_DIR="$WORK/work2" SETTLE_OUT_DIR="$WORK/out2"
"$S" run 20261005 2>/dev/null; rc=$?
r2=$("$S" report 20261005 2>/dev/null)
check "sample day run (got $rc)" '[ "$rc" -eq 0 ]'
check "sample day report valid UTF-8" 'utf8 "$r2"'
check "sample day inactive partner omitted" '[[ "$r2" == *"(inactive partners omitted: 1)"* ]]'

echo "failures=$failures"
[ "$failures" -eq 0 ]
