#!/usr/bin/env bash
# Step 30 — hand every built file to the transfer CLI and record the ticket.
PAYOUT_STEP=ship
. "$(dirname "$0")/_env.sh"

date=$(check_date "${1:-}")
dir=$(run_dir "$date")
require_file "$dir/files.lst" "file list"

sent=0 failed=0
while IFS='|' read -r p name rows; do
  [ -n "$name" ] || continue
  if ticket=$(xfer_send "$p" "$dir/files/$name" "$name"); then
    manifest_add "$date" "$p" "$name" "$ticket" "$rows"
    log "sent $name ($rows rows) -> $ticket"
    sent=$((sent + 1))
  else
    failed=$((failed + 1))
  fi
done < "$dir/files.lst"
manifest_compact
[ "$failed" -eq 0 ] || die "$failed file(s) not sent" 3
log "shipped $sent files for $date"
