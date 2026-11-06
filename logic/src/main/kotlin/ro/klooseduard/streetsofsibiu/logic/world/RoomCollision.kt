package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomDef
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomTileSet
import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld

object RoomCollision {
    fun solids(room: RoomDef, origin: Vec2 = Vec2.ZERO): List<Rect> {
        val size = RoomTileSet.TILE_SIZE.toFloat()
        val tiles = room.tiles.cells.filter { RoomTileSet.isSolid(it.second) }
            .map { (cell, _) -> Rect(origin.x + cell.first * size, origin.y + cell.second * size, size, size) }
        return tiles + room.doors.map { it.preventer.translated(origin) }
    }

    fun world(room: RoomDef, origin: Vec2 = Vec2.ZERO) = StaticWorld(solids(room, origin))
}
