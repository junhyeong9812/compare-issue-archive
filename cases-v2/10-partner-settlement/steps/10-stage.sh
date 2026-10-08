#!/usr/bin/env bash
# Step 10 — copy the day's feed into the run directory after basic checks.
SETTLE_STEP=stage
. "$(dirname "$0")/_env.sh"

date=$(check_date "${1:-}")
feed=$(feed_path "$date")
require_file "$feed" "feed"

hdr=$(feed_header "$feed") || die "feed has no header record: $feed"
hdr_date=${hdr%%$'\t'*}
[ "$hdr_date" = "$date" ] || die "feed header date $hdr_date does not match $date"

dir=$(run_dir "$date")
ensure_dir "$dir"
cp -f "$feed" "$dir/feed.dat" || die "copy failed"
printf '%s\n' "${hdr#*$'\t'}" > "$dir/source"
log "staged $(basename "$feed") ($(wc -l < "$feed") lines)"
