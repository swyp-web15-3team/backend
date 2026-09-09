#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: bash $0 <actual-base-ref> <candidate-head-ref> <create|modify|delete>" >&2
  exit 2
fi

case "$3" in
  create|modify|delete) ;;
  *) echo "Invalid change type: use create, modify, or delete." >&2; exit 2 ;;
esac

base=$(git merge-base "$1" "$2")
if [[ "$3" != modify ]]; then
  echo "PASS: feature $3 has no PR line limit."
  exit 0
fi
git diff --no-ext-diff --no-textconv --no-renames --numstat "$base" "$2" -- |
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
