#!/usr/bin/env bash
# Hidden judge: for another host set and three consecutive releases (plus
# operator changes made straight in the configuration service between them),
# every active host's rendered app.conf must match the values the configuration
# service itself serves once the rollout is done (checked straight through
# vendor/bin/cfgsvc) and the expected effective values.
set -u
export LC_ALL=C.UTF-8
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
failures=0
ok()  { echo "ok   - $1"; }
bad() { echo "FAIL - $1"; [ -n "${2:-}" ] && echo "       $2"; failures=$((failures + 1)); }
C="$HERE/vendor/bin/cfgsvc"

mkdir -p "$WORK/rel" "$WORK/cfg"
cat > "$WORK/hosts.conf" <<'H'
# host|pool|zone|status
pay-01|pay|a|A
pay-02|pay|b|A
pay-03|pay|a|A
ops-01|ops|a|A
ops-02|ops|b|D
edge-01|edge|b|A
H
cat > "$WORK/rel/h1.rel" <<'R'
@release h1
global.db_url          = jdbc:postgresql://core-db:5432/core
global.db_pool_size    = 8
global.timeout_ms      = 4000
global.worker_threads  = 4
global.log_level       = info
global.feature_fastpay = off
pool.pay.db_pool_size  = 12
pool.ops.worker_threads = 2
host.pay-03.timeout_ms = 700
R
cat > "$WORK/rel/h2.rel" <<'R'
# global change (existing key), host override change (existing), new host
# override, new pool key, special characters
@release h2
global.timeout_ms      = 3500
host.pay-03.timeout_ms = 650
host.pay-01.db_pool_size = 30
global.db_url          = jdbc:postgresql://core-db-v2:5432/core?opt=a|b&ssl=1
pool.edge.log_level    = warn
R
cat > "$WORK/rel/h3.rel" <<'R'
# same key twice (last one wins), existing pool key
@release h3
global.log_level       = warn
pool.pay.db_pool_size  = 14
global.log_level       = error
R

export ROLLOUT_HOSTS_FILE="$WORK/hosts.conf" ROLLOUT_RELEASE_DIR="$WORK/rel"
export ROLLOUT_WORK_DIR="$WORK/work" ROLLOUT_OUT_DIR="$WORK/out" CFGSVC_HOME="$WORK/cfg"
SETTINGS="db_url db_pool_size timeout_ms worker_threads log_level feature_fastpay"
line_key() { case "$1" in db_url) echo url ;; db_pool_size) echo pool_size ;; *) echo "$1" ;; esac; }

# value the service serves now for HOST POOL SETTING (most specific scope)
served() {
  local v
  for k in "host.$1.$3" "pool.$2.$3" "global.$3"; do
    v=$("$C" get "$k" 2>/dev/null) && { printf '%s' "$v"; return 0; }
  done
  return 1
}
rendered() {
  local f="$ROLLOUT_OUT_DIR/hosts/$1/app.conf" k
  k=$(line_key "$2")
  awk -v k="$k" '{ i = index($0, " = ") } i && substr($0, 1, i - 1) == k { print substr($0, i + 3); exit }' "$f" 2>/dev/null
}

# expect REL HOST POOL "setting=value ..."
expect() {
  local rel=$1 host=$2 pool=$3 pair s want got srv
  for pair in $4; do
    s=${pair%%=*} want=${pair#*=}
    got=$(rendered "$host" "$s")
    srv=$(served "$host" "$pool" "$s")
    if [ "$got" = "$want" ] && [ "$srv" = "$want" ]; then ok "$rel $host $s = $want"
    else bad "$rel $host $s" "rendered [$got], service [$srv], expected [$want]"; fi
  done
}
# every active host, every setting: rendered == served
consistent() {
  local rel=$1 host pool s got srv n=0
  while IFS='|' read -r host pool _ st; do
    case "$host" in '#'*|'') continue ;; esac
    [ "$st" = A ] || continue
    for s in $SETTINGS; do
      got=$(rendered "$host" "$s"); srv=$(served "$host" "$pool" "$s")
      [ "$got" = "$srv" ] || { bad "$rel $host $s matches the service" "rendered [$got], service [$srv]"; n=$((n + 1)); }
    done
  done < "$WORK/hosts.conf"
  [ "$n" -eq 0 ] && ok "$rel every active host matches the service"
}
apply() {
  "$HERE/bin/rollout" apply "$1" 2>>"$WORK/apply.err"; local rc=$?
  [ "$rc" -eq 0 ] && ok "rollout $1 completes" || bad "rollout $1 completes (exit $rc)" "$(tail -3 "$WORK/apply.err")"
}

URL1='jdbc:postgresql://core-db:5432/core'
URL2='jdbc:postgresql://core-db-v2:5432/core?opt=a|b&ssl=1'

apply h1
consistent h1
expect h1 pay-01 pay "db_pool_size=12 timeout_ms=4000 db_url=$URL1"
expect h1 pay-03 pay "timeout_ms=700"
expect h1 ops-01 ops "worker_threads=2 db_pool_size=8"
[ -e "$ROLLOUT_OUT_DIR/hosts/ops-02/app.conf" ] && bad "drained host gets no config" || ok "drained host gets no config"

# operator changes made straight in the service between releases:
#   fast-pay switched on for everyone (and marked by the operator console),
#   ops worker threads raised without marking.
"$C" set global.feature_fastpay on >/dev/null && "$C" mark >/dev/null
"$C" set pool.ops.worker_threads 6 >/dev/null

apply h2
consistent h2
expect h2 pay-01 pay "timeout_ms=3500 db_pool_size=30 db_url=$URL2 feature_fastpay=on log_level=info"
expect h2 pay-02 pay "timeout_ms=3500 db_pool_size=12 db_url=$URL2"
expect h2 pay-03 pay "timeout_ms=650 db_pool_size=12"
expect h2 ops-01 ops "timeout_ms=3500 worker_threads=6 feature_fastpay=on"
expect h2 edge-01 edge "log_level=warn timeout_ms=3500 db_pool_size=8"

apply h3
consistent h3
expect h3 pay-01 pay "log_level=error db_pool_size=30 timeout_ms=3500"
expect h3 pay-02 pay "log_level=error db_pool_size=14"
expect h3 edge-01 edge "log_level=warn"
expect h3 ops-01 ops "log_level=error worker_threads=6"

n=$("$HERE/bin/rollout" history 2>/dev/null | wc -l | tr -d ' ')
[ "$n" = 3 ] && ok "three rollouts in the history" || bad "three rollouts in the history" "got $n"

echo "# hidden: $failures failure(s)"
[ "$failures" -eq 0 ]
