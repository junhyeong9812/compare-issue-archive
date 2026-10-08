# feed.sh — reader for the partner ERP fixed-width feed (see conf/layout.conf).
#
# feed_records FILE
#   Prints one TAB-separated line per detail record, already in UTF-8:
#     date  seq  code  name  amount  memo
#   Header/trailer records are skipped; use feed_trailer for the trailer.

# layout_slice LINE OFFSET:LENGTH — byte slice (caller runs under LC_ALL=C).
layout_slice() {
  local spec=$2
  printf '%s' "${1:${spec%%:*}:${spec##*:}}"
}

feed_records() (
  export LC_ALL=C
  local file=$1 line type n=0 seq code name amount memo date
  while IFS= read -r line || [ -n "$line" ]; do
    n=$((n + 1))
    line=$(charset_strip_cr "$line")
    type=$(layout_slice "$line" "$REC_TYPE")
    [ "$type" = D ] || continue
    if [ "${#line}" -ne 64 ]; then
      warn "line $n: record length ${#line}, expected 64 — skipped"
      continue
    fi
    date=$(layout_slice "$line" "$REC_DATE")
    seq=$(layout_slice "$line" "$D_SEQ")
    code=$(text_trim "$(layout_slice "$line" "$D_CODE")")
    name=$(text_rtrim "$(layout_slice "$line" "$D_NAME")")
    amount=$(money_parse "$(layout_slice "$line" "$D_AMOUNT")") || {
      warn "line $n: bad amount — skipped"; continue; }
    memo=$(text_rtrim "$(layout_slice "$line" "$D_MEMO")")
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$date" "$((10#$seq))" "$code" \
      "$(charset_to_utf8 "$name")" "$amount" "$(charset_to_utf8 "$memo")"
  done < "$file"
)

# feed_header FILE — prints "date<TAB>source" from the H record.
feed_header() (
  export LC_ALL=C
  local line
  IFS= read -r line < "$1" || exit 1
  line=$(charset_strip_cr "$line")
  [ "$(layout_slice "$line" "$REC_TYPE")" = H ] || exit 1
  printf '%s\t%s\n' "$(layout_slice "$line" "$REC_DATE")" \
    "$(text_rtrim "$(layout_slice "$line" "$H_SOURCE")")"
)

# feed_trailer FILE — prints "count<TAB>total" from the T record.
feed_trailer() (
  export LC_ALL=C
  local line last=""
  while IFS= read -r line || [ -n "$line" ]; do
    line=$(charset_strip_cr "$line")
    [ "$(layout_slice "$line" "$REC_TYPE")" = T ] && last=$line
  done < "$1"
  [ -n "$last" ] || exit 1
  printf '%s\t%s\n' "$((10#$(layout_slice "$last" "$T_COUNT")))" \
    "$((10#$(layout_slice "$last" "$T_TOTAL")))"
)
