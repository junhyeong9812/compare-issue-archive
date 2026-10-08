#!/usr/bin/env bash
# Step 40 — write the report and the accounting export.
SETTLE_STEP=publish
. "$(dirname "$0")/_env.sh"

date=$(check_date "${1:-}")
dir=$(run_dir "$date")
require_file "$dir/totals.tsv" "totals"
ensure_dir "$OUT_DIR"
partners_load

source=$(cat "$dir/source" 2>/dev/null)
report="$OUT_DIR/report-$date.txt"
export="$OUT_DIR/export-$date.csv"

report_render "$date" "$dir/totals.tsv" "${source:-unknown}" > "$report.tmp" \
  || { rm -f "$report.tmp"; die "report rendering failed"; }
export_csv "$dir/normalized.tsv" > "$export.tmp" \
  || { rm -f "$export.tmp"; die "export failed"; }
mv -f "$report.tmp" "$report" && mv -f "$export.tmp" "$export" || die "publish failed"
log "wrote $report"
log "wrote $export"
