#!/usr/bin/env bash
. "$(dirname "$0")/lib.sh"
t_setup
t_source_libs

assert_eq "plain amount"            "$(money_parse 000001250000)" 1250000
assert_eq "zero"                    "$(money_parse 000000000000)" 0
assert_eq "leading zeros not octal" "$(money_parse 000000000089)" 89
assert_eq "refund suffix"           "$(money_parse 00000035000-)" -35000
money_parse 00000abc0000 >/dev/null; assert_rc "garbage rejected" $? 1
assert_eq "format millions"         "$(money_fmt 1250000)" "1,250,000"
assert_eq "format small"            "$(money_fmt 990)" "990"
assert_eq "format negative"         "$(money_fmt -35000)" "-35,000"
assert_eq "format exact thousands"  "$(money_fmt 100000)" "100,000"
t_done
