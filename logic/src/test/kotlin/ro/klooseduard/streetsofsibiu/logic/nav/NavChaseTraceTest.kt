package ro.klooseduard.streetsofsibiu.logic.nav

import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.parity.MiniJson
import ro.klooseduard.streetsofsibiu.logic.parity.Original
import ro.klooseduard.streetsofsibiu.logic.physics.BodyShape
import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import ro.klooseduard.streetsofsibiu.logic.physics.KinematicBody
import ro.klooseduard.streetsofsibiu.logic.physics.MotionMode
import ro.klooseduard.streetsofsibiu.logic.world.RoomCollision
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class NavChaseTraceTest {
    @Suppress("UNCHECKED_CAST")
    private val rooms = (MiniJson.parse(File(Original.repoRoot, "tools/godot-reference/results/v8_navigation.json").readText())
        as Map<String, Any?>)["rooms"] as Map<String, Map<String, Any?>>

    @Suppress("UNCHECKED_CAST")
    private fun replay(name: String): Triple<Double, Double, Int> {
        val trace = rooms.getValue(name)["chase_trace"] as Map<String, Any?>
        val start = (trace["start"] as List<Double>).let { Vec2(it[0].toFloat(), it[1].toFloat()) }
        val targets = (trace["targets"] as List<List<Double>>).map { Vec2(it[0].toFloat(), it[1].toFloat()) }
        val rows = trace["rows"] as List<Map<String, Any?>>
        val room = RoomCatalog.room(name)
        val world = RoomCollision.world(room)
        val body = KinematicBody(BodyShape(10.0, 20.0), start, MotionMode.FLOATING)
        body.wallMinSlideAngle = 0.0
        val agent = NavAgent(NavQuery(RoomNav.mesh(room)), { body.position })
        var maxPos = 0.0
        var maxNext = 0.0
        var finishedMismatch = 0
        for ((t, row) in rows.withIndex()) {
            if (t % 60 == 0) agent.setTarget(targets[(t / 60) % targets.size])
            val next = agent.nextPathPosition()
            val dx = (next.x - body.position.x).toDouble()
            val dy = (next.y - body.position.y).toDouble()
            val len = sqrt(dx * dx + dy * dy)
            body.velocity = if (len > 0) DVec(dx / len * 300.0, dy / len * 300.0) else DVec.ZERO
            body.moveAndSlide(world, 1.0 / 60)
            val pos = row["pos"] as List<Double>
            val exp = row["next"] as List<Double>
            maxPos = max(maxPos, max(abs(body.position.x - pos[0]), abs(body.position.y - pos[1])))
            maxNext = max(maxNext, max(abs(next.x - exp[0]), abs(next.y - exp[1])))
            if (agent.isNavigationFinished() != (row["finished"] as Boolean)) finishedMismatch++
        }
        return Triple(maxPos, maxNext, finishedMismatch)
    }

    @Test
    fun chaseTracesMatchGodot() {
        for (name in listOf("SpawnRoom", "FIGHTROOM_13_BIG")) {
            val (pos, next, finished) = replay(name)
            println("chase %-18s max position error %.4f px, next waypoint %.4f px, finished mismatches %d".format(name, pos, next, finished))
            assertTrue("$name position $pos", pos <= 1.0)
            assertTrue("$name next $next", next <= 1.0)
        }
    }
}
