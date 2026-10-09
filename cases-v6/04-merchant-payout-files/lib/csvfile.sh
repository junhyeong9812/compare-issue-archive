# csvfile.sh — settlement file body (PAY-IF-03 rev.2).
#
#   H,<sender>,<partner>,<date>,<part>/<parts>
#   D,<txn>,<date>,<amount>,<fee>,<net>      one per transaction
#   T,<detail count>,<net total>
#
# Lines end in CRLF when LINE_END=crlf.

_eol() { [ "${LINE_END:-lf}" = crlf ] && printf '\r\n' || printf '\n'; }

# csv_render DATE PARTNER PART PARTS < rows(txn<TAB>amount<TAB>fee)
csv_render() {
  local date=$1 partner=$2 part=$3 parts=$4 txn amount fee net n=0 total=0
  printf 'H,%s,%s,%s,%d/%d' "$SENDER_ID" "$partner" "$date" "$part" "$parts"; _eol
  while IFS=$'\t' read -r txn amount fee; do
    [ -n "$txn" ] || continue
    net=$(money_net "$amount" "$fee")
    printf 'D,%s,%s,%s,%s,%s' "$txn" "$date" "$amount" "$fee" "$net"; _eol
    n=$((n + 1)); total=$((total + net))
  done
  printf 'T,%d,%d' "$n" "$total"; _eol
}
