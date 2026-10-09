#!/usr/bin/env bash
# Builds the stripped ptsctl binary into the case's vendor/bin/.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v4/10-membership-points/vendor/bin/ptsctl"
mkdir -p "$(dirname "$OUT")"
gcc -O2 -s -Wall -o "$OUT" "$HERE/ptsctl.c"
echo "built $OUT"
