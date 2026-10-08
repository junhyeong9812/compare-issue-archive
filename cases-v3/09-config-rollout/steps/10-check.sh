#!/usr/bin/env bash
# Step 10 — validate the release file and stage it in the run directory.
ROLLOUT_STEP=check
. "$(dirname "$0")/_env.sh"

rel=${1:?release id}
file=$(release_file "$rel")
require_file "$file" "release"
[ "$(release_id "$file")" = "$rel" ] || die "release header does not match $rel"
release_check "$file" || die "release $rel has invalid lines" 3

dir=$(run_dir "$rel")
rm -rf "$dir"
ensure_dir "$dir"
release_changes "$file" > "$dir/changes.tsv"
active_hosts > "$dir/hosts.tsv"
log "$rel: $(wc -l < "$dir/changes.tsv" | tr -d ' ') changes, $(wc -l < "$dir/hosts.tsv" | tr -d ' ') hosts"
