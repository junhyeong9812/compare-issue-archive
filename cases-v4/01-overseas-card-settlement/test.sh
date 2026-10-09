#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
JDK_BIN="$HOME/.sdkman/candidates/java/21.0.5-tem/bin"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
"$JDK_BIN/javac" -encoding UTF-8 -d "$out" $(find src test -name '*.java')
"$JDK_BIN/java" -cp "$out" UnitChecks
"$JDK_BIN/java" -cp "$out" SettlementE2ETest
