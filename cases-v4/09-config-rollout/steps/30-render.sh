#!/usr/bin/env bash
# Step 30 — render app.conf for every active host from the configuration service.
ROLLOUT_STEP=render
. "$(dirname "$0")/_env.sh"

rel=${1:?release id}
dir=$(run_dir "$rel")
require_file "$dir/hosts.tsv" "host list"
cache_init || die "cannot create lookup cache"

n=0
while IFS=$'\t' read -r host pool; do
  [ -n "$host" ] || continue
  out=$(host_out "$host")
  ensure_dir "$(dirname "$out")"
  render_host "$host" "$pool" "$rel" > "$out.tmp" || { rm -f "$out.tmp"; die "render failed for $host"; }
  mv -f "$out.tmp" "$out" || die "cannot write $out"
  n=$((n + 1))
done < "$dir/hosts.tsv"
log "rendered $n hosts"
