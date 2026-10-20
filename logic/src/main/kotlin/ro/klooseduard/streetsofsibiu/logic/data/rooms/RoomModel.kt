package ro.klooseduard.streetsofsibiu.logic.data.rooms

import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2

enum class DoorOrientation {
    NORTH, SOUTH, EAST, WEST;

    val opposite: DoorOrientation
        get() = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
        }
}

data class AtlasCoord(val x: Int, val y: Int)

object RoomTileSet {
    const val TILE_SIZE = 100
    const val TEXTURE = "tilesetbig.png"
    const val ATLAS_COLUMNS = 8
    const val ATLAS_ROWS = 5

    fun isSolid(c: AtlasCoord): Boolean = (c.y == 0 && c.x != 1) || c.y == 4

    fun isWalkable(c: AtlasCoord): Boolean = !isSolid(c)
}

class TileGrid(val minX: Int, val minY: Int, val width: Int, val height: Int, private val rows: List<String>) {
    init {
        require(rows.size == height && rows.all { it.length == width * 2 }) { "malformed tile rows" }
    }

    fun atlasAt(x: Int, y: Int): AtlasCoord? {
        val col = x - minX
        val row = y - minY
        if (row !in 0 until height || col !in 0 until width) return null
        val a = rows[row][col * 2]
        val b = rows[row][col * 2 + 1]
        return if (a == '.') null else AtlasCoord(a - '0', b - '0')
    }

    val cells: List<Pair<Pair<Int, Int>, AtlasCoord>> by lazy {
        buildList {
            for (y in minY until minY + height) for (x in minX until minX + width) {
                atlasAt(x, y)?.let { add((x to y) to it) }
            }
        }
    }
}

data class NodeTransform(val position: Vec2, val rotation: Float, val scale: Vec2)

data class DoorDef(
    val name: String,
    val orientation: DoorOrientation,
    val transform: NodeTransform,
    val teleporter: Rect,
    val preventer: Rect,
    val arrival: Vec2,
)

data class SpawnNodeDef(val name: String, val position: Vec2, val spawnRadius: Int)

data class RoomDef(
    val id: String,
    val tiles: TileGrid,
    val doors: List<DoorDef>,
    val moduleBox: Rect,
    val spawnNodes: List<SpawnNodeDef>,
)
