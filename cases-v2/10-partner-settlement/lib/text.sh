# text.sh — trimming and column layout for the text report.
# Hangul and other non-ASCII characters take two terminal columns.

text_rtrim() {
  local s=$1
  printf '%s' "${s%"${s##*[![:space:]]}"}"
}

text_ltrim() {
  local s=$1
  printf '%s' "${s#"${s%%[![:space:]]*}"}"
}

text_trim() { text_ltrim "$(text_rtrim "$1")"; }

# Number of terminal columns TEXT occupies.
text_width() {
  local s=$1 ascii
  ascii=${s//[^[:ascii:]]/}
  printf '%d' $(( ${#ascii} + 2 * (${#s} - ${#ascii}) ))
}

# text_fit TEXT COLS — cut TEXT so that it fits in COLS columns, marking
# the cut with "~".
text_fit() {
  local s=$1 cols=$2 out="" ch w=0 cw i
  if [ "$(text_width "$s")" -le "$cols" ]; then printf '%s' "$s"; return; fi
  for (( i = 0; i < ${#s}; i++ )); do
    ch=${s:i:1}
    if [[ $ch == [[:ascii:]] ]]; then cw=1; else cw=2; fi
    (( w + cw > cols - 1 )) && break
    out+=$ch; w=$(( w + cw ))
  done
  printf '%s~' "$out"
}

# text_pad TEXT COLS — left-aligned, padded with spaces to COLS columns.
text_pad() {
  local s w
  s=$(text_fit "$1" "$2")
  w=$(text_width "$s")
  printf '%s%*s' "$s" $(( $2 - w )) ''
}

# text_lpad TEXT COLS — right-aligned.
text_lpad() {
  local w
  w=$(text_width "$1")
  printf '%*s%s' $(( $2 > w ? $2 - w : 0 )) '' "$1"
}
