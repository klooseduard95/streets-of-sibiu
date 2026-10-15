package ro.klooseduard.streetsofsibiu.logic.data.rooms

import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2

/** Godot `GenerationNode.orientation`. */
enum class DoorOrientation {
    NORTH, SOUTH, EAST, WEST;

    /** `RoomModule.opposite`. */
    val opposite: DoorOrientation
        get() = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
        }
}

/** Atlas coordinate in `tilesetbig.png` (8x5 grid of 100x100 tiles). */
data class AtlasCoord(val x: Int, val y: Int)

/** Tile semantics shared by every room's TileSet (verified identical in all 20 rooms). */
object RoomTileSet {
    const val TILE_SIZE = 100
    const val TEXTURE = "tilesetbig.png"
    const val ATLAS_COLUMNS = 8
    const val ATLAS_ROWS = 5

    /** Full-square collision on physics layer 1: row 0 except (1,0), plus all of row 4. */
    fun isSolid(c: AtlasCoord): Boolean = (c.y == 0 && c.x != 1) || c.y == 4

    /** Full-square navigation polygon on navigation layer 2 (agent radius 0): every other tile. */
    fun isWalkable(c: AtlasCoord): Boolean = !isSolid(c)
}

/**
 * A room's TileMapLayer (no transform in any room). Cell (x, y) covers
 * [x*100, x*100+100) × [y*100, y*100+100) in room-local coordinates; coordinates may be negative.
 */
class TileGrid(val minX: Int, val minY: Int, val width: Int, val height: Int, private val rows: List<String>) {
    init {
        require(rows.size == height && rows.all { it.length == width * 2 }) { "malformed tile rows" }
    }

    /** The atlas tile at cell ([x], [y]), or null if the cell is empty. */
    fun atlasAt(x: Int, y: Int): AtlasCoord? {
        val col = x - minX
        val row = y - minY
        if (row !in 0 until height || col !in 0 until width) return null
        val a = rows[row][col * 2]
        val b = rows[row][col * 2 + 1]
        return if (a == '.') null else AtlasCoord(a - '0', b - '0')
    }

    /** All non-empty cells in row-major order. */
    val cells: List<Pair<Pair<Int, Int>, AtlasCoord>> by lazy {
        buildList {
            for (y in minY until minY + height) for (x in minX until minX + width) {
                atlasAt(x, y)?.let { add((x to y) to it) }
            }
        }
    }
}

/** A Godot `Node2D` transform as stored in the scene (rotation in radians). */
data class NodeTransform(val position: Vec2, val rotation: Float, val scale: Vec2)

/**
 * A GenerationNode (door). Geometry is room-local and already transformed by the node's
 * transform: the Teleporter area, the Preventer static body and the ArrivalNode point.
 */
data class DoorDef(
    val name: String,
    val orientation: DoorOrientation,
    val transform: NodeTransform,
    val teleporter: Rect,
    val preventer: Rect,
    val arrival: Vec2,
)

/** An EnemySpawnNode placed in the room scene. `spawnRadius` resolves to 100 everywhere (V2). */
data class SpawnNodeDef(val name: String, val position: Vec2, val spawnRadius: Int)

/** One room module scene (`background/modules/<id>.tscn`). Node order is kept from the scene. */
data class RoomDef(
    val id: String,
    val tiles: TileGrid,
    val doors: List<DoorDef>,
    /** ModuleBox shape rectangle: overlap test during generation, camera limits, room centre. */
    val moduleBox: Rect,
    val spawnNodes: List<SpawnNodeDef>,
)
