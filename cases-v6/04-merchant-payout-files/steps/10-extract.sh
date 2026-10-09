#!/usr/bin/env bash
# Step 10 — cut the day's rows per active partner out of the acquiring extract.
PAYOUT_STEP=extract
. "$(dirname "$0")/_env.sh"

date=$(check_date "${1:-}")
dir=$(run_dir "$date")
rm -rf "$dir"
ensure_dir "$dir/rows"

total=0
for p in $(active_partners); do
  txns_for "$date" "$p" > "$dir/rows/$p.tsv" || die "extract failed for $p"
  n=$(wc -l < "$dir/rows/$p.tsv" | tr -d ' ')
  total=$((total + n))
  log "$p: $n rows"
done
log "extracted $total rows for $date"
