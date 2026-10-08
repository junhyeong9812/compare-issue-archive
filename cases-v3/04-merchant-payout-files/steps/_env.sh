# Sourced by every step script. Expects PAYOUT_HOME (set by bin/payout).
: "${PAYOUT_HOME:?run steps through bin/payout}"
for _lib in common dates money txns csvfile chunk naming xfer manifest receipts; do
  # shellcheck disable=SC1090
  . "$PAYOUT_HOME/lib/$_lib.sh"
done
unset _lib
load_conf
