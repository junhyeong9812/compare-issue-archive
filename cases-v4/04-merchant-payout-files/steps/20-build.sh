#!/usr/bin/env bash
# Step 20 — build the settlement files (split to the partner row limit).
PAYOUT_STEP=build
. "$(dirname "$0")/_env.sh"

date=$(check_date "${1:-}")
dir=$(run_dir "$date")
[ -d "$dir/rows" ] || die "nothing extracted for $date"
ensure_dir "$dir/files"
: > "$dir/files.lst"

for rows in "$dir"/rows/*.tsv; do
  [ -e "$rows" ] || continue
  p=$(basename "$rows" .tsv)
  parts=$(chunk_rows "$rows" "$MAX_ROWS_PER_FILE" "$dir/rows/$p.part") || die "split failed for $p"
  for i in $(seq 1 "$parts"); do
    chunk=$(printf '%s/rows/%s.part.%02d' "$dir" "$p" "$i")
    name=$(settle_name "$p" "$date" "$i" "$parts")
    csv_render "$date" "$p" "$i" "$parts" < "$chunk" > "$dir/files/$name" \
      || die "render failed for $name"
    printf '%s|%s|%s\n' "$p" "$name" "$(wc -l < "$chunk" | tr -d ' ')" >> "$dir/files.lst"
  done
done
log "built $(wc -l < "$dir/files.lst" | tr -d ' ') files"
