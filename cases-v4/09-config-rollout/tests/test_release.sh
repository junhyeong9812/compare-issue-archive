#!/usr/bin/env bash
. "$(dirname "$0")/lib.sh"
t_setup
t_source_libs

f="$T_WORK/r.rel"
cat > "$f" <<'R'
# comment
@release r-test
global.log_level   =   warn
pool.api.timeout_ms=1500
host.web-02.db_url = jdbc:postgresql://x:5432/p?a=1&b=2
R
assert_eq "release id" "$(release_id "$f")" "r-test"
assert_eq "change lines, trimmed" "$(release_changes "$f" | tr '\t' '=' | tr '\n' ' ')" \
  "global.log_level=warn pool.api.timeout_ms=1500 host.web-02.db_url=jdbc:postgresql://x:5432/p?a=1&b=2 "
release_check "$f" 2>/dev/null; assert_rc "valid release passes" $? 0

printf '@release r-bad\nhost.api-09.log_level = info\n' > "$T_WORK/b1.rel"
release_check "$T_WORK/b1.rel" 2>/dev/null; assert_rc "unknown host rejected" $? 1
printf '@release r-bad\nglobal.color = blue\n' > "$T_WORK/b2.rel"
release_check "$T_WORK/b2.rel" 2>/dev/null; assert_rc "unknown setting rejected" $? 1
printf '@release r-bad\npool.db.log_level = info\n' > "$T_WORK/b3.rel"
release_check "$T_WORK/b3.rel" 2>/dev/null; assert_rc "unknown pool rejected" $? 1
assert_eq "template settings" "$(template_settings | tr '\n' ' ')" \
  "db_pool_size db_url feature_fastpay log_level timeout_ms worker_threads "
t_done
