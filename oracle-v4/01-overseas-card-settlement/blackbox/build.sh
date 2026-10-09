#!/usr/bin/env bash
# Builds the stripped vendor binary into the case folder.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../../../cases-v4/01-overseas-card-settlement/vendor/bin"
mkdir -p "$OUT"
gcc -O2 -s -Wall -Wextra -o "$OUT/fxq" "$HERE/fxq.c"
