# Sourced by every step script. Expects SETTLE_HOME (set by bin/settle).
: "${SETTLE_HOME:?run steps through bin/settle}"
for _lib in common charset text money cache feed partners ledger report export; do
  # shellcheck disable=SC1090
  . "$SETTLE_HOME/lib/$_lib.sh"
done
unset _lib
load_conf
