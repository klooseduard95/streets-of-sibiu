package ro.klooseduard.streetsofsibiu.logic.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.parity.MiniJson
import ro.klooseduard.streetsofsibiu.logic.parity.Original
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.runtime.FrameHooks
import ro.klooseduard.streetsofsibiu.logic.runtime.GameLoop
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRandom
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime
import java.io.File
import kotlin.math.abs
import kotlin.math.max

class PlayerTraceTest {
    @Suppress("UNCHECKED_CAST")
    private val scenarios = (MiniJson.parse(File(Original.repoRoot, "tools/godot-reference/results/v9_player_traces.json").readText())
        as Map<String, Any?>)["scenarios"] as Map<String, Map<String, Any?>>

    private val actions = mapOf(
        "move_forward" to Action.MOVE_FORWARD, "move_backward" to Action.MOVE_BACKWARD,
        "move_leftward" to Action.MOVE_LEFTWARD, "move_rightward" to Action.MOVE_RIGHTWARD,
    )

    @Suppress("UNCHECKED_CAST")
    private fun replay(name: String): Pair<Double, Int> {
        val sc = scenarios.getValue(name)
        val rows = sc["rows"] as List<Map<String, Any?>>
        val inputs = (sc["inputs"] as Map<String, Map<String, List<String>>>).mapKeys { it.key.toDouble().toInt() }
        val clamp = (sc["clamp_rect"] as List<List<Double>>).let { (a, b) -> if (b[0] == 0.0 && b[1] == 0.0) null else listOf(a, b) }
        val start = rows[0]["pos"] as List<Double>
        val runtime = GameRuntime(GameRandom(1))
        val input = ActionInput<Action>(runtime)
        val player = Player(Vec2(start[0].toFloat(), start[1].toFloat()))
        val world = ro.klooseduard.streetsofsibiu.logic.world.RoomCollision.world(RoomCatalog.room("SpawnRoom"))
        var tick = 0
        var maxErr = 0.0
        var frameMismatches = 0
        val loop = GameLoop(runtime, object : FrameHooks {
            override fun flushPhysicsQueries() {
                if (tick < rows.size) {
                    val expected = rows[tick]
                    val pos = expected["pos"] as List<Double>
                    maxErr = max(maxErr, max(abs(player.position.x - pos[0]), abs(player.position.y - pos[1])))
                    if ((expected["frame"] as Double).toInt() != player.animator.frame) frameMismatches++
                }
                if (clamp != null) {
                    val p = player.body.position
                    player.body.position = Vec2(p.x.coerceIn(clamp[0][0].toFloat(), clamp[1][0].toFloat()), p.y.coerceIn(clamp[0][1].toFloat(), clamp[1][1].toFloat()))
                }
                inputs[tick]?.let { step ->
                    step["release"]?.forEach { actions[it]?.let(input::release) }
                    step["press"]?.forEach { actions[it]?.let(input::press) }
                }
            }
            override fun physicsProcess(delta: Double) = player.physicsProcess(input, world, delta)
            override fun process(delta: Double) = player.process(delta)
        })
        while (tick < rows.size) {
            loop.advance(1.0 / 60)
            tick++
        }
        return maxErr to frameMismatches
    }

    @Test
    fun playerWalkingAndAnimationMatchGodot() {
        val names = listOf(
            "walk_right_into_east_wall", "diagonal_up_right_slide_to_corner", "diagonal_down_left_into_corner",
            "walk_right_into_pillar", "walk_down_into_pillar_top", "diagonal_down_right_into_pillar_corner",
            "walk_left_then_stop", "walk_up_then_stop", "walk_right_through_east_opening_with_room_clamp",
        )
        for (n in names) {
            val (err, frames) = replay(n)
            println("player %-50s max pos error %.4f px, frame mismatches %d".format(n, err, frames))
            assertTrue("$n position error $err", err <= 1.0)
            assertEquals("$n sprite frames", 0, frames)
        }
    }
}
