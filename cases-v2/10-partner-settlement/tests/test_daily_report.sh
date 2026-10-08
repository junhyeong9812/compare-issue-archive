#!/usr/bin/env bash
# The settlement desk's daily report for 2026-10-05.
. "$(dirname "$0")/lib.sh"
t_setup

settle run 20261005 2>"$T_WORK/err"; rc=$?
assert_rc "daily run completes" "$rc" 0
report=$(settle report 20261005)
printf '%s\n' "$report"

expect_row() {  # code name count amount
  local line
  line=$(printf '%s\n' "$report" | grep -a "^$1 ")
  assert_contains "report row $1 shows partner name '$2'" "$line" "$2"
  assert_contains "report row $1 totals" "$line" "$3"
}
expect_row P10001 "한빛상사"            "1,295,000"
expect_row P10002 "대성유통"            "442,000"
expect_row P10003 "제3물류센터운영위탁" "2,188,000"
expect_row P10004 "ACME Korea Ltd"      "990,000"
expect_row P10005 "미래푸드시스템즈"    "154,000"
expect_row P10006 "(주)동방정밀기계공업" "3,300,000"
expect_row P10007 "S&T글로벌로지스틱스" "751,000"

assert_contains "grand total" "$report" "9,120,000"
if printf '%s' "$report" | iconv -f UTF-8 -t UTF-8 >/dev/null 2>&1; then
  ok "report text is readable (valid UTF-8)"
else
  fail "report text is readable (valid UTF-8)" "report contains garbled characters"
fi
t_done
