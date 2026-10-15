package ro.klooseduard.streetsofsibiu.core.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomDef
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomTileSet
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Draws a room's TileMapLayer like Godot: cell (x, y) at [x*100, y*100] in room-local
 * coordinates, 1:1 texels, Nearest filtering, padded atlas (no seams), culled to the visible area.
 */
class RoomRenderer(private val atlas: PaddedTileAtlas) {
    private val size = RoomTileSet.TILE_SIZE

    /** Draws [room] placed at [origin] (world position of the room node), limited to the [viewTopLeft] view. */
    fun draw(batch: SpriteBatch, room: RoomDef, origin: Vec2, viewTopLeft: Vec2, viewWidth: Float, viewHeight: Float) {
        val tiles = room.tiles
        val x0 = maxOf(tiles.minX, floor((viewTopLeft.x - origin.x) / size).toInt() - 1)
        val y0 = maxOf(tiles.minY, floor((viewTopLeft.y - origin.y) / size).toInt() - 1)
        val x1 = minOf(tiles.minX + tiles.width - 1, ceil((viewTopLeft.x + viewWidth - origin.x) / size).toInt())
        val y1 = minOf(tiles.minY + tiles.height - 1, ceil((viewTopLeft.y + viewHeight - origin.y) / size).toInt())
        for (y in y0..y1) for (x in x0..x1) {
            val atlas = tiles.atlasAt(x, y) ?: continue
            batch.draw(this.atlas.region(atlas.x, atlas.y), origin.x + x * size, origin.y + y * size, size.toFloat(), size.toFloat())
        }
    }
}
