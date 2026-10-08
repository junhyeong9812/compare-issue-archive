#!/usr/bin/env bash
# Nightly backup of the orders data directory.
#
#   SRC_DIR   directory to back up            (default /var/lib/orders)
#   DEST_DIR  where archives are kept         (default /var/backups/orders)
#   KEEP      number of archives to retain    (default 7)
#   STAMP     archive date stamp              (default today, YYYYMMDD)
#
# The scheduler records the run as SUCCESS when this script exits 0.

SRC_DIR=${SRC_DIR:-/var/lib/orders}
DEST_DIR=${DEST_DIR:-/var/backups/orders}
KEEP=${KEEP:-7}
STAMP=${STAMP:-$(date +%Y%m%d)}

archive="$DEST_DIR/orders-$STAMP.tar.gz"

echo "[backup] preparing $DEST_DIR"
mkdir -p "$DEST_DIR"

echo "[backup] archiving $SRC_DIR -> $archive"
tar -czf "$archive" -C "$SRC_DIR" .

echo "[backup] verifying archive"
tar -tzf "$archive" > /dev/null

echo "[backup] rotating (keep $KEEP)"
ls -1 "$DEST_DIR"/orders-*.tar.gz | sort -r | tail -n +"$((KEEP + 1))" | xargs -r rm -f

echo "[backup] publishing LATEST"
echo "$STAMP" > "$DEST_DIR/LATEST"

echo "[backup] done"
