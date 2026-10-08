# money.sh — amounts in the feed are zero-padded whole won, 12 digits,
# with a trailing "-" in place of the last pad digit for refunds
# (e.g. "00000035000-" is -35000 on the older ERP build).

money_parse() {
  local s=$1 neg=0
  s=${s// /}
  if [ "${s: -1}" = "-" ]; then neg=1; s=${s%-}; fi
  case "$s" in
    ''|*[!0-9]*) return 1 ;;
  esac
  s=$((10#$s))
  if [ "$neg" -eq 1 ]; then s=$(( -s )); fi
  printf '%d' "$s"
}

# money_fmt 1250000 -> 1,250,000
money_fmt() {
  local n=$1 sign="" out=""
  if [ "${n:0:1}" = "-" ]; then sign="-"; n=${n#-}; fi
  while [ ${#n} -gt 3 ]; do
    out=",${n: -3}$out"
    n=${n:0:${#n}-3}
  done
  printf '%s%s%s' "$sign" "$n" "$out"
}
