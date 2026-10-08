#!/usr/bin/env bash
# Builds the stripped xferq binary into the case's vendor/bin/.
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v3/04-merchant-payout-files/vendor/bin/xferq"
mkdir -p "$(dirname "$OUT")"
gcc -O2 -s -Wall -o "$OUT" "$HERE/xferq.c"
echo "built $OUT"
