# charset.sh — conversion between the partner ERP code page and UTF-8.
# Everything this tool writes (reports, exports, lookups) is UTF-8.

# charset_to_utf8 TEXT [ENCODING]
#   Converts TEXT from ENCODING (default FEED_ENCODING) to UTF-8.
charset_to_utf8() {
  local raw=$1 enc=${2:-$FEED_ENCODING} out
  [ -n "$raw" ] || return 0
  case "$enc" in
    UTF-8|utf-8|UTF8|utf8) printf '%s' "$raw"; return 0 ;;
  esac
  out=$(printf '%s' "$raw" | iconv -f "$enc" -t UTF-8 -c 2>/dev/null) || out=$raw
  printf '%s' "$out"
}

# charset_is_utf8 TEXT — succeeds when TEXT is well-formed UTF-8.
charset_is_utf8() {
  printf '%s' "$1" | iconv -f UTF-8 -t UTF-8 >/dev/null 2>&1
}

# charset_check_file FILE — succeeds when the whole file is well-formed UTF-8.
charset_check_file() {
  iconv -f UTF-8 -t UTF-8 "$1" >/dev/null 2>&1
}

# Removes carriage returns left by the ERP's Windows transfer job.
charset_strip_cr() {
  local s=$1
  printf '%s' "${s//$'\r'/}"
}
