#!/usr/bin/env bash
# Step 20 — write the release's changes to the configuration service.
ROLLOUT_STEP=apply
. "$(dirname "$0")/_env.sh"

rel=${1:?release id}
dir=$(run_dir "$rel")
require_file "$dir/changes.tsv" "staged changes"

: > "$dir/applied.tsv"
while IFS=$'\t' read -r key val; do
  [ -n "$key" ] || continue
  chg=$(cfg_set "$key" "$val") || die "could not apply $key"
  printf '%s\t%s\n' "$key" "$chg" >> "$dir/applied.tsv"
  log "$key -> $chg"
done < "$dir/changes.tsv"
log "applied $(wc -l < "$dir/applied.tsv" | tr -d ' ') changes"

# The service keeps answering lookups of keys that already had a value with
# the value as of its last mark; the changes above only become visible to
# `get` (and so to rendering) after the next mark.
mark=$(cfg_mark) || die "could not mark the applied changes"
log "changes visible at $mark"
