#!/usr/bin/env bash
. "$(dirname "$0")/lib.sh"
t_setup
t_source_libs

assert_eq "escape ampersand"  "$(sed_repl_escape 'a&b')" 'a\&b'
assert_eq "escape delimiter"  "$(sed_repl_escape 'a|b')" 'a\|b'
assert_eq "escape backslash"  "$(sed_repl_escape 'a\b')" 'a\\b'
assert_eq "trim"              "$(trim '   x y  ')" "x y"

# scope precedence on a fresh service
"$CFGSVC" set global.db_url 'jdbc:postgresql://h:5432/d?x=1&y=a|b' >/dev/null
"$CFGSVC" set global.db_pool_size 10 >/dev/null
"$CFGSVC" set global.timeout_ms 5000 >/dev/null
"$CFGSVC" set global.worker_threads 2 >/dev/null
"$CFGSVC" set global.log_level info >/dev/null
"$CFGSVC" set global.feature_fastpay off >/dev/null
"$CFGSVC" set pool.web.timeout_ms 1500 >/dev/null
"$CFGSVC" set host.web-02.timeout_ms 900 >/dev/null

assert_eq "global value"          "$(resolve web-01 web db_pool_size)" 10
assert_eq "pool beats global"     "$(resolve web-01 web timeout_ms)" 1500
assert_eq "host beats pool"       "$(resolve web-02 web timeout_ms)" 900
assert_eq "other pool unaffected" "$(resolve api-01 api timeout_ms)" 5000
resolve api-01 api no_such_setting; assert_rc "missing setting" $? 1

out=$(render_host web-02 web r-x)
assert_contains "header names the host" "$out" "for web-02 (pool web, release r-x)"
assert_contains "special characters survive" "$out" 'url = jdbc:postgresql://h:5432/d?x=1&y=a|b'
assert_contains "host override rendered" "$out" "timeout_ms = 900"
printf '%s\n' "$out" > "$T_WORK/app.conf"
assert_eq "rendered value lookup" "$(rendered_value "$T_WORK/app.conf" pool_size)" 10
t_done
