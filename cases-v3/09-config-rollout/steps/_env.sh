# Sourced by every step script. Expects ROLLOUT_HOME (set by bin/rollout).
: "${ROLLOUT_HOME:?run steps through bin/rollout}"
for _lib in common text cache cfg inventory release resolve render audit; do
  # shellcheck disable=SC1090
  . "$ROLLOUT_HOME/lib/$_lib.sh"
done
unset _lib
load_conf
