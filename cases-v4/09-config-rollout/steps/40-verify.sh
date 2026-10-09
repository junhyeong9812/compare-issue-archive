#!/usr/bin/env bash
# Step 40 — re-read every rendered file and compare it with the service.
ROLLOUT_STEP=verify
. "$(dirname "$0")/_env.sh"

rel=${1:?release id}
dir=$(run_dir "$rel")
cache_init || die "cannot create lookup cache"

bad=0
while IFS=$'\t' read -r host pool; do
  [ -n "$host" ] || continue
  file=$(host_out "$host")
  require_file "$file" "rendered config"
  for s in $(template_settings); do
    line_key=$(grep -o "^[a-z_]* = {{$s}}" "$TEMPLATE" | cut -d' ' -f1)
    want=$(resolve "$host" "$pool" "$s")
    got=$(rendered_value "$file" "$line_key")
    if [ "$want" != "$got" ]; then
      warn "$host: $line_key is [$got], service has [$want]"
      bad=1
    fi
  done
done < "$dir/hosts.tsv"
[ "$bad" -eq 0 ] || die "rendered files do not match the configuration service" 4
log "verified $(wc -l < "$dir/hosts.tsv" | tr -d ' ') hosts"
