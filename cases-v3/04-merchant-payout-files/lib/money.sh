# money.sh — integer KRW arithmetic and formatting.

# money_net AMOUNT FEE -> amount minus fee
money_net() { printf '%d' $(( 10#$1 - 10#$2 )); }

# money_fmt N -> "1,234,567"
money_fmt() {
  local n=$1 sign="" out=""
  case "$n" in -*) sign="-"; n=${n#-} ;; esac
  while [ ${#n} -gt 3 ]; do
    out=",${n: -3}$out"
    n=${n:0:${#n}-3}
  done
  printf '%s%s%s' "$sign" "$n" "$out"
}
