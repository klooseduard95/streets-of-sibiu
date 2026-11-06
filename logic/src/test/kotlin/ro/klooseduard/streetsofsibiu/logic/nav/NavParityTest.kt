package ro.klooseduard.streetsofsibiu.logic.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.parity.MiniJson
import ro.klooseduard.streetsofsibiu.logic.parity.Original
import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import java.io.File
import kotlin.math.abs
import kotlin.math.max

class NavParityTest {
    @Suppress("UNCHECKED_CAST")
    private val rooms = (MiniJson.parse(File(Original.repoRoot, "tools/godot-reference/results/v8_navigation.json").readText())
        as Map<String, Any?>)["rooms"] as Map<String, Map<String, Any?>>

    @Suppress("UNCHECKED_CAST")
    private fun toPath(raw: Any?): List<DVec> = (raw as List<List<Double>>).map { DVec(it[0], it[1]) }

    private fun maxDeviation(a: List<DVec>, b: List<DVec>): Double {
        if (a.size != b.size) return Double.MAX_VALUE
        return a.indices.maxOfOrNull { max(abs(a[it].x - b[it].x), abs(a[it].y - b[it].y)) } ?: 0.0
    }

    @Test
    fun walkableTileCountsMatchGodot() {
        for ((name, data) in rooms) {
            assertEquals(name, (data["walkable_tiles"] as Double).toInt(), RoomNav.mesh(RoomCatalog.room(name)).polygons.size)
        }
    }

    @Test
    fun recordedGodotPathsAreReproducedExactly() {
        var total = 0
        var worst = 0.0
        for ((name, data) in rooms) {
            val query = NavQuery(RoomNav.mesh(RoomCatalog.room(name)))
            @Suppress("UNCHECKED_CAST")
            for ((index, q) in (data["queries"] as List<Map<String, Any?>>).withIndex()) {
                val from = (q["from"] as List<Double>).let { DVec(it[0], it[1]) }
                val to = (q["to"] as List<Double>).let { DVec(it[0], it[1]) }
                for ((key, limit) in listOf("path_limited_500" to 500.0, "path_unlimited" to 0.0)) {
                    val expected = toPath(q[key])
                    val mine = query.path(from, to, 512, limit)
                    val dev = maxDeviation(expected, mine)
                    total++
                    worst = max(worst, if (dev == Double.MAX_VALUE) 1e9 else dev)
                    assertTrue("$name query $index $key: ${expected.size} vs ${mine.size} points, deviation $dev", dev < 0.01)
                }
            }
        }
        println("nav: $total paths reproduced, worst deviation %.6f px".format(worst))
    }
}
