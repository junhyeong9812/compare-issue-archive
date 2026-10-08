# inventory.sh — host list and pools.

# active_hosts — host<TAB>pool for every host that gets a config.
active_hosts() {
  require_file "$HOSTS_FILE" "host list"
  awk -F'|' '!/^#/ && NF >= 4 && $4 != "D" { print $1 "\t" $2 }' "$HOSTS_FILE"
}

host_pool() {
  awk -F'|' -v h="$1" '!/^#/ && $1 == h { print $2; exit }' "$HOSTS_FILE"
}

known_host() { [ -n "$(host_pool "$1")" ]; }

known_pool() {
  awk -F'|' -v p="$1" '!/^#/ && $2 == p { f = 1 } END { exit !f }' "$HOSTS_FILE"
}
