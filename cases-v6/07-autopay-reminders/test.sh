#!/usr/bin/env bash
set -uo pipefail
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
tsc -p . --outDir "$OUT" || exit 2
status=0
for f in "$OUT"/tests/*.test.js; do
  echo "# $(basename "$f" .test.js)"
  node "$f" || status=1
done
exit $status
