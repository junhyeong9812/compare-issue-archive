#!/usr/bin/env bash
# Hidden judge: for another week and another partner set, every settlement
# detail row must be downloadable by the partner through the transfer service
# itself (checked straight through vendor/bin/xferq, not only through our
# receipts report), within the partner file contract, with nothing left behind
# in the transfer spool.
set -u
export LC_ALL=C.UTF-8
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
failures=0
ok()   { echo "ok   - $1"; }
bad()  { echo "FAIL - $1"; [ -n "${2:-}" ] && echo "       $2"; failures=$((failures + 1)); }
check() { if eval "$2"; then ok "$1"; else bad "$1"; fi; }
X="$HERE/vendor/bin/xferq"

# --- fixture: rows per day for 2026-11-28 .. 2026-12-02 ----------------------
gen() {  # spec month-start-day seed
  awk -v spec="$1" -v seed="$3" -v start="$2" '
    function day(i,   y, m, d) {   # start = 20261128, five consecutive days
      split("20261128 20261129 20261130 20261201 20261202", ds, " "); return ds[i] }
    BEGIN {
      n = split(spec, ps, ";"); s = seed
      for (i = 1; i <= n; i++) {
        split(ps[i], kv, ":"); p = kv[1]; m = split(kv[2], cnt, ",")
        for (d = 1; d <= m; d++)
          for (k = 1; k <= cnt[d]; k++) {
            s = (s * 1103515245 + 12345) % 2147483648
            amt = (int(s / 65536) % 9000 + 3) * 100
            fee = int(amt * 18 / 1000)
            printf "%s\t%s\tN%s%d%04d\t%d\t%d\n", day(d), p, p, d, k, amt, fee
          }
      }
    }'
}
{
  printf '# settle_date\tpartner\ttxn\tamount\tfee\n'
  # Q101: 20 (at the limit), 21 (one over), 45 (three parts), 0 (empty day), 40 (two full parts)
  # Q202: small every day, one day with 41 rows
  # Q303: suspended
  gen "Q101:20,21,45,0,40;Q202:3,1,41,2,5;Q303:4,4,4,4,4" 20261128 31
} | sort -t$'\t' -k1,1 -k2,2 -k3,3 > "$WORK/txns.tsv"
cat > "$WORK/partners.conf" <<'P'
# code|name|cycle|status
Q101|새봄유통|daily|A
Q202|가람서점|daily|A
Q303|옛날다방|daily|I
P

export PAYOUT_TXN_FILE="$WORK/txns.tsv" PAYOUT_PARTNERS_FILE="$WORK/partners.conf"
export PAYOUT_WORK_DIR="$WORK/work" PAYOUT_OUT_DIR="$WORK/out" XFERQ_HOME="$WORK/spool"
mkdir -p "$XFERQ_HOME"
DAYS="20261128 20261129 20261130 20261201 20261202"

for d in $DAYS; do
  "$HERE/bin/payout" run "$d" 2>>"$WORK/run.err"; rc=$?
  check "run $d completes (exit $rc)" '[ "$rc" -eq 0 ]'
done
# a re-run of the same day must not leave anything behind either
"$HERE/bin/payout" run 20261130 2>>"$WORK/run.err"; rc=$?
check "re-run 20261130 completes (exit $rc)" '[ "$rc" -eq 0 ]'

# --- what the partners can download, straight from the transfer service ------
: > "$WORK/got.tsv"; : > "$WORK/parts.tsv"
for p in Q101 Q202; do
  "$X" ls --to "$p" > "$WORK/ls.$p" 2>/dev/null || bad "xferq ls $p"
  while IFS=$'\t' read -r name bytes ticket; do
    [ -n "$name" ] || continue
    "$X" pull --to "$p" --name "$name" > "$WORK/f" 2>/dev/null || { bad "pull $p $name"; continue; }
    tr -d '\r' < "$WORK/f" > "$WORK/f.lf"
    hdr=$(head -1 "$WORK/f.lf")
    IFS=, read -r h_tag h_sender h_partner h_date h_part <<< "$hdr"
    [ "$h_tag" = H ] && [ "$h_sender" = HBP ] && [ "$h_partner" = "$p" ] \
      || bad "$name header is H,HBP,$p,..." "$hdr"
    case "$name" in
      "HBP_SETTLE_${p}_${h_date}.csv"|"HBP_SETTLE_${p}_${h_date}"[!0-9]*.csv) ;;
      *) bad "$name keeps the agreed prefix HBP_SETTLE_${p}_${h_date}" ;;
    esac
    [ "$(grep -c $'\r$' "$WORK/f")" -eq "$(wc -l < "$WORK/f" | tr -d ' ')" ] || bad "$name lines end in CRLF"
    rows=$(grep -c '^D,' "$WORK/f.lf")
    [ "$rows" -le 20 ] || bad "$name holds at most 20 detail rows (has $rows)"
    trl=$(tail -1 "$WORK/f.lf")
    sum=$(awk -F, '$1 == "D" { s += $6 } END { print s + 0 }' "$WORK/f.lf")
    [ "$trl" = "T,$rows,$sum" ] || bad "$name trailer matches its rows" "$trl vs T,$rows,$sum"
    printf '%s\t%s\t%s\n' "$p" "$h_date" "$h_part" >> "$WORK/parts.tsv"
    awk -F, -v p="$p" '$1 == "D" { print $3 "\t" p "\t" $2 "\t" $4 "\t" $5 }' "$WORK/f.lf" >> "$WORK/got.tsv"
  done < "$WORK/ls.$p"
done

expected=$(awk -F'\t' '!/^#/ && $2 != "Q303"' "$WORK/txns.tsv" | sort)
got=$(sort "$WORK/got.tsv")
if [ "$expected" = "$got" ]; then
  ok "every detail row is downloadable exactly once ($(printf '%s\n' "$got" | grep -c .) rows)"
else
  bad "every detail row is downloadable exactly once" \
      "expected $(printf '%s\n' "$expected" | grep -c .) rows, partners can download $(printf '%s\n' "$got" | grep -c .)"
  diff <(printf '%s\n' "$expected" | cut -f1,2 | uniq -c) <(printf '%s\n' "$got" | cut -f1,2 | uniq -c) | head -10
fi

# every day has a complete set of parts 1/n .. n/n for each active partner
for p in Q101 Q202; do
  for d in $DAYS; do
    set=$(awk -F'\t' -v p="$p" -v d="$d" '$1 == p && $2 == d { print $3 }' "$WORK/parts.tsv" 2>/dev/null | sort -t/ -k1,1n | tr '\n' ' ')
    n=$(printf '%s' "$set" | awk '{ split($1, a, "/"); print a[2] + 0 }')
    want=""
    [ "${n:-0}" -ge 1 ] && for i in $(seq 1 "$n"); do want="$want$i/$n "; done
    [ -n "$set" ] && [ "$set" = "$want" ] && ok "$p $d parts complete ($set)" || bad "$p $d parts complete" "got [$set]"
  done
done
expect_parts() { local c; c=$(awk -F'\t' -v p="$1" -v d="$2" '$1 == p && $2 == d' "$WORK/parts.tsv" | wc -l); [ "$c" -eq "$3" ] && ok "$1 $2 sent as $3 file(s)" || bad "$1 $2 sent as $3 file(s)" "got $c"; }
expect_parts Q101 20261128 1
expect_parts Q101 20261129 2
expect_parts Q101 20261130 3
expect_parts Q101 20261201 1
expect_parts Q101 20261202 2
expect_parts Q202 20261130 3

# nothing handed to the transfer service may be left undelivered
i=1 states=""
while t=$(printf 'T%08d' "$i") && line=$("$X" status "$t" 2>/dev/null); do
  st=${line##*$'\t'}
  states="$states $st"
  [ "$st" = 2 ] || bad "transfer $t is in the partner box" "$line"
  i=$((i + 1))
done
check "transfer service saw the uploads ($((i - 1)) tickets)" '[ "$i" -gt 10 ]'

# the ops receipts report agrees with what the partner can download
r=$("$HERE/bin/payout" receipts Q101 20261128 20261202 2>/dev/null)
for pair in "2026-11-28:20" "2026-11-29:21" "2026-11-30:45" "2026-12-01:-" "2026-12-02:40"; do
  d=${pair%%:*} n=${pair#*:}
  line=$(printf '%s\n' "$r" | grep "^$d ")
  [ "$(printf '%s' "$line" | awk '{ print $2 }')" = "$n" ] && ok "receipts $d shows $n" || bad "receipts $d shows $n" "$line"
done


# --- split days where one upload failed (relay outage on one remote name) -----
# The count and net columns are what the on-call reads as "the partner can load
# this day"; a day with missing parts must not show numbers there. What did
# arrive must still be visible on the line (the change request).
W2="$WORK/partial"; mkdir -p "$W2/spool"
cat > "$W2/xferq-outage" <<SH
#!/usr/bin/env bash
if [ "\$1" = send ]; then
  for a in "\$@"; do
    [ "\$a" = "\$XFERQ_OUTAGE_NAME" ] && { echo "E31" >&2; exit 4; }
  done
fi
exec "$X" "\$@"
SH
chmod +x "$W2/xferq-outage"
export PAYOUT_WORK_DIR="$W2/work" PAYOUT_OUT_DIR="$W2/out" XFERQ_HOME="$W2/spool"

outage_run() {  # date failing-name
  PAYOUT_XFERQ="$W2/xferq-outage" XFERQ_OUTAGE_NAME="$2" "$HERE/bin/payout" run "$1" 2>>"$WORK/run.err"
}
# net of Q101's rows on DAY, parts given as a list (1-based, 20 rows per part)
arrived() {  # day "parts" -> "<rows> <net formatted>"
  awk -F'\t' -v d="$1" '!/^#/ && $1 == d && $2 == "Q101"' "$WORK/txns.tsv" | sort -t$'\t' -k3,3 \
    | awk -F'\t' -v parts=" $2 " '{ k = int((NR - 1) / 20) + 1; if (index(parts, " " k " ")) { n++; s += $4 - $5 } }
        END { printf "%d %d\n", n, s }'
}
fmt() { printf '%s' "$1" | rev | sed 's/[0-9]\{3\}/&,/g' | rev | sed 's/^,//'; }
line_of() { printf '%s\n' "$1" | grep "^$2 "; }
not_counted() {  # line: count and net columns hold no numbers
  printf '%s\n' "$1" | awk '{ exit !($2 !~ /^[0-9]+$/ && $3 !~ /^-?[0-9,]+$/) }'
}
shows() { printf '%s' "$1" | cut -c11- | grep -Eq "(^|[^0-9,])$2([^0-9,]|\$)"; }

outage_run 20261130 HBP_SETTLE_Q101_20261130-2.csv; rc=$?
check "outage run 20261130 reports the failed upload (exit $rc)" '[ "$rc" -eq 3 ]'
outage_run 20261129 HBP_SETTLE_Q101_20261129-1.csv; rc=$?
check "outage run 20261129 reports the failed upload (exit $rc)" '[ "$rc" -eq 3 ]'
"$HERE/bin/payout" run 20261128 2>>"$WORK/run.err"
"$HERE/bin/payout" run 20261201 2>>"$WORK/run.err"

r=$("$HERE/bin/payout" receipts Q101 20261128 20261201 2>/dev/null); rc=$?
check "receipts with partial days exits 0 (exit $rc)" '[ "$rc" -eq 0 ]'
echo "$r"

l30=$(line_of "$r" 2026-11-30); read -r n30 s30 <<< "$(arrived 20261130 "1 3")"
if not_counted "$l30"; then ok "2/3 day is not shown as receivable in the count/net columns"
else bad "2/3 day is not shown as receivable in the count/net columns" "$l30"; fi
case "$l30" in *2/3*) ok "2/3 day shows 2/3" ;; *) bad "2/3 day shows 2/3" "$l30" ;; esac
shows "$l30" "$n30" && ok "2/3 day shows the $n30 rows that arrived" || bad "2/3 day shows the $n30 rows that arrived" "$l30"
case "$l30" in *"$(fmt "$s30")"*) ok "2/3 day shows the arrived net" ;; *) bad "2/3 day shows the arrived net $(fmt "$s30")" "$l30" ;; esac

l29=$(line_of "$r" 2026-11-29); read -r n29 s29 <<< "$(arrived 20261129 "2")"
if not_counted "$l29"; then ok "1/2 day is not shown as receivable in the count/net columns"
else bad "1/2 day is not shown as receivable in the count/net columns" "$l29"; fi
case "$l29" in *1/2*) ok "1/2 day shows 1/2" ;; *) bad "1/2 day shows 1/2" "$l29" ;; esac
case "$l29" in *"$(fmt "$s29")"*) ok "1/2 day shows the arrived net" ;; *) bad "1/2 day shows the arrived net $(fmt "$s29")" "$l29" ;; esac

l28=$(line_of "$r" 2026-11-28)
[ "$(printf '%s' "$l28" | awk '{ print $2 }')" = 20 ] && ! printf '%s' "$l28" | grep -q / \
  && ok "complete day unchanged" || bad "complete day unchanged" "$l28"
l01=$(line_of "$r" 2026-12-01)
[ "$(printf '%s' "$l01" | tr -s ' ')" = "2026-12-01 - -" ] && ok "empty day unchanged" || bad "empty day unchanged" "$l01"

r2=$("$HERE/bin/payout" receipts Q202 20261130 20261130 2>/dev/null)
[ "$(line_of "$r2" 2026-11-30 | awk '{ print $2 }')" = 41 ] && ok "other partner's split day complete" || bad "other partner's split day complete" "$(line_of "$r2" 2026-11-30)"

# the on-call re-runs the days; both become complete
"$HERE/bin/payout" run 20261130 2>>"$WORK/run.err"; "$HERE/bin/payout" run 20261129 2>>"$WORK/run.err"
r=$("$HERE/bin/payout" receipts Q101 20261129 20261130 2>/dev/null)
for pair in "2026-11-29:21" "2026-11-30:45"; do
  d=${pair%%:*} n=${pair#*:}
  line=$(line_of "$r" "$d")
  [ "$(printf '%s' "$line" | awk '{ print $2 }')" = "$n" ] && ! printf '%s' "$line" | grep -q / \
    && ok "after re-run $d shows $n" || bad "after re-run $d shows $n" "$line"
done

echo "# hidden: $failures failure(s)"
[ "$failures" -eq 0 ]
