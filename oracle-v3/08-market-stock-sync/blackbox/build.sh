#!/usr/bin/env bash
# Builds the stripped stockpush binary into the case's vendor/bin/.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v3/08-market-stock-sync/vendor/bin/stockpush"
mkdir -p "$(dirname "$OUT")"
gcc -O2 -s -Wall -o "$OUT" "$HERE/stockpush.c"
echo "built $OUT"
