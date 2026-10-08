# txns.sh — reads the acquiring extract (settle_date, partner, txn, amount, fee).

# txns_for DATE PARTNER — that partner's rows for DATE, ordered by txn id.
txns_for() {
  require_file "$TXN_FILE" "transaction extract"
  awk -F'\t' -v d="$1" -v p="$2" '
    /^#/ || NF < 5 { next }
    $1 == d && $2 == p { print $3 "\t" $4 "\t" $5 }
  ' "$TXN_FILE" | sort -t$'\t' -k1,1
}

# txns_count DATE PARTNER
txns_count() { txns_for "$1" "$2" | wc -l | tr -d ' '; }
