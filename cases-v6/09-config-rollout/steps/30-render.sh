#!/usr/bin/env bash
# Step 30 — render app.conf for every active host from the configuration service.
ROLLOUT_STEP=render
. "$(dirname "$0")/_env.sh"

rel=${1:?release id}
dir=$(run_dir "$rel")
require_file "$dir/hosts.tsv" "host list"
cache_init || die "cannot create lookup cache"

# Hosts whose settings came out the same keep their existing file as it is,
# header included (OPS-1874), so the header names the release that last
# changed the host, not necessarily this one.
# TODO: people keep asking why the header shows an older release — could we
#       simply rewrite every file again now?
n=0 kept=0
while IFS=$'\t' read -r host pool; do
  [ -n "$host" ] || continue
  out=$(host_out "$host")
  ensure_dir "$(dirname "$out")"
  render_host "$host" "$pool" "$rel" > "$out.tmp" || { rm -f "$out.tmp"; die "render failed for $host"; }
  if [ -f "$out" ] && cmp -s <(tail -n +2 "$out") <(tail -n +2 "$out.tmp"); then
    rm -f "$out.tmp"
    kept=$((kept + 1))
  else
    mv -f "$out.tmp" "$out" || die "cannot write $out"
  fi
  n=$((n + 1))
done < "$dir/hosts.tsv"
log "rendered $n hosts ($kept unchanged, left as they were)"
