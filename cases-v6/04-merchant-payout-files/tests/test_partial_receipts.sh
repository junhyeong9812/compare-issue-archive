#!/usr/bin/env bash
# The receipts report on a split day where one file did not make it to the
# partner box (relay outage on one upload), and after the re-run.
. "$(dirname "$0")/lib.sh"
t_setup

# A transfer CLI that refuses one remote name, as during a relay outage.
cat > "$T_WORK/xferq-outage" <<SH
#!/usr/bin/env bash
if [ "\$1" = send ]; then
  for a in "\$@"; do
    [ "\$a" = "\$XFERQ_OUTAGE_NAME" ] && { echo "E31" >&2; exit 4; }
  done
fi
exec "$T_HOME/vendor/bin/xferq" "\$@"
SH
chmod +x "$T_WORK/xferq-outage"

# report line for a day, without the leading date
day_line() { printf '%s\n' "$1" | grep "^$2 " | cut -c11-; }
# does LINE show the number N (not as part of a longer number)?
shows_num() { printf '%s' "$1" | grep -Eq "(^|[^0-9,])$2([^0-9,]|\$)"; }

# 2026-10-02: P001 has 26 rows -> two files (20 + 6); the second one fails.
PAYOUT_XFERQ="$T_WORK/xferq-outage" XFERQ_OUTAGE_NAME=HBP_SETTLE_P001_20261002-2.csv \
  payout run 20261002 2>>"$T_WORK/run.err"; rc=$?
assert_rc "run with one failed upload reports the failure" "$rc" 3

r=$(payout receipts P001 20261001 20261003)
printf '%s\n' "$r"
line=$(day_line "$r" 2026-10-02)
assert_contains "partial day shows how many files arrived" "$line" "1/2"
if shows_num "$line" 20; then ok "partial day shows the 20 detail rows that arrived"; else fail "partial day shows the 20 detail rows that arrived" "$line"; fi
assert_contains "partial day shows the net of the rows that arrived" "$line" "4,041,576"
assert_eq "day with no file stays '-'" "$(day_line "$r" 2026-10-01 | tr -s ' ')" " - -"

r2=$(payout receipts P002 20261002 20261002)
assert_contains "other partner's complete day unchanged" "$(day_line "$r2" 2026-10-02)" "   6        1,279,201"

# re-run after the outage: the day is complete
payout run 20261002 2>>"$T_WORK/run.err"; rc=$?
assert_rc "re-run completes" "$rc" 0
line=$(day_line "$(payout receipts P001 20261002 20261002)" 2026-10-02)
assert_contains "complete day after re-run" "$line" "  26        5,363,775"
if printf '%s' "$line" | grep -q '/'; then fail "complete day has no partial marker" "$line"; else ok "complete day has no partial marker"; fi
t_done
