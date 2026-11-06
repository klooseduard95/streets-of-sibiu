import base64
import math
import os
import re
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GAME = os.path.join(ROOT, "2026-Game_of_the_year-main", "newgame")
MODULES = os.path.join(GAME, "background", "modules")
OUT = os.path.join(ROOT, "logic", "src", "main", "kotlin", "ro", "klooseduard", "streetsofsibiu",
                   "logic", "data", "rooms", "GeneratedRooms.kt")

ROOMS = [
    "SpawnRoom",
    "FIGHTROOMBIGENEMY_1", "FIGHTROOMBIGENEMY_2", "FIGHTROOMBIGENEMY_3", "FIGHTROOMBIGENEMY_4",
    "FIGHTROOMBIGENEMY_1234", "FIGHTROOM_12", "FIGHTROOM_12_BIG", "FIGHTROOM_12_MEDIUM",
    "FIGHTROOM_13_BIG", "FIGHTROOM_13_MEDIUM", "FIGHTROOM_13_SMALL", "FIGHTROOM_24_MEDIUM",
    "FIGHTROOM_34_BIG", "FIGHTROOM_34_SMALL", "FIGHTROOM_1234_SMALL", "FIGHTROOM_234_SMALL",
    "FIGHTROOM_134_SMALL", "FIGHTROOM_124_SMALL", "FIGHTROOM_123_SMALL",
]


def f32(v):
    return struct.unpack("<f", struct.pack("<f", float(v)))[0]


def kfloat(v):
    s = "%.9g" % f32(v)
    if s == "-0":
        s = "0"
    return s + "f"


def parse_nodes(text):
    nodes = []
    current = None
    for line in text.splitlines():
        if line.startswith("[node "):
            attrs = dict(re.findall(r'(\w+)="([^"]*)"', line))
            current = (attrs, {})
            nodes.append(current)
        elif line.startswith("["):
            current = None
        elif current is not None and " = " in line:
            key, value = line.split(" = ", 1)
            current[1][key] = value
    return nodes


def sub_resources(text):
    out = {}
    current = None
    for line in text.splitlines():
        if line.startswith("[sub_resource "):
            current = re.search(r'id="([^"]*)"', line).group(1)
            out[current] = {"type": re.search(r'type="([^"]*)"', line).group(1)}
        elif line.startswith("["):
            current = None
        elif current is not None and " = " in line:
            key, value = line.split(" = ", 1)
            out[current][key] = value
    return out


def vec(value):
    m = re.fullmatch(r"Vector2i?\(([^,]+),\s*([^)]+)\)", value.strip())
    return (float(m.group(1)), float(m.group(2)))


def transform(position=(0.0, 0.0), rotation=0.0, scale=(1.0, 1.0)):
    c, s = math.cos(f32(rotation)), math.sin(f32(rotation))
    sx, sy = f32(scale[0]), f32(scale[1])
    return (c * sx, s * sx, -s * sy, c * sy, f32(position[0]), f32(position[1]))


def compose(a, b):
    axx, axy, ayx, ayy, aox, aoy = a
    bxx, bxy, byx, byy, box_, boy = b
    return (axx * bxx + ayx * bxy, axy * bxx + ayy * bxy,
            axx * byx + ayx * byy, axy * byx + ayy * byy,
            axx * box_ + ayx * boy + aox, axy * box_ + ayy * boy + aoy)


def apply(t, p):
    xx, xy, yx, yy, ox, oy = t
    return (xx * p[0] + yx * p[1] + ox, xy * p[0] + yy * p[1] + oy)


def rect_aabb(t, size):
    hw, hh = size[0] / 2, size[1] / 2
    pts = [apply(t, (sx * hw, sy * hh)) for sx in (-1, 1) for sy in (-1, 1)]
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    return (min(xs), min(ys), max(xs) - min(xs), max(ys) - min(ys))


def generation_node_geometry():
    text = open(os.path.join(GAME, "background", "GenerationNode.tscn"), encoding="utf-8").read()
    subs = sub_resources(text)
    nodes = {a["name"]: (a, p) for a, p in parse_nodes(text)}

    def shape_size(props):
        return vec(subs[re.search(r'SubResource\("([^"]*)"\)', props["shape"]).group(1)]["size"])

    tele = nodes["Teleporter"][1]
    tele_shape = nodes["TeleportArea"][1]
    prev = nodes["Preventer"][1]
    prev_shape = nodes["PreventerShape"][1]
    arrival = nodes["ArrivalNode"][1]
    return {
        "teleporter": (compose(transform(vec(tele["position"])),
                               transform(scale=vec(tele_shape.get("scale", "Vector2(1, 1)")))),
                       shape_size(tele_shape)),
        "preventer": (compose(transform(vec(prev["position"])),
                              transform(scale=vec(prev_shape.get("scale", "Vector2(1, 1)")))),
                      shape_size(prev_shape)),
        "arrival": vec(arrival["position"]),
    }


def decode_tiles(text):
    data = re.search(r'tile_map_data = PackedByteArray\("([^"]*)"\)', text).group(1)
    raw = base64.b64decode(data)
    assert struct.unpack_from("<H", raw, 0)[0] == 0, "unexpected TileMapLayer data format"
    cells = {}
    for off in range(2, len(raw) - 11, 12):
        x, y, source, ax, ay, alt = struct.unpack_from("<hhHHHH", raw, off)
        assert source == 0 and alt == 0 and 0 <= ax < 8 and 0 <= ay < 5
        cells[(x, y)] = (ax, ay)
    return cells


def convert(room, geo):
    text = open(os.path.join(MODULES, room + ".tscn"), encoding="utf-8").read()
    subs = sub_resources(text)
    cells = decode_tiles(text)
    xs = [x for x, _ in cells]
    ys = [y for _, y in cells]
    min_x, min_y = min(xs), min(ys)
    width, height = max(xs) - min_x + 1, max(ys) - min_y + 1
    rows = []
    for y in range(min_y, min_y + height):
        rows.append("".join("%d%d" % cells[(x, y)] if (x, y) in cells else ".." for x in range(min_x, min_x + width)))

    doors, spawns, module_box = [], [], None
    for attrs, props in parse_nodes(text):
        if attrs.get("parent") == "GenerationNodes":
            pos = vec(props["position"])
            rot = float(props.get("rotation", "0"))
            scl = vec(props.get("scale", "Vector2(1, 1)"))
            t = transform(pos, rot, scl)
            tele = rect_aabb(compose(t, geo["teleporter"][0]), geo["teleporter"][1])
            prev = rect_aabb(compose(t, geo["preventer"][0]), geo["preventer"][1])
            arr = apply(t, geo["arrival"])
            doors.append((attrs["name"], props["orientation"].strip('"').upper(), pos, rot, scl, tele, prev, arr))
        elif attrs.get("parent") == "ModuleBox":
            size = vec(subs[re.search(r'SubResource\("([^"]*)"\)', props["shape"]).group(1)]["size"])
            centre = vec(props["position"])
            module_box = (centre[0] - size[0] / 2, centre[1] - size[1] / 2, size[0], size[1])
        elif attrs.get("parent") == "." and attrs["name"].startswith("EnemySpawnNode"):
            radius = int(props.get("spawnRadius", "100"))
            spawns.append((attrs["name"], vec(props["position"]), radius))
    assert module_box is not None
    order = ",".join("%d,%d" % c for c in cells)
    return dict(id=room, order=order, min_x=min_x, min_y=min_y, width=width, height=height, rows=rows,
                doors=doors, module_box=module_box, spawns=spawns, cell_count=len(cells))


def kvec(p):
    return "Vec2(%s, %s)" % (kfloat(p[0]), kfloat(p[1]))


def krect(r):
    return "Rect(%s, %s, %s, %s)" % tuple(kfloat(v) for v in r)


def emit(rooms):
    out = []
    out.append("package ro.klooseduard.streetsofsibiu.logic.data.rooms")
    out.append("")
    out.append("import ro.klooseduard.streetsofsibiu.logic.math.Rect")
    out.append("import ro.klooseduard.streetsofsibiu.logic.math.Vec2")
    out.append("")
    out.append("internal object GeneratedRooms {")
    out.append("    val all: List<RoomDef> by lazy {")
    out.append("        listOf(%s)" % ", ".join(fn_name(r["id"]) + "()" for r in rooms))
    out.append("    }")
    for r in rooms:
        out.append("")
        out.append("    private fun %s() = RoomDef(" % fn_name(r["id"]))
        out.append('        id = "%s",' % r["id"])
        out.append("        tiles = TileGrid(")
        out.append("            minX = %d, minY = %d, width = %d, height = %d," % (r["min_x"], r["min_y"], r["width"], r["height"]))
        out.append("            rows = listOf(")
        for row in r["rows"]:
            out.append('                "%s",' % row)
        out.append("            ),")
        out.append('            order = "%s",' % r["order"])
        out.append("        ),")
        out.append("        doors = listOf(")
        for name, orient, pos, rot, scl, tele, prev, arr in r["doors"]:
            out.append("            DoorDef(")
            out.append('                name = "%s", orientation = DoorOrientation.%s,' % (name, orient))
            out.append("                transform = NodeTransform(%s, %s, %s)," % (kvec(pos), kfloat(rot), kvec(scl)))
            out.append("                teleporter = %s," % krect(tele))
            out.append("                preventer = %s," % krect(prev))
            out.append("                arrival = %s," % kvec(arr))
            out.append("            ),")
        out.append("        ),")
        out.append("        moduleBox = %s," % krect(r["module_box"]))
        out.append("        spawnNodes = listOf(")
        for name, pos, radius in r["spawns"]:
            out.append('            SpawnNodeDef("%s", %s, %d),' % (name, kvec(pos), radius))
        out.append("        ),")
        out.append("    )")
    out.append("}")
    return "\n".join(out) + "\n"


def fn_name(room_id):
    parts = room_id.lower().split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def main():
    geo = generation_node_geometry()
    rooms = [convert(r, geo) for r in ROOMS]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(emit(rooms))
    for r in rooms:
        print("%-24s cells %5d  grid %2dx%-2d origin (%d,%d)  doors %d  spawns %d"
              % (r["id"], r["cell_count"], r["width"], r["height"], r["min_x"], r["min_y"], len(r["doors"]), len(r["spawns"])))
    print("wrote", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    sys.exit(main())
