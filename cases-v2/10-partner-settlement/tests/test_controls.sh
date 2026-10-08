#!/usr/bin/env bash
# Control totals and run behaviour on the sample feeds.
. "$(dirname "$0")/lib.sh"
t_setup

settle run 20261004 2>"$T_WORK/err"; rc=$?
assert_rc "run 2026-10-04 succeeds" "$rc" 0
out=$(settle check 20261004 2>&1)
assert_contains "control totals verified" "$out" "control totals OK"

settle run 20261003 2>"$T_WORK/err"; rc=$?
if [ "$rc" -ne 0 ]; then ok "missing feed fails the run"; else fail "missing feed fails the run"; fi

csv=$(settle export 20261004)
assert_eq "export row count" "$(printf '%s\n' "$csv" | wc -l)" 4
assert_contains "export header" "$csv" "date,seq,code,partner,amount,memo"
assert_contains "ascii partner row" "$csv" "2026-10-04,2,P10004,ACME Korea Ltd,120000,import fee"

out=$(settle partner P99999 2>&1); rc=$?
assert_rc "unknown partner lookup" "$rc" 4
t_done
