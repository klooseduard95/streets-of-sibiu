package ro.klooseduard.streetsofsibiu.logic.physics

import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.PlayerDefs
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.parity.MiniJson
import ro.klooseduard.streetsofsibiu.logic.parity.Original
import ro.klooseduard.streetsofsibiu.logic.world.RoomCollision
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Replays the Godot 4.6.1 walking traces (Phase 0, v9_player_traces.json) through the ported
 * move_and_slide in the real SpawnRoom and compares the player position at every tick.
 */
class PlayerWalkTraceTest {
    @Suppress("UNCHECKED_CAST")
    private val scenarios: Map<String, Map<String, Any?>> by lazy {
        val json = MiniJson.parse(File(Original.repoRoot, "tools/godot-reference/results/v9_player_traces.json").readText()) as Map<String, Any?>
        json["scenarios"] as Map<String, Map<String, Any?>>
    }

    private val directions = mapOf("move_forward" to (0 to -1), "move_backward" to (0 to 1), "move_leftward" to (-1 to 0), "move_rightward" to (1 to 0))

    /** Max per-tick position error in px for one scenario (walking only, dash NONE, no parry). */
    @Suppress("UNCHECKED_CAST")
    private fun replay(name: String): Double {
        val sc = scenarios.getValue(name)
        val rows = sc["rows"] as List<Map<String, Any?>>
        val inputs = (sc["inputs"] as Map<String, Map<String, List<String>>>).mapKeys { it.key.toDouble().toInt() }
        val clamp = (sc["clamp_rect"] as List<List<Double>>).let { (a, b) -> if (b[0] == 0.0 && b[1] == 0.0) null else floatArrayOf(a[0].toFloat(), a[1].toFloat(), b[0].toFloat(), b[1].toFloat()) }
        val world = RoomCollision.world(RoomCatalog.room("SpawnRoom"))
        val start = (rows[0]["pos"] as List<Double>)
        val body = KinematicBody(BodyShape(PlayerDefs.CAPSULE_RADIUS, PlayerDefs.CAPSULE_HEIGHT), Vec2(start[0].toFloat(), start[1].toFloat()))
        val held = mutableSetOf<String>()
        var maxErr = 0.0
        for (t in 0 until rows.size - 1) {
            if (clamp != null) {
                body.position = Vec2(body.position.x.coerceIn(clamp[0], clamp[2]), body.position.y.coerceIn(clamp[1], clamp[3]))
            }
            val expected = rows[t]["pos"] as List<Double>
            maxErr = max(maxErr, abs(body.position.x - expected[0]).coerceAtLeast(abs(body.position.y - expected[1])))
            inputs[t]?.let { held.removeAll(it["release"] ?: emptyList()); held.addAll(it["press"] ?: emptyList()) }
            var dx = 0.0
            var dy = 0.0
            held.forEach { a -> directions[a]?.let { dx += it.first; dy += it.second } }
            val len = sqrt(dx * dx + dy * dy)
            body.velocity = if (len > 0) DVec(dx / len * PlayerDefs.BASE_SPEED, dy / len * PlayerDefs.BASE_SPEED) else DVec.ZERO
            body.moveAndSlide(world, 1.0 / 60)
        }
        return maxErr
    }

    @Test
    fun walkingMatchesGodotWithinOnePixelEveryTick() {
        val names = listOf(
            "walk_right_into_east_wall", "diagonal_up_right_slide_to_corner", "diagonal_down_left_into_corner",
            "walk_right_into_pillar", "walk_down_into_pillar_top", "diagonal_down_right_into_pillar_corner",
            "walk_left_then_stop", "walk_up_then_stop", "walk_right_through_east_opening_with_room_clamp",
        )
        val errors = names.associateWith { replay(it) }
        errors.forEach { (n, e) -> println("trace %-50s max error %.4f px".format(n, e)) }
        errors.forEach { (n, e) -> assertTrue("$n max error $e px", e <= 1.0) }
    }
}
