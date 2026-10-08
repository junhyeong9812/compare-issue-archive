#!/usr/bin/env bash
set -euo pipefail
JDK_BIN="$HOME/.sdkman/candidates/java/21.0.5-tem/bin"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
"$JDK_BIN/javac" -d "$out" src/*.java test/*.java
"$JDK_BIN/java" -cp "$out" SalesReportTest
