# partners.sh — partner master lookups.
#
# The master is exported by the same ERP as the feed, pipe-delimited:
#   CODE|NAME|REGION|STATUS        (STATUS: A active, I inactive)
# Names are loaded once per run into the "partners" cache.

partners_load() {
  local code name region status n=0
  require_file "$MASTER_FILE" "partner master"
  if cache_fresh partners "$MASTER_FILE" && [ -s "$(cache_file partners)" ]; then
    return 0
  fi
  cache_init partners
  cache_init partner_status
  while IFS='|' read -r code name region status || [ -n "$code" ]; do
    code=$(text_trim "$code")
    case "$code" in ''|\#*) continue ;; esac
    name=$(charset_strip_cr "$name")
    status=$(charset_strip_cr "$status")
    cache_put partners "$code" "$(charset_to_utf8 "$name")"
    cache_put partner_status "$code" "${status:-A}"
    n=$((n + 1))
  done < "$MASTER_FILE"
  cache_stamp partners "$MASTER_FILE"
  log "partner master: $n entries"
}

# partner_name CODE [DEFAULT] — display name for CODE.
partner_name() {
  local v
  if v=$(cache_get partners "$1") && [ -n "$v" ]; then
    printf '%s' "$v"
  elif [ -n "${2:-}" ]; then
    printf '%s' "$2"
  else
    printf '(unknown %s)' "$1"
  fi
}

partner_active() {
  local s
  s=$(cache_get partner_status "$1") || return 0
  [ "$s" != I ]
}
