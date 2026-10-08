#!/usr/bin/env bash
# 케이스 자동 검증: buggy 상태에서 test.sh·hidden.sh 실패, fix.patch + hidden 적용 후 둘 다 통과해야 한다.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
pass=0; fail=0
for case_dir in "$ROOT"/cases/*/; do
  name="$(basename "$case_dir")"
  oracle="$ROOT/oracle/$name"
  [ -n "${ONLY:-}" ] && [[ "$name" != ${ONLY}* ]] && continue
  tmp="$(mktemp -d)"
  cp -r "$case_dir". "$tmp/"
  errs=()
  (cd "$tmp" && bash test.sh >"$tmp/.buggy.out" 2>&1) && errs+=("buggy test.sh 통과(실패해야 함)")
  b="$(mktemp -d)"; cp -r "$case_dir". "$b/"; cp -r "$oracle/hidden/." "$b/"
  (cd "$b" && bash hidden.sh >"$b/.hidden-buggy.out" 2>&1) && errs+=("buggy hidden.sh 통과(실패해야 함)")
  rm -rf "$b"
  if ! (cd "$tmp" && patch -s -p1 < "$oracle/fix.patch"); then
    errs+=("fix.patch 적용 실패")
  else
    cp -r "$oracle/hidden/." "$tmp/"
    (cd "$tmp" && bash test.sh >"$tmp/.fixed.out" 2>&1) || errs+=("fixed test.sh 실패: $(tail -3 "$tmp/.fixed.out")")
    (cd "$tmp" && bash hidden.sh >"$tmp/.hidden.out" 2>&1) || errs+=("fixed hidden.sh 실패: $(tail -3 "$tmp/.hidden.out")")
  fi
  if [ ${#errs[@]} -eq 0 ]; then echo "PASS $name"; pass=$((pass+1)); rm -rf "$tmp"
  else echo "FAIL $name"; printf '  - %s\n' "${errs[@]}"; echo "  (작업 폴더 보존: $tmp)"; fail=$((fail+1)); fi
done
echo "합계: pass=$pass fail=$fail"
[ "$fail" -eq 0 ]
