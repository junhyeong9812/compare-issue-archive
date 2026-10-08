#!/usr/bin/env bash
# Step 20 — decode the fixed-width feed into normalized.tsv (UTF-8).
SETTLE_STEP=normalize
. "$(dirname "$0")/_env.sh"

date=$(check_date "${1:-}")
dir=$(run_dir "$date")
require_file "$dir/feed.dat" "staged feed"

tmp="$dir/normalized.tsv.tmp"
feed_records "$dir/feed.dat" > "$tmp" || { rm -f "$tmp"; die "decode failed"; }
mv -f "$tmp" "$dir/normalized.tsv" || die "cannot write normalized.tsv"

ledger_check "$dir/feed.dat" "$dir/normalized.tsv"
case $? in
  0) log "control totals match" ;;
  1) die "control totals mismatch — refusing to continue" 3 ;;
  *) warn "control totals not verified" ;;
esac
log "normalized $(wc -l < "$dir/normalized.tsv") records"
