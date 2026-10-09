# receipts.sh — what the partner can actually download, per settlement day.
# Reads the partner's box through the transfer CLI and totals the detail rows.

# receipts_rows PARTNER — one line per header and per detail row of every file
# in the partner's box:
#   P<TAB>date<TAB>part<TAB>parts
#   D<TAB>date<TAB>net
receipts_rows() {
  local partner=$1 name bytes ticket
  xfer_ls "$partner" | while IFS=$'\t' read -r name bytes ticket; do
    [ -n "$name" ] || continue
    xfer_pull "$partner" "$name" | tr -d '\r' | awk -F, '
      $1 == "H" { split($5, pp, "/"); print "P\t" $4 "\t" pp[1] "\t" pp[2] }
      $1 == "D" { print "D\t" $3 "\t" $6 }'
  done
}

# receipts_day ROWS DATE -> "<count> <net>" for the day.
# A split day is totalled only once parts 1..n are all in the box; until then
# the answer is "0 0", the same as a day with no file at all.
# TODO: 일부 파트만 온 날도 온 만큼은 세어 주면 되지 않나? 지금은 '-'라 헷갈린다.
receipts_day() {
  printf '%s\n' "$1" | awk -F'\t' -v d="$2" '
    $1 == "P" && $2 == d { seen[$3 + 0] = 1; want = $4 + 0 }
    $1 == "D" && $2 == d { n++; s += $3 }
    END {
      have = 0
      for (p in seen) if (p >= 1 && p <= want) have++
      if (want == 0 || have < want) { print "0 0"; exit }
      print n + 0, s + 0
    }'
}

# receipts_report PARTNER FROM TO
receipts_report() {
  local partner=$1 from=$2 to=$3 rows d cnt net
  rows=$(receipts_rows "$partner")
  printf '# partner %s  %s..%s\n' "$partner" "$(date_dash "$from")" "$(date_dash "$to")"
  for d in $(date_range "$from" "$to"); do
    read -r cnt net <<< "$(receipts_day "$rows" "$d")"
    if [ "$cnt" -eq 0 ]; then
      printf '%s  %4s  %15s\n' "$(date_dash "$d")" "-" "-"
    else
      printf '%s  %4d  %15s\n' "$(date_dash "$d")" "$cnt" "$(money_fmt "$net")"
    fi
  done
}
