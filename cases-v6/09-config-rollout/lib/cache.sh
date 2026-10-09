# cache.sh — per-step memo of config lookups.
# Rendering asks for the same global/pool keys once per host; the memo keeps
# that to one CLI call per key. It lives in a fresh directory for every step
# process and is removed when the step exits.

cache_init() {
  CACHE_DIR=$(mktemp -d "${TMPDIR:-/tmp}/rollout-cache.XXXXXX") || return 1
  trap 'rm -rf "$CACHE_DIR"' EXIT
}

# cache_get KEY — prints the memoized answer; status 0 hit, 1 miss.
cache_get() {
  [ -n "${CACHE_DIR:-}" ] || return 1
  local f="$CACHE_DIR/$(key_file "$1")"
  [ -f "$f" ] || return 1
  # a memoized "no such key" is an empty marker file
  [ -s "$f" ] || return 2
  cat "$f"
}

# cache_put KEY VALUE ; cache_put_missing KEY
cache_put() {
  [ -n "${CACHE_DIR:-}" ] || return 0
  printf '%s' "$2" > "$CACHE_DIR/$(key_file "$1")"
}
cache_put_missing() {
  [ -n "${CACHE_DIR:-}" ] || return 0
  : > "$CACHE_DIR/$(key_file "$1")"
}
