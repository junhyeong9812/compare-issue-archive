#!/usr/bin/env bash
# After a rollout, `rollout show HOST` names the release that just finished,
# for every active host.
. "$(dirname "$0")/lib.sh"
t_setup

rollout apply r2026.10.01 2>>"$T_WORK/apply.err"; assert_rc "baseline rollout completes" $? 0
for h in api-01 web-01 bat-02; do
  assert_contains "$h shows r2026.10.01" "$(rollout show "$h" 2>&1)" "r2026.10.01"
done

rollout apply r2026.10.08 2>>"$T_WORK/apply.err"; assert_rc "r2026.10.08 rollout completes" $? 0
for h in api-01 api-02 api-03 bat-01 bat-02 web-01 web-02; do
  assert_contains "$h shows r2026.10.08" "$(rollout show "$h" 2>&1)" "r2026.10.08"
done
assert_contains "web-01 settings unchanged" "$(rollout show web-01 2>&1)" "log_level = warn"
assert_contains "api-02 settings updated" "$(rollout show api-02 2>&1)" "pool_size = 40"
rollout show web-03 >/dev/null 2>&1
assert_rc "drained host still has no config" $? 1
t_done
