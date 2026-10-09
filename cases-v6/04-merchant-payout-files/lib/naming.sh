# naming.sh — remote file names for settlement files.
#
#   HBP_SETTLE_<partner>_<YYYYMMDD>.csv          one file for the day
#   HBP_SETTLE_<partner>_<YYYYMMDD>-<n>.csv      day split into several parts
#
# The transfer service parks (never delivers) names with four or more '_',
# so the part number must not add another '_' field.

# settle_name PARTNER DATE PART PARTS
settle_name() {
  local partner=$1 date=$2 part=$3 parts=$4
  local base="${FILE_PREFIX}_${partner}_${date}"
  if [ "$parts" -gt 1 ]; then
    base="${base}-${part}"
  fi
  printf '%s.%s' "$base" "$FILE_EXT"
}
