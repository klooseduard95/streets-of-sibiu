package ro.klooseduard.streetsofsibiu.logic.nav

import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import kotlin.math.floor

class NavPolygon(val id: Int, val vertices: List<DVec>)

class NavConnection(val polygon: NavPolygon, val edge: Int, val pathwayStart: DVec, val pathwayEnd: DVec)

class NavMesh(cells: List<Pair<Int, Int>>, tile: Double = 100.0) {
    val polygons: List<NavPolygon>
    val connections: List<MutableList<NavConnection>>

    private data class EdgeKey(val a: Long, val b: Long)

    private class Pending(val polygon: NavPolygon, val edge: Int, val start: DVec, val end: DVec)

    init {
        val half = tile / 2
        polygons = cells.mapIndexed { index, (cx, cy) ->
            val x = cx * tile + half
            val y = cy * tile + half
            NavPolygon(index, listOf(DVec(x + half, y + half), DVec(x - half, y + half), DVec(x - half, y - half), DVec(x + half, y - half)))
        }
        connections = List(polygons.size) { mutableListOf<NavConnection>() }
        val pairs = LinkedHashMap<EdgeKey, MutableList<Pending>>()
        for (polygon in polygons) {
            for (e in polygon.vertices.indices) {
                val v1 = polygon.vertices[e]
                val v2 = polygon.vertices[(e + 1) % polygon.vertices.size]
                val k1 = pointKey(v1)
                val k2 = pointKey(v2)
                val key = if (k1 > k2) EdgeKey(k2, k1) else EdgeKey(k1, k2)
                val list = pairs.getOrPut(key) { mutableListOf() }
                if (list.size < 2) list += Pending(polygon, e, v1, v2)
            }
        }
        for (list in pairs.values) {
            if (list.size == 2) {
                val c1 = list[0]
                val c2 = list[1]
                connections[c1.polygon.id] += NavConnection(c2.polygon, c2.edge, c2.start, c2.end)
                connections[c2.polygon.id] += NavConnection(c1.polygon, c1.edge, c1.start, c1.end)
            }
        }
    }

    private fun pointKey(p: DVec): Long {
        val x = floor(p.x).toInt()
        val y = floor(p.y).toInt()
        return (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)
    }
}
