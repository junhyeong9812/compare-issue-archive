# common.sh — logging, configuration loading and small guards shared by
# the wrapper and every step script.

log()  { printf '[%s] %s\n' "${SETTLE_STEP:-settle}" "$*" >&2; }
warn() { printf '[%s] warn: %s\n' "${SETTLE_STEP:-settle}" "$*" >&2; }
die()  { printf '[%s] error: %s\n' "${SETTLE_STEP:-settle}" "$*" >&2; exit "${2:-1}"; }

load_conf() {
  local f
  for f in "$SETTLE_HOME/conf/settle.conf" "$SETTLE_HOME/conf/layout.conf"; do
    [ -r "$f" ] || die "missing config $f"
    # shellcheck disable=SC1090
    . "$f"
  done
}

require_file() {
  [ -f "$1" ] || die "${2:-file} not found: $1"
  [ -r "$1" ] || die "${2:-file} not readable: $1"
}

ensure_dir() {
  mkdir -p "$1" 2>/dev/null || die "cannot create directory $1"
  [ -w "$1" ] || die "directory not writable: $1"
}

# Validates a business date in YYYYMMDD form and echoes it back.
check_date() {
  local d=$1
  case "$d" in
    [0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]) ;;
    *) die "bad date '$d' (want YYYYMMDD)" ;;
  esac
  local m=$((10#${d:4:2})) day=$((10#${d:6:2}))
  [ "$m" -ge 1 ] && [ "$m" -le 12 ] || die "bad month in $d"
  [ "$day" -ge 1 ] && [ "$day" -le 31 ] || die "bad day in $d"
  printf '%s' "$d"
}

# "20261005" -> "2026-10-05"
date_dash() { printf '%s-%s-%s' "${1:0:4}" "${1:4:2}" "${1:6:2}"; }

feed_path() { printf '%s/%s%s%s' "$FEED_DIR" "$FEED_PREFIX" "$1" "$FEED_SUFFIX"; }

# Per-run working area: $WORK_DIR/<date>
run_dir() { printf '%s/%s' "$WORK_DIR" "$1"; }
