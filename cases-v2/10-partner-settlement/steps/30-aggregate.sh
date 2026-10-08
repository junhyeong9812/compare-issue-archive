#!/usr/bin/env bash
# Step 30 — per-partner totals.
SETTLE_STEP=aggregate
. "$(dirname "$0")/_env.sh"

date=$(check_date "${1:-}")
dir=$(run_dir "$date")
require_file "$dir/normalized.tsv" "normalized records"

ledger_totals "$dir/normalized.tsv" > "$dir/totals.tsv.tmp" \
  && mv -f "$dir/totals.tsv.tmp" "$dir/totals.tsv" \
  || die "aggregation failed"
log "$(wc -l < "$dir/totals.tsv") partners"
