#!/usr/bin/env bash
# A week of daily runs (2026-10-01..07); partners must be able to download
# every day's settlement.
. "$(dirname "$0")/lib.sh"
t_setup

for d in 20261001 20261002 20261003 20261004 20261005 20261006 20261007; do
  payout run "$d" 2>>"$T_WORK/run.err"; rc=$?
  assert_rc "daily run $d completes" "$rc" 0
  assert_contains "run $d got transfer tickets" "$(payout manifest "$d")" " T0"
done

expect_day() {  # report date rows net
  local line
  line=$(printf '%s\n' "$1" | grep "^$2 ")
  assert_contains "partner can download $2 settlement ($3 rows)" "$line" "$2  $(printf '%4d' "$3")"
  assert_contains "partner total for $2" "$line" "$4"
}

r1=$(payout receipts P001 20261001 20261007)
printf '%s\n' "$r1"
expect_day "$r1" 2026-10-01 12 "2,829,356"
expect_day "$r1" 2026-10-02 26 "5,363,775"
expect_day "$r1" 2026-10-03  9 "1,817,402"
expect_day "$r1" 2026-10-04  7 "1,501,892"
expect_day "$r1" 2026-10-05 31 "6,257,753"
expect_day "$r1" 2026-10-06 14 "3,937,833"
expect_day "$r1" 2026-10-07 18 "4,705,841"

r3=$(payout receipts P003 20261001 20261007)
expect_day "$r3" 2026-10-02 3 "728,033"
expect_day "$r3" 2026-10-05 2 "195,293"

r4=$(payout receipts P004 20261001 20261007)
assert_eq "suspended partner gets nothing" "$(printf '%s\n' "$r4" | grep -c ' - ')" 7
t_done
