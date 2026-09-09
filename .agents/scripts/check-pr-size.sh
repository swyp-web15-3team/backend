#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: bash $0 <actual-base-ref> [candidate-head-ref]" >&2
  exit 2
fi

base=$(git merge-base "$1" "${2:-HEAD}")
git diff --no-ext-diff --no-textconv --no-renames --numstat "$base" "${2:-HEAD}" -- |
  awk -F '\t' '
    $1 == "-" || $2 == "-" { binary = 1; next }
    { total += $1 + $2 }
    END {
      if (binary) {
        print "FAIL: binary changes require an explicit review policy." > "/dev/stderr"
        exit 2
      }
      printf "%s: %d/400 changed lines (added + deleted).\n", (total <= 400 ? "PASS" : "FAIL"), total
      exit (total > 400)
    }
  '
