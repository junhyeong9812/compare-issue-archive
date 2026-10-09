# common.sh — logging, configuration loading and small guards shared by
# the wrapper and every step script.

log()  { printf '[%s] %s\n' "${ROLLOUT_STEP:-rollout}" "$*" >&2; }
warn() { printf '[%s] warn: %s\n' "${ROLLOUT_STEP:-rollout}" "$*" >&2; }
die()  { printf '[%s] error: %s\n' "${ROLLOUT_STEP:-rollout}" "$*" >&2; exit "${2:-1}"; }

load_conf() {
  local f="$ROLLOUT_HOME/conf/rollout.conf"
  [ -r "$f" ] || die "missing config $f"
  # shellcheck disable=SC1090
  . "$f"
}

require_file() {
  [ -f "$1" ] || die "${2:-file} not found: $1"
  [ -r "$1" ] || die "${2:-file} not readable: $1"
}

ensure_dir() {
  mkdir -p "$1" 2>/dev/null || die "cannot create directory $1"
  [ -w "$1" ] || die "directory not writable: $1"
}

# Per-release working area: $WORK_DIR/<release id>
run_dir() { printf '%s/%s' "$WORK_DIR" "$1"; }

host_out() { printf '%s/hosts/%s/app.conf' "$OUT_DIR" "$1"; }
