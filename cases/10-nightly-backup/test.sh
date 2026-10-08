#!/usr/bin/env bash
# Reproduces the nightly backup scenarios. Exit 0 = all checks pass.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
failures=0
check() { if eval "$2"; then echo "ok   - $1"; else echo "FAIL - $1"; failures=$((failures + 1)); fi; }

# --- scenario 1: normal night ---------------------------------------------
mkdir -p "$WORK/data" "$WORK/backups"
echo "order-1001,PAID" > "$WORK/data/orders.csv"
SRC_DIR="$WORK/data" DEST_DIR="$WORK/backups" STAMP=20261001 \
  bash "$HERE/backup.sh" > "$WORK/run1.log" 2>&1
rc=$?
check "normal run exits 0" '[ "$rc" -eq 0 ]'
check "normal run publishes LATEST=20261001" '[ "$(cat "$WORK/backups/LATEST" 2>/dev/null)" = 20261001 ]'
check "archive contains orders.csv" 'tar -tzf "$WORK/backups/orders-20261001.tar.gz" 2>/dev/null | grep -q orders.csv'

# --- scenario 2: data directory not mounted -------------------------------
SRC_DIR="$WORK/not-mounted" DEST_DIR="$WORK/backups" STAMP=20261002 \
  bash "$HERE/backup.sh" > "$WORK/run2.log" 2>&1
rc=$?
check "run with missing data dir exits non-zero (got $rc)" '[ "$rc" -ne 0 ]'
check "LATEST still points at last good backup (got $(cat "$WORK/backups/LATEST" 2>/dev/null))" \
  '[ "$(cat "$WORK/backups/LATEST" 2>/dev/null)" = 20261001 ]'

echo "failures=$failures"
[ "$failures" -eq 0 ]
