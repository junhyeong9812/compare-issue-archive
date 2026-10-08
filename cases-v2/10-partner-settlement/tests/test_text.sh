#!/usr/bin/env bash
. "$(dirname "$0")/lib.sh"
t_setup
t_source_libs

assert_eq "rtrim"                "$(text_rtrim 'abc   ')" "abc"
assert_eq "trim both"            "$(text_trim '  a b  ')" "a b"
assert_eq "width ascii"          "$(text_width 'ACME')" 4
assert_eq "width hangul"         "$(text_width '한빛상사')" 8
assert_eq "width mixed"          "$(text_width 'S&T물류')" 7
assert_eq "fit keeps short"      "$(text_fit '대성유통' 10)" "대성유통"
assert_eq "fit cuts on char"     "$(text_fit '미래푸드시스템즈' 9)" "미래푸드~"
assert_eq "pad to columns"       "$(text_pad '한빛' 6)|" "한빛  |"
assert_eq "lpad right aligns"    "$(text_lpad '건수' 6)" "  건수"
assert_eq "cr stripped"          "$(charset_strip_cr $'P1\r')" "P1"
t_done
