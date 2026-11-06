package ro.klooseduard.streetsofsibiu.logic.parity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.rooms.AtlasCoord
import ro.klooseduard.streetsofsibiu.logic.data.rooms.DoorOrientation
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomTileSet
import ro.klooseduard.streetsofsibiu.logic.math.Rect
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.cos
import kotlin.math.sin

class RoomParityTest {
    private fun scene(id: String) = GodotText.load("background/modules/$id.tscn")

    private fun decodeTiles(scene: GodotText): Map<Pair<Int, Int>, AtlasCoord> {
        val layer = scene.node("TileMapLayer")
        val b64 = Regex("""PackedByteArray\("([^"]*)"\)""").find(layer.props.getValue("tile_map_data"))!!.groupValues[1]
        val buf = ByteBuffer.wrap(Base64.getDecoder().decode(b64)).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("TileMapLayer data format", 0, buf.short.toInt())
        val cells = LinkedHashMap<Pair<Int, Int>, AtlasCoord>()
        while (buf.remaining() >= 12) {
            val x = buf.short.toInt()
            val y = buf.short.toInt()
            val source = buf.short.toInt() and 0xFFFF
            val ax = buf.short.toInt() and 0xFFFF
            val ay = buf.short.toInt() and 0xFFFF
            val alt = buf.short.toInt() and 0xFFFF
            assertEquals(0, source)
            assertEquals(0, alt)
            cells[x to y] = AtlasCoord(ax, ay)
        }
        return cells
    }

    @Test
    fun catalogContainsExactlyTheRoomsTheGameLoads() {
        val referenced = Regex("""load\("res://background/modules/(\w+)\.tscn"\)""")
            .findAll(Original.text("GlobalUtils.gd")).map { it.groupValues[1] }.toSet() + "SpawnRoom"
        assertTrue(Original.text("background/main_background.gd").contains("res://background/modules/SpawnRoom.tscn"))
        assertEquals(referenced, RoomCatalog.all.map { it.id }.toSet())
        assertEquals(20, RoomCatalog.all.size)
    }

    @Test
    fun everyTileMatchesTheOriginal() {
        var total = 0
        for (room in RoomCatalog.all) {
            val cells = decodeTiles(scene(room.id))
            assertEquals("${room.id} cell count", cells.size, room.tiles.cells.size)
            for ((cell, atlas) in cells) assertEquals("${room.id} $cell", atlas, room.tiles.atlasAt(cell.first, cell.second))
            for ((cell, _) in room.tiles.cells) assertTrue("${room.id} extra $cell", cell in cells)
            assertNull(room.tiles.atlasAt(room.tiles.minX - 1, room.tiles.minY))
            total += cells.size
        }
        assertEquals(10565, total)
    }

    @Test
    fun knownCellCounts() {
        val counts = RoomCatalog.all.associate { it.id to it.tiles.cells.size }
        assertEquals(220, counts["SpawnRoom"])
        assertEquals(1813, counts["FIGHTROOM_12_BIG"])
        assertEquals(1764, counts["FIGHTROOM_13_BIG"])
        assertEquals(913, counts["FIGHTROOM_34_BIG"])
        assertEquals(630, counts["FIGHTROOM_12_MEDIUM"])
        assertEquals(441, counts["FIGHTROOMBIGENEMY_1"])
    }

    @Test
    fun everyRoomTileSetMatchesTheSharedSemantics() {
        for (room in RoomCatalog.all) {
            val scene = scene(room.id)
            val atlas = scene.subResources.values.single { it.attrs["type"] == "TileSetAtlasSource" }
            assertEquals(RoomTileSet.TEXTURE, scene.extPath(atlas.props.getValue("texture")))
            assertEquals("Vector2i(100, 100)", atlas.props["texture_region_size"])
            val defined = atlas.props.keys.mapNotNull { Regex("""^(\d+):(\d+)/0$""").find(it) }
                .map { AtlasCoord(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }.toSet()
            assertEquals(room.id, 40, defined.size)
            for (c in defined) {
                val physics = atlas.props["${c.x}:${c.y}/0/physics_layer_0/polygon_0/points"]
                val nav = atlas.props.containsKey("${c.x}:${c.y}/0/navigation_layer_0/polygon")
                assertEquals("${room.id} $c solid", RoomTileSet.isSolid(c), physics != null)
                assertEquals("${room.id} $c walkable", RoomTileSet.isWalkable(c), nav)
                if (physics != null) assertEquals("PackedVector2Array(-50, -50, 50, -50, 50, 50, -50, 50)", physics)
            }
            val tileSet = scene.subResources.values.single { it.attrs["type"] == "TileSet" }
            assertEquals("Vector2i(100, 100)", tileSet.props["tile_size"])
            assertEquals("1", tileSet.props["physics_layer_0/collision_layer"])
            assertEquals("2", tileSet.props["navigation_layer_0/layers"])
            scene.subResources.values.filter { it.attrs["type"] == "NavigationPolygon" }.forEach {
                assertEquals("PackedVector2Array(50, 50, -50, 50, -50, -50, 50, -50)", it.props["vertices"])
                assertEquals("0.0", it.props["agent_radius"])
            }
        }
    }

    @Test
    fun roomScenesContainOnlyTheExpectedNodes() {
        for (room in RoomCatalog.all) {
            for (node in scene(room.id).nodes) {
                val name = node.attrs.getValue("name")
                val parent = node.attrs["parent"]
                val ok = when {
                    parent == null -> node.attrs["type"] == "Node2D"
                    name == "TileMapLayer" -> node.props.keys == setOf("tile_map_data", "tile_set")
                    name == "GenerationNodes" || name == "ModuleBox" -> node.props.isEmpty()
                    parent == "GenerationNodes" -> name.matches(Regex("GN\\d"))
                    parent == "ModuleBox" -> name == "Shape"
                    else -> parent == "." && name.startsWith("EnemySpawnNode")
                }
                assertTrue("${room.id}: unexpected node $name under $parent", ok)
            }
        }
    }

    private class Xf(val xx: Double, val xy: Double, val yx: Double, val yy: Double, val ox: Double, val oy: Double) {
        operator fun times(b: Xf) = Xf(
            xx * b.xx + yx * b.xy, xy * b.xx + yy * b.xy,
            xx * b.yx + yx * b.yy, xy * b.yx + yy * b.yy,
            xx * b.ox + yx * b.oy + ox, xy * b.ox + yy * b.oy + oy,
        )
        fun apply(px: Double, py: Double) = (xx * px + yx * py + ox) to (xy * px + yy * py + oy)

        companion object {
            fun of(pos: Pair<Double, Double> = 0.0 to 0.0, rot: Double = 0.0, scale: Pair<Double, Double> = 1.0 to 1.0): Xf {
                val r = rot.toFloat().toDouble()
                return Xf(cos(r) * scale.first, sin(r) * scale.first, -sin(r) * scale.second, cos(r) * scale.second, pos.first, pos.second)
            }
        }
    }

    private fun aabb(xf: Xf, w: Double, h: Double): DoubleArray {
        val pts = listOf(-1, 1).flatMap { sx -> listOf(-1, 1).map { sy -> xf.apply(sx * w / 2, sy * h / 2) } }
        val minX = pts.minOf { it.first }
        val minY = pts.minOf { it.second }
        return doubleArrayOf(minX, minY, pts.maxOf { it.first } - minX, pts.maxOf { it.second } - minY)
    }

    private fun assertRect(msg: String, expected: DoubleArray, actual: Rect) {
        val got = doubleArrayOf(actual.x.toDouble(), actual.y.toDouble(), actual.width.toDouble(), actual.height.toDouble())
        for (i in 0..3) assertEquals("$msg[$i]", expected[i], got[i], 1e-3)
    }

    @Test
    fun doorsMatchOrientationTransformAndGeometry() {
        val gn = GodotText.load("background/GenerationNode.tscn")
        fun scaleOf(s: GodotText.Section) = s.props["scale"]?.let(GodotText::vec2) ?: (1.0 to 1.0)
        val teleShape = gn.node("TeleportArea")
        val teleLocal = Xf.of(GodotText.vec2(gn.node("Teleporter").props.getValue("position"))) * Xf.of(scale = scaleOf(teleShape))
        val teleSize = GodotText.vec2(gn.sub(teleShape.props.getValue("shape")).props.getValue("size"))
        val prevShape = gn.node("PreventerShape")
        val prevLocal = Xf.of(GodotText.vec2(gn.node("Preventer").props.getValue("position"))) * Xf.of(scale = scaleOf(prevShape))
        val prevSize = GodotText.vec2(gn.sub(prevShape.props.getValue("shape")).props.getValue("size"))
        val arrival = GodotText.vec2(gn.node("ArrivalNode").props.getValue("position"))
        var doors = 0
        for (room in RoomCatalog.all) {
            val originals = scene(room.id).nodes.filter { it.attrs["parent"] == "GenerationNodes" }
            assertEquals(room.id, originals.map { it.attrs["name"] }, room.doors.map { it.name })
            for ((orig, door) in originals.zip(room.doors)) {
                val msg = "${room.id}/${door.name}"
                assertEquals(msg, DoorOrientation.valueOf(GodotText.string(orig.props.getValue("orientation")).uppercase()), door.orientation)
                val pos = GodotText.vec2(orig.props.getValue("position"))
                val rot = orig.props["rotation"]?.toDouble() ?: 0.0
                val scale = scaleOf(orig)
                assertEquals(msg, pos.first.toFloat(), door.transform.position.x)
                assertEquals(msg, pos.second.toFloat(), door.transform.position.y)
                assertEquals(msg, rot.toFloat(), door.transform.rotation)
                assertEquals(msg, scale.first.toFloat(), door.transform.scale.x)
                assertEquals(msg, scale.second.toFloat(), door.transform.scale.y)
                val xf = Xf.of(pos, rot, scale)
                assertRect("$msg teleporter", aabb(xf * teleLocal, teleSize.first, teleSize.second), door.teleporter)
                assertRect("$msg preventer", aabb(xf * prevLocal, prevSize.first, prevSize.second), door.preventer)
                val arr = xf.apply(arrival.first, arrival.second)
                assertEquals("$msg arrival x", arr.first, door.arrival.x.toDouble(), 1e-3)
                assertEquals("$msg arrival y", arr.second, door.arrival.y.toDouble(), 1e-3)
                assertTrue("$msg arrival inside box", room.moduleBox.contains(door.arrival))
                doors++
            }
        }
        assertEquals(46, doors)
    }

    @Test
    fun moduleBoxesMatchTheOriginal() {
        for (room in RoomCatalog.all) {
            val scene = scene(room.id)
            val shape = scene.node("Shape")
            val centre = GodotText.vec2(shape.props.getValue("position"))
            val size = GodotText.vec2(scene.sub(shape.props.getValue("shape")).props.getValue("size"))
            assertRect(room.id, doubleArrayOf(centre.first - size.first / 2, centre.second - size.second / 2, size.first, size.second), room.moduleBox)
            val t = room.tiles
            assertTrue(room.id, room.moduleBox.x >= t.minX * 100f && room.moduleBox.right <= (t.minX + t.width) * 100f)
            assertTrue(room.id, room.moduleBox.y >= t.minY * 100f && room.moduleBox.bottom <= (t.minY + t.height) * 100f)
        }
    }

    @Test
    fun spawnNodesMatchTheOriginalWithInheritedRadius100() {
        var total = 0
        for (room in RoomCatalog.all) {
            val originals = scene(room.id).nodes.filter { it.attrs["name"]!!.startsWith("EnemySpawnNode") }
            assertEquals(room.id, originals.map { it.attrs["name"] }, room.spawnNodes.map { it.name })
            for ((orig, spawn) in originals.zip(room.spawnNodes)) {
                val pos = GodotText.vec2(orig.props.getValue("position"))
                assertEquals(pos.first.toFloat(), spawn.position.x)
                assertEquals(pos.second.toFloat(), spawn.position.y)
                assertEquals(orig.props["spawnRadius"]?.toInt() ?: 100, spawn.spawnRadius)
                assertTrue("${room.id} spawn node on walkable tile", room.tiles.atlasAt((spawn.position.x / 100).toInt(), Math.floorDiv(spawn.position.y.toInt(), 100))?.let(RoomTileSet::isWalkable) ?: false)
                total++
            }
        }
        assertEquals(47, total)
        assertEquals("null", GodotText.load("enemy/EnemySpawnNode.tscn").node("EnemySpawnNode").props["spawnRadius"])
    }

    @Test
    fun barricadeTileMatchesTheOriginal() {
        val scene = GodotText.load("background/Barricade.tscn")
        val atlas = scene.subResources.values.single { it.attrs["type"] == "TileSetAtlasSource" }
        assertEquals("cone.png", scene.extPath(atlas.props.getValue("texture")))
        assertEquals("PackedVector2Array(-50, -50, 50, -50, 50, 50, -50, 50)", atlas.props["0:0/0/physics_layer_0/polygon_0/points"])
    }
}
