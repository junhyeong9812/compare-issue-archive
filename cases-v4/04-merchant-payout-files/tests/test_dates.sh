#!/usr/bin/env bash
. "$(dirname "$0")/lib.sh"
t_setup
t_source_libs

assert_eq "dash form"             "$(date_dash 20261005)" "2026-10-05"
assert_eq "compact form"          "$(date_compact 2026-10-05)" "20261005"
assert_eq "next day"              "$(date_next 20261031)" "20261101"
assert_eq "leap day"              "$(date_next 20280228)" "20280229"
assert_eq "range across month end" "$(date_range 20260929 20261002 | tr '\n' ' ')" "20260929 20260930 20261001 20261002 "
assert_eq "single day range"      "$(date_range 20261005 20261005)" "20261005"
assert_eq "net amount"            "$(money_net 10000 250)" 9750
assert_eq "format millions"       "$(money_fmt 6257753)" "6,257,753"
assert_eq "format small"          "$(money_fmt 990)" "990"
t_done
