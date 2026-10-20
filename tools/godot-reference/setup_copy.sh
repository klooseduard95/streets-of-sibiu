#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
GODOT="$HERE/engine/Godot.app/Contents/MacOS/Godot"
ORIGINAL="$ROOT/2026-Game_of_the_year-main/newgame"
COPY="$HERE/project-copy"

[ -x "$GODOT" ] || { echo "Godot 4.6.1 not found at $GODOT (see README.md)"; exit 1; }
rm -rf "$COPY"
cp -R "$ORIGINAL" "$COPY"
mkdir -p "$COPY/probes"
shopt -s nullglob; for f in "$HERE"/probes/*.gd; do cp "$f" "$COPY/probes/"; done
"$GODOT" --headless --path "$COPY" --import 2>&1 | tail -n 20
echo "project copy ready: $COPY"
