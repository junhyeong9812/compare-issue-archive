# report.sh — the daily text report read by the settlement desk.

# report_render DATE TOTALS_TSV FEED_SOURCE
report_render() {
  local date=$1 totals=$2 source=$3 code cnt sum feedname name
  local all_cnt=0 all_sum=0 skipped=0 w=$REPORT_NAME_WIDTH
  printf '%s  %s  (source: %s)\n' "$REPORT_TITLE" "$(date_dash "$date")" "$source"
  printf '%s\n' "$(printf '%*s' $(( 8 + w + 22 )) '' | tr ' ' '=')"
  printf '%s%s%s%s\n' "$(text_pad "코드" 8)" "$(text_pad "거래처" "$w")" "$(text_lpad "건수" 6)" \
    "$(text_lpad "금액($REPORT_CURRENCY)" 16)"
  while IFS=$'\t' read -r code cnt sum feedname; do
    [ -n "$code" ] || continue
    if [ "${REPORT_SKIP_INACTIVE:-0}" = 1 ] && ! partner_active "$code"; then
      skipped=$((skipped + 1))
      continue
    fi
    name=$(partner_name "$code" "$feedname")
    printf '%-8s%s%6d%16s\n' "$code" "$(text_pad "$name" "$w")" "$cnt" "$(money_fmt "$sum")"
    all_cnt=$((all_cnt + cnt))
    all_sum=$((all_sum + sum))
  done < "$totals"
  printf '%s\n' "$(printf '%*s' $(( 8 + w + 22 )) '' | tr ' ' '-')"
  printf '%-8s%s%6d%16s\n' "" "$(text_pad "합계" "$w")" "$all_cnt" "$(money_fmt "$all_sum")"
  if [ "$skipped" -gt 0 ]; then
    printf '(inactive partners omitted: %d)\n' "$skipped"
  fi
}
