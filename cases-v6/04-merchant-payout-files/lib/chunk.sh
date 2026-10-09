# chunk.sh — splits a row file into parts of at most N rows.

# chunk_rows FILE MAX PREFIX — writes PREFIX.01, PREFIX.02, ... and prints the
# number of parts. An empty FILE still yields one (empty) part so the partner
# receives a file for every settlement day.
chunk_rows() {
  local file=$1 max=$2 prefix=$3 rows parts
  rows=$(wc -l < "$file" | tr -d ' ')
  if [ "$rows" -le "$max" ]; then
    cp -f "$file" "$prefix.01" || return 1
    printf '1'
    return 0
  fi
  split -l "$max" -d -a 2 --numeric-suffixes=1 "$file" "$prefix." || return 1
  parts=$(( (rows + max - 1) / max ))
  printf '%d' "$parts"
}
