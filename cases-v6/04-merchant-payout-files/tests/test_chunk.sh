#!/usr/bin/env bash
. "$(dirname "$0")/lib.sh"
t_setup
t_source_libs

seq 1 45 > "$T_WORK/rows"
assert_eq "45 rows at 20 per file -> 3 parts" "$(chunk_rows "$T_WORK/rows" 20 "$T_WORK/a")" 3
assert_eq "first part full"  "$(wc -l < "$T_WORK/a.01" | tr -d ' ')" 20
assert_eq "second part full" "$(wc -l < "$T_WORK/a.02" | tr -d ' ')" 20
assert_eq "last part rest"   "$(wc -l < "$T_WORK/a.03" | tr -d ' ')" 5
assert_eq "no row lost"      "$(cat "$T_WORK"/a.0? | tail -1)" 45

seq 1 20 > "$T_WORK/rows20"
assert_eq "exactly at the limit -> 1 part" "$(chunk_rows "$T_WORK/rows20" 20 "$T_WORK/b")" 1

: > "$T_WORK/empty"
assert_eq "empty day still yields a part" "$(chunk_rows "$T_WORK/empty" 20 "$T_WORK/c")" 1
assert_eq "empty part is empty" "$(wc -c < "$T_WORK/c.01" | tr -d ' ')" 0
t_done
