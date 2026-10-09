# Test helpers. Each test file sources this, calls t_setup, then asserts.
T_HOME=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
T_FAIL=0
T_PASS=0

t_setup() {
  T_WORK=$(mktemp -d)
  trap 't_cleanup' EXIT
  export ROLLOUT_WORK_DIR="$T_WORK/work"
  export ROLLOUT_OUT_DIR="$T_WORK/out"
  export ROLLOUT_HOSTS_FILE="$T_HOME/conf/hosts.conf"
  export ROLLOUT_RELEASE_DIR="$T_HOME/releases"
  export CFGSVC_HOME="$T_WORK/cfgsvc"
  mkdir -p "$CFGSVC_HOME"
}

t_cleanup() { [ -n "${T_WORK:-}" ] && rm -rf "$T_WORK"; }

# Loads the libraries into the test shell (for unit tests).
t_source_libs() {
  export LC_ALL=C.UTF-8
  export ROLLOUT_HOME="$T_HOME"
  . "$T_HOME/steps/_env.sh"
}

rollout() { "$T_HOME/bin/rollout" "$@"; }

ok()   { T_PASS=$((T_PASS + 1)); printf 'ok   - %s\n' "$1"; }
fail() { T_FAIL=$((T_FAIL + 1)); printf 'FAIL - %s\n' "$1"; [ -n "${2:-}" ] && printf '       %s\n' "$2"; return 0; }

assert_eq() {
  if [ "$2" = "$3" ]; then ok "$1"; else fail "$1" "expected [$3] got [$2]"; fi
}

assert_contains() {
  if printf '%s' "$2" | grep -qF -- "$3"; then ok "$1"; else fail "$1" "missing [$3]"; fi
}

assert_rc() {
  if [ "$2" -eq "$3" ]; then ok "$1"; else fail "$1" "expected exit $3 got $2"; fi
}

t_done() {
  printf '# %s: %d passed, %d failed\n' "$(basename "$0")" "$T_PASS" "$T_FAIL"
  [ "$T_FAIL" -eq 0 ]
}
