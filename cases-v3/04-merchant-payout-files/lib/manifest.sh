# manifest.sh — local record of what was handed to the transfer CLI.
# One line per file: date|partner|name|ticket|rows

manifest_file() { printf '%s/manifest.psv' "$OUT_DIR"; }

manifest_add() {
  ensure_dir "$OUT_DIR"
  printf '%s|%s|%s|%s|%s\n' "$1" "$2" "$3" "$4" "$5" >> "$(manifest_file)"
}

# A re-run of the same day appends new lines; keep only the latest line for
# each (date, partner, name).
manifest_compact() {
  local f tmp
  f=$(manifest_file)
  [ -f "$f" ] || return 0
  tmp="$f.tmp"
  awk -F'|' '{ k = $1 "|" $2 "|" $3; if (!(k in last)) order[++n] = k; last[k] = $0 }
             END { for (i = 1; i <= n; i++) print last[order[i]] }' "$f" > "$tmp" \
    && mv -f "$tmp" "$f"
}

# manifest_show DATE — the day's lines, partner order.
manifest_show() {
  local f
  f=$(manifest_file)
  [ -f "$f" ] || return 0
  awk -F'|' -v d="$1" '$1 == d' "$f" | sort -t'|' -k2,2 -k3,3
}
