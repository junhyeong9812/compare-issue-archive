#!/usr/bin/env bash
# Runs every tests/test_*.sh and reports which files failed.
cd "$(dirname "$0")" || exit 2
failed=()
for t in test_*.sh; do
  echo "== $t"
  bash "$t"
  rc=$?
  [ "$rc" -eq 0 ] || failed+=("$t (exit $rc)")
done
echo
if [ ${#failed[@]} -eq 0 ]; then
  echo "all test files passed"
  exit 0
fi
printf 'failed: %s\n' "${failed[@]}"
exit 1
