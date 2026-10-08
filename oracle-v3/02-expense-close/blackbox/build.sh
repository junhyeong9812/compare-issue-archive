#!/usr/bin/env bash
# Builds the stripped ledgerctl binary into the case's vendor/bin/.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v3/02-expense-close/vendor/bin/ledgerctl"
mkdir -p "$(dirname "$OUT")"
gcc -O2 -s -Wall -o "$OUT" "$HERE/ledgerctl.c"
echo "built $OUT"
