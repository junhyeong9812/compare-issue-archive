#!/usr/bin/env bash
# The baseline release followed by this week's release (r2026.10.08):
# every host must end up running the configuration of the latest release.
. "$(dirname "$0")/lib.sh"
t_setup

for rel in r2026.10.01 r2026.10.08; do
  rollout apply "$rel" 2>>"$T_WORK/apply.err"; rc=$?
  assert_rc "rollout $rel completes" "$rc" 0
done

expect() {  # host line
  assert_contains "$1 runs with '$2'" "$(rollout show "$1" 2>&1)" "$2"
}
for h in api-01 api-02 api-03; do
  expect "$h" "pool_size = 40"
  expect "$h" "timeout_ms = 2500"
  expect "$h" "feature_fastpay = off"
done
expect bat-01 "feature_fastpay = on"
expect bat-01 "log_level = info"
expect bat-02 "feature_fastpay = on"
expect bat-02 "log_level = debug"
expect bat-02 "timeout_ms = 60000"
expect web-01 "log_level = warn"
expect web-01 "pool_size = 16"
expect web-02 "timeout_ms = 5000"
expect web-02 "url = jdbc:postgresql://pay-db.hbp.internal:5432/pay?ssl=true&app=hbpay"

rollout show web-03 >/dev/null 2>&1
assert_rc "drained host gets no config" $? 1
assert_eq "both rollouts in the history" "$(rollout history | wc -l | tr -d ' ')" 2
assert_contains "latest release recorded" "$(rollout history | tail -1)" "r2026.10.08"
t_done
