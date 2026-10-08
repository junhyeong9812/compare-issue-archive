# ledger.sh — aggregation and control totals.

# ledger_totals NORMALIZED_TSV — per partner "code<TAB>count<TAB>sum<TAB>feedname",
# sorted by code. The feed name of the first record is kept for partners
# missing from the master.
ledger_totals() {
  awk -F '\t' '
    { c[$3]++; s[$3] += $5; if (!($3 in nm)) nm[$3] = $4 }
    END { for (k in c) printf "%s\t%d\t%d\t%s\n", k, c[k], s[k], nm[k] }
  ' "$1" | LC_ALL=C sort -t "$(printf '\t')" -k1,1
}

# ledger_sum NORMALIZED_TSV — "count<TAB>total" over all records.
ledger_sum() {
  awk -F '\t' '{ n++; t += $5 } END { printf "%d\t%d\n", n, t }' "$1"
}

# ledger_check FEED NORMALIZED_TSV — compares the trailer with what we read.
ledger_check() {
  local want got
  want=$(feed_trailer "$1") || { warn "no trailer in $1"; return 2; }
  got=$(ledger_sum "$2")
  if [ "$want" != "$got" ]; then
    warn "control totals differ: trailer=${want//$'\t'/ / } read=${got//$'\t'/ / }"
    return 1
  fi
  return 0
}
