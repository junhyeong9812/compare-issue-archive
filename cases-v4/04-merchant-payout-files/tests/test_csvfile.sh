#!/usr/bin/env bash
. "$(dirname "$0")/lib.sh"
t_setup
t_source_libs

body=$(printf 'M001\t10000\t250\nM002\t5500\t137\n' | csv_render 20261005 P002 1 1)
assert_contains "header names sender, partner, day and part" "$body" "H,HBP,P002,20261005,1/1"
assert_contains "detail carries net amount" "$body" "D,M001,20261005,10000,250,9750"
assert_contains "trailer counts and totals" "$body" "T,2,15113"
assert_eq "lines end in CRLF" "$(printf '%s\n' "$body" | head -1 | tr -d '\n' | tail -c 1 | od -An -c | tr -d ' ')" '\r'
assert_eq "one file for a normal day" "$(settle_name P002 20261005 1 1)" "HBP_SETTLE_P002_20261005.csv"
t_done
