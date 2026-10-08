#!/usr/bin/env bash
# Builds the stripped idchk binary into the case's vendor/bin/.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v3/05-member-onboarding/vendor/bin/idchk"
mkdir -p "$(dirname "$OUT")"
gcc -O2 -s -Wall -o "$OUT" "$HERE/idchk.c"
echo "built $OUT"
