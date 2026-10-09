# text.sh — small string helpers.

# trim STRING — strip leading/trailing blanks
trim() {
  local s=$1
  s=${s#"${s%%[![:space:]]*}"}
  s=${s%"${s##*[![:space:]]}"}
  printf '%s' "$s"
}

# sed_repl_escape STRING — make STRING safe as the replacement part of
# s|pattern|replacement| (escapes backslash, '&' and the '|' delimiter).
sed_repl_escape() {
  printf '%s' "$1" | sed -e 's/[\\&|]/\\&/g'
}

# key_file KEY — a file-name-safe form of a config key
key_file() { printf '%s' "$1" | tr -c 'a-zA-Z0-9._-' '_'; }
