#!/usr/bin/env bash
set -euo pipefail
JDK_BIN="$HOME/.sdkman/candidates/java/21.0.5-tem/bin"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
"$JDK_BIN/javac" -d "$out" $(find src -name '*.java') HiddenAccountCheck.java
"$JDK_BIN/java" -cp "$out" HiddenAccountCheck
"$JDK_BIN/java" -Dpassword.reuseWindow=3 -cp "$out" HiddenAccountCheck
"$JDK_BIN/java" -Dpassword.reuseWindow=1 -cp "$out" HiddenAccountCheck
