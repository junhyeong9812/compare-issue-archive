#!/usr/bin/env bash
# Builds the stripped pgsim binary into the case's vendor/bin/.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v3/06-partial-refund/vendor/bin/pgsim"
mkdir -p "$(dirname "$OUT")"
gcc -O2 -s -Wall -o "$OUT" "$HERE/pgsim.c"
echo "built $OUT"
