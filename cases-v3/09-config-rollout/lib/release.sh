# release.sh — release files: "@release <id>" plus "<key> = <value>" lines.

release_file() { printf '%s/%s.rel' "$RELEASE_DIR" "$1"; }

# release_id FILE
release_id() { awk '$1 == "@release" { print $2; exit }' "$1"; }

# release_changes FILE — key<TAB>value, in file order.
release_changes() {
  local line key val
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in ''|'#'*|'@'*) continue ;; esac
    key=$(trim "${line%%=*}")
    val=$(trim "${line#*=}")
    printf '%s\t%s\n' "$key" "$val"
  done < "$1"
}

# template_settings — setting names used by the template.
template_settings() {
  grep -o '{{[a-z_]*}}' "$TEMPLATE" | tr -d '{}' | sort -u
}

# release_check FILE — every key must be <scope>.<...>.<setting> with a known
# setting, pool and host.
release_check() {
  local key val setting scope mid bad=0 settings
  settings=" $(template_settings | tr '\n' ' ') "
  while IFS=$'\t' read -r key val; do
    scope=${key%%.*}
    setting=${key##*.}
    mid=${key#*.}; mid=${mid%.*}
    case " $settings " in *" $setting "*) ;; *) warn "unknown setting in $key"; bad=1; continue ;; esac
    case "$scope" in
      global) [ "$mid" = "$setting" ] || { warn "bad global key $key"; bad=1; } ;;
      pool)   known_pool "$mid" || { warn "unknown pool in $key"; bad=1; } ;;
      host)   known_host "$mid" || { warn "unknown host in $key"; bad=1; } ;;
      *)      warn "unknown scope in $key"; bad=1 ;;
    esac
    [ -n "$val" ] || { warn "empty value for $key"; bad=1; }
  done < <(release_changes "$1")
  return "$bad"
}
