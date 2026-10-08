# export.sh — CSV export for the accounting system (UTF-8, one row per record).

csv_field() {
  local s=$1
  case "$s" in
    *[\",$'\n']*) s=${s//\"/\"\"}; printf '"%s"' "$s" ;;
    *) printf '%s' "$s" ;;
  esac
}

# export_csv NORMALIZED_TSV — writes CSV to stdout.
export_csv() {
  local date seq code name amount memo
  printf '%s\n' "$EXPORT_HEADER"
  while IFS=$'\t' read -r date seq code name amount memo; do
    printf '%s%s%s%s%s%s%s%s%s%s%s\n' \
      "$(date_dash "$date")" "$EXPORT_DELIM" "$seq" "$EXPORT_DELIM" "$code" "$EXPORT_DELIM" \
      "$(csv_field "$name")" "$EXPORT_DELIM" "$amount" "$EXPORT_DELIM" "$(csv_field "$memo")"
  done < "$1"
}
