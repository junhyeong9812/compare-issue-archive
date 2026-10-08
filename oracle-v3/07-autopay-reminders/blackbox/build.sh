#!/usr/bin/env bash
# pushq 블랙박스 빌드 → 케이스 vendor/bin 에 strip 바이너리로 배치
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
DEST="$HERE/../../../cases-v3/07-autopay-reminders/vendor/bin"
mkdir -p "$DEST"
gcc -O2 -s -o "$DEST/pushq" "$HERE/pushq.c"
echo "built $DEST/pushq"
