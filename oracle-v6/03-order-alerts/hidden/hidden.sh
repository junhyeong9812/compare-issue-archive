#!/usr/bin/env bash
set -uo pipefail
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
tsc -p . --outDir "$OUT" || exit 2
status=0
node "$OUT/checks/acceptance.test.js" || status=1
node "$OUT/checks/resend.test.js" || status=1
exit $status
