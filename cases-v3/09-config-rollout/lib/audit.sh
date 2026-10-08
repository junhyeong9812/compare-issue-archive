# audit.sh — rollout history: one line per finished rollout.
# release|mark|hosts|changes

history_file() { printf '%s/history.log' "$OUT_DIR"; }

history_add() {
  ensure_dir "$OUT_DIR"
  printf '%s|%s|%s|%s\n' "$1" "$2" "$3" "$4" >> "$(history_file)"
}

history_show() {
  local f
  f=$(history_file)
  [ -f "$f" ] || return 0
  awk -F'|' '{ printf "%-14s %-9s %3s hosts %3s changes\n", $1, $2, $3, $4 }' "$f"
}
