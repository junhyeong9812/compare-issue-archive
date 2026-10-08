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

echo "# hidden: $failures failure(s)"
[ "$failures" -eq 0 ]
