# resolve.sh — effective value of a setting for a host.
# Most specific scope wins: host.<host>.<s>, then pool.<pool>.<s>, then global.<s>.

# scope_key SCOPE HOST POOL SETTING
scope_key() {
  case "$1" in
    host)   printf 'host.%s.%s' "$2" "$4" ;;
    pool)   printf 'pool.%s.%s' "$3" "$4" ;;
    global) printf 'global.%s' "$4" ;;
  esac
}

# resolve HOST POOL SETTING — prints the value; status 1 if no scope has it.
resolve() {
  local host=$1 pool=$2 setting=$3 scope
  for scope in $SCOPES; do
    cfg_get "$(scope_key "$scope" "$host" "$pool" "$setting")" && return 0
  done
  return 1
}
