# receipts.sh — what the partner can actually download, per settlement day.
# Reads the partner's box through the transfer CLI and totals the detail rows.

# receipts_rows PARTNER — date<TAB>net for every detail row in the partner's box.
receipts_rows() {
  local partner=$1 name bytes ticket
  xfer_ls "$partner" | while IFS=$'\t' read -r name bytes ticket; do
    [ -n "$name" ] || continue
    xfer_pull "$partner" "$name" | tr -d '\r' | awk -F, '$1 == "D" { print $3 "\t" $6 }'
  done
}

# receipts_report PARTNER FROM TO
receipts_report() {
  local partner=$1 from=$2 to=$3 rows d cnt net
  rows=$(receipts_rows "$partner")
  printf '# partner %s  %s..%s\n' "$partner" "$(date_dash "$from")" "$(date_dash "$to")"
  for d in $(date_range "$from" "$to"); do
    cnt=$(printf '%s\n' "$rows" | awk -F'\t' -v d="$d" '$1 == d { n++ } END { print n + 0 }')
    net=$(printf '%s\n' "$rows" | awk -F'\t' -v d="$d" '$1 == d { s += $2 } END { print s + 0 }')
    if [ "$cnt" -eq 0 ]; then
      printf '%s  %4s  %15s\n' "$(date_dash "$d")" "-" "-"
    else
      printf '%s  %4d  %15s\n' "$(date_dash "$d")" "$cnt" "$(money_fmt "$net")"
    fi
  done
}
