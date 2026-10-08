#!/usr/bin/env bash
# Hidden judge: same invariant (failed night => exit!=0 and LATEST unchanged; good night => exit 0) via other paths.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
failures=0
check() { if eval "$2"; then echo "ok   - $1"; else echo "FAIL - $1"; failures=$((failures + 1)); fi; }
run() { bash "$HERE/backup.sh" > "$WORK/last.log" 2>&1; }

mkdir -p "$WORK/inventory" "$WORK/vault"
echo "sku-77,12" > "$WORK/inventory/stock.csv"

# good nights with retention
for d in 20260301 20260302 20260303; do
  SRC_DIR="$WORK/inventory" DEST_DIR="$WORK/vault" KEEP=2 STAMP=$d run; rc=$?
  check "good night $d exits 0 (got $rc)" '[ "$rc" -eq 0 ]'
done
check "retention keeps 2 archives" '[ "$(ls "$WORK"/vault/orders-*.tar.gz | wc -l)" -eq 2 ]'
check "LATEST=20260303" '[ "$(cat "$WORK/vault/LATEST")" = 20260303 ]'

# source path is a regular file, not a directory
echo x > "$WORK/notadir"
SRC_DIR="$WORK/notadir" DEST_DIR="$WORK/vault" KEEP=2 STAMP=20260304 run; rc=$?
check "source-is-file night exits non-zero (got $rc)" '[ "$rc" -ne 0 ]'
check "LATEST unchanged after source-is-file night" '[ "$(cat "$WORK/vault/LATEST")" = 20260303 ]'

# archive is written but fails verification (tar stub: -t fails, everything else real)
mkdir -p "$WORK/bin"
REAL_TAR="$(command -v tar)"
cat > "$WORK/bin/tar" <<STUB
#!/usr/bin/env bash
case "\$1" in -t*) echo "gzip: stdin: unexpected end of file" >&2; exit 2;; esac
exec "$REAL_TAR" "\$@"
STUB
chmod +x "$WORK/bin/tar"
PATH="$WORK/bin:$PATH" SRC_DIR="$WORK/inventory" DEST_DIR="$WORK/vault" KEEP=2 STAMP=20260305 run; rc=$?
check "verify-failure night exits non-zero (got $rc)" '[ "$rc" -ne 0 ]'
check "LATEST unchanged after verify-failure night" '[ "$(cat "$WORK/vault/LATEST")" = 20260303 ]'

# recovery night succeeds again
SRC_DIR="$WORK/inventory" DEST_DIR="$WORK/vault" KEEP=2 STAMP=20260306 run; rc=$?
check "recovery night exits 0 (got $rc)" '[ "$rc" -eq 0 ]'
check "LATEST=20260306" '[ "$(cat "$WORK/vault/LATEST")" = 20260306 ]'

echo "failures=$failures"
[ "$failures" -eq 0 ]
