"""Compares the port's room captures with Godot's reference renders pixel by pixel.

Usage: python3 -I tools/compare_room_screens.py <port_dir> [<godot_dir>]
  port_dir:  output of ./gradlew :desktop:run -Psos.captureRooms=<port_dir> (960x540 window = 1920x1080 back buffer)
  godot_dir: default tools/godot-reference/results/room_screens (probe v10_room_screens)
Exit code 0 only if every pair is identical.
"""
import os
import sys

from PIL import Image, ImageChops

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
port_dir = sys.argv[1]
godot_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "tools", "godot-reference", "results", "room_screens")
names = sorted(f for f in os.listdir(godot_dir) if f.endswith(".png"))
identical = 0
for name in names:
    g = Image.open(os.path.join(godot_dir, name)).convert("RGB")
    p = Image.open(os.path.join(port_dir, name)).convert("RGB")
    if g.size != p.size:
        print("%-36s SIZE MISMATCH godot %s port %s" % (name, g.size, p.size))
        continue
    diff = ImageChops.difference(g, p)
    bbox = diff.getbbox()
    if bbox is None:
        identical += 1
        print("%-36s identical" % name)
    else:
        changed = sum(1 for px in diff.getdata() if px != (0, 0, 0))
        maxd = max(m for _, m in diff.getextrema())
        print("%-36s DIFFERENT pixels %d (%.3f%%) max channel diff %d bbox %s" % (name, changed, 100.0 * changed / (g.width * g.height), maxd, bbox))
print("%d/%d identical" % (identical, len(names)))
sys.exit(0 if identical == len(names) else 1)
