#!/usr/bin/env bash
# Builds the stripped cfgsvc binary into the case's vendor/bin/.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v3/09-config-rollout/vendor/bin/cfgsvc"
mkdir -p "$(dirname "$OUT")"
gcc -O2 -s -Wall -o "$OUT" "$HERE/cfgsvc.c"
echo "built $OUT"
