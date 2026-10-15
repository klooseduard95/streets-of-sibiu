#!/usr/bin/env bash
# Runs one probe against the project copy and stores its JSON in results/.
# Usage: tools/godot-reference/run_probe.sh v1_runtime [--windowed]
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
GODOT="$HERE/engine/Godot.app/Contents/MacOS/Godot"
NAME="$1"; shift || true
cp "$HERE"/probes/*.gd "$HERE/project-copy/probes/"
MODE=(--headless)
[ "${1:-}" = "--windowed" ] && MODE=(--windowed --resolution 1920x1080)
"$GODOT" "${MODE[@]}" --path "$HERE/project-copy" --script "res://probes/$NAME.gd" -- --out="$HERE/results" \
  > "$HERE/results/$NAME.log" 2>&1 || true
grep -E "PROBE|SCRIPT ERROR|ERROR" "$HERE/results/$NAME.log" | head -n 40 || true
