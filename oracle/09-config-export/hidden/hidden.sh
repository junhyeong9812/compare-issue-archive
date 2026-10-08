#!/usr/bin/env bash
set -euo pipefail
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
tsc -p . --outDir "$OUT"
node "$OUT/hidden.test.js"
