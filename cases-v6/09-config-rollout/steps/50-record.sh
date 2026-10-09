#!/usr/bin/env bash
# Step 50 — note the rollout in the history together with the service mark.
ROLLOUT_STEP=record
. "$(dirname "$0")/_env.sh"

rel=${1:?release id}
dir=$(run_dir "$rel")
mark=$(cfg_mark) || die "could not read the service mark"
history_add "$rel" "$mark" "$(wc -l < "$dir/hosts.tsv" | tr -d ' ')" "$(wc -l < "$dir/applied.tsv" | tr -d ' ')"
log "recorded $rel at $mark"
