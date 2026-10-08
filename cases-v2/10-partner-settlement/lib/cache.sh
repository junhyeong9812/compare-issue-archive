# cache.sh — tiny key/value cache kept under the run's work directory so
# that repeated lookups (partner names, mostly) do not re-read the master.
#
# File format: one "key<TAB>value" per line, file per namespace.

cache_file() { printf '%s/cache/%s.kv' "$WORK_DIR" "$1"; }

cache_init() {
  ensure_dir "$WORK_DIR/cache"
  : > "$(cache_file "$1")"
}

# cache_put NS KEY VALUE — replaces any previous value for KEY.
cache_put() {
  local f tmp
  f=$(cache_file "$1")
  tmp="$f.$$"
  if [ -f "$f" ]; then
    awk -F '\t' -v k="$2" '$1 != k' "$f" > "$tmp" || { rm -f "$tmp"; return 1; }
  else
    : > "$tmp"
  fi
  printf '%s\t%s\n' "$2" "$3" >> "$tmp"
  mv -f "$tmp" "$f"
}

# cache_get NS KEY — prints the value; fails when KEY is absent.
cache_get() {
  local f
  f=$(cache_file "$1")
  [ -f "$f" ] || return 1
  LC_ALL=C awk -F '\t' -v k="$2" '
    $1 == k { printf "%s", substr($0, length(k) + 2); found = 1; exit }
    END { exit !found }
  ' "$f"
}

# cache_stamp NS SOURCE — remembers SOURCE's mtime; cache_fresh compares.
cache_stamp() { stat -c %Y "$2" > "$(cache_file "$1").stamp" 2>/dev/null; }

cache_fresh() {
  local s
  s=$(cat "$(cache_file "$1").stamp" 2>/dev/null) || return 1
  [ "$s" = "$(stat -c %Y "$2" 2>/dev/null)" ]
}
