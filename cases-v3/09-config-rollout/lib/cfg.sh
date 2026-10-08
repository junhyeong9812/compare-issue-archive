# cfg.sh — wrapper around the configuration service vendor CLI.

_cfgsvc() { "$CFGSVC" "$@"; }

# cfg_set KEY VALUE — records VALUE and prints the change number.
cfg_set() {
  local out rc
  out=$(_cfgsvc set "$1" "$2" 2>&1); rc=$?
  [ "$rc" -eq 0 ] || { warn "set $1 failed ($out, exit $rc)"; return 1; }
  case "$out" in
    C[0-9][0-9][0-9][0-9][0-9][0-9][0-9]) printf '%s' "$out" ;;
    *) warn "unexpected answer from config service: $out"; return 1 ;;
  esac
}

# cfg_get KEY — prints the value. Status 1 when the key does not exist,
# anything else from the CLI is fatal.
cfg_get() {
  local out rc
  out=$(cache_get "$1"); rc=$?
  [ "$rc" -eq 0 ] && { printf '%s' "$out"; return 0; }
  [ "$rc" -eq 2 ] && return 1
  out=$(_cfgsvc get "$1" 2>&1); rc=$?
  if [ "$rc" -eq 0 ]; then
    cache_put "$1" "$out"
    printf '%s' "$out"
    return 0
  fi
  if [ "$rc" -eq 3 ] && [ "$out" = "E31" ]; then
    cache_put_missing "$1"
    return 1
  fi
  die "config service lookup of $1 failed ($out, exit $rc)"
}

# cfg_mark — the service's current mark (recorded with every rollout).
cfg_mark() { _cfgsvc mark; }

# cfg_keys PREFIX — key<TAB>change<TAB>st
cfg_keys() { _cfgsvc keys "$1"; }
