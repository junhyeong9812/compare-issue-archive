# xfer.sh — thin wrapper around the transfer vendor CLI.

_xferq() { "$XFERQ" "$@"; }

# xfer_send PARTNER FILE NAME — uploads FILE as NAME and prints the ticket.
# Transient spool errors are retried; a malformed answer counts as a failure.
xfer_send() {
  local partner=$1 file=$2 name=$3 out rc try=0
  while :; do
    out=$(_xferq send --to "$partner" --file "$file" --as "$name" 2>&1); rc=$?
    if [ "$rc" -eq 0 ]; then
      case "$out" in
        T[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]) printf '%s' "$out"; return 0 ;;
        *) warn "unexpected answer from transfer CLI: $out"; return 1 ;;
      esac
    fi
    try=$((try + 1))
    if [ "$try" -gt "$SEND_RETRIES" ]; then
      warn "send $name to $partner failed ($out, exit $rc)"
      return 1
    fi
    log "retrying $name ($out)"
  done
}

# xfer_ls PARTNER — name<TAB>bytes<TAB>ticket for files in the partner's box.
xfer_ls() { _xferq ls --to "$1"; }

# xfer_pull PARTNER NAME — file contents on stdout.
xfer_pull() { _xferq pull --to "$1" --name "$2"; }
