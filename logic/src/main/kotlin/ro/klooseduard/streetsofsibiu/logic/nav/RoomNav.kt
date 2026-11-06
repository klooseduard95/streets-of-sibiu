package ro.klooseduard.streetsofsibiu.logic.nav

import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomDef
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomTileSet

object RoomNav {
    fun mesh(room: RoomDef): NavMesh {
        val cells = room.tiles.cellOrder.filter { (x, y) ->
            room.tiles.atlasAt(x, y)?.let(RoomTileSet::isWalkable) == true
        }
        return NavMesh(cells)
    }
}
