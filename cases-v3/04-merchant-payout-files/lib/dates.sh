# dates.sh — business date helpers (GNU date).

# "20261005" -> "2026-10-05"
date_dash() { printf '%s-%s-%s' "${1:0:4}" "${1:4:2}" "${1:6:2}"; }

# "2026-10-05" or "20261005" -> "20261005"
date_compact() { local d=${1//-/}; printf '%s' "$d"; }

# date_next YYYYMMDD -> the following calendar day
date_next() { date -u -d "$(date_dash "$1") +1 day" +%Y%m%d; }

# date_range FROM TO — every calendar day from FROM to TO inclusive.
# Partners settle seven days a week, so weekends are included.
date_range() {
  local d=$1 end=$2 guard=0
  while [ "$d" -le "$end" ]; do
    printf '%s\n' "$d"
    d=$(date_next "$d")
    guard=$((guard + 1))
    [ "$guard" -le 400 ] || { warn "date range too long: $1..$2"; return 1; }
  done
}
