package ro.klooseduard.streetsofsibiu.logic.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.DashType
import ro.klooseduard.streetsofsibiu.logic.data.ParryType
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
        "blink" to Action.BLINK, "parry" to Action.PARRY,
    )

    private class Result(
        var posError: Double = 0.0, var velError: Double = 0.0, var frameMismatches: Int = 0,
        var stateMismatches: Int = 0, var modulateError: Double = 0.0, var cooldownError: Double = 0.0,
        var firstStateMismatch: String = "",
        val mine: MutableList<Pair<Boolean, Boolean>> = mutableListOf(),
        val godot: MutableList<Pair<Boolean, Boolean>> = mutableListOf(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun replay(name: String): Result {
        val sc = scenarios.getValue(name)
        val rows = sc["rows"] as List<Map<String, Any?>>
        val inputs = (sc["inputs"] as Map<String, Map<String, List<String>>>).mapKeys { it.key.toDouble().toInt() }
        val clamp = (sc["clamp_rect"] as List<List<Double>>).let { (a, b) -> if (b[0] == 0.0 && b[1] == 0.0) null else listOf(a, b) }
        val start = rows[0]["pos"] as List<Double>
        val runtime = GameRuntime(GameRandom(1))
        val input = ActionInput<Action>(runtime)
        val player = Player(Vec2(start[0].toFloat(), start[1].toFloat()), runtime)
        player.selectedDash = DashType.valueOf(sc["dash"] as String)
        player.selectedParry = (sc["parry"] as Double).toInt().let { ParryType.entries.getOrNull(it) }
        if (sc["setup"] == "consumable") player.heldItem = HeldConsumable()
        val world = RoomCollision.world(RoomCatalog.room("SpawnRoom"))
        var tick = 0
        val result = Result()
        val loop = GameLoop(runtime, object : FrameHooks {
            override fun flushPhysicsQueries() {
                if (clamp != null) {
                    val p = player.body.position
                    player.body.position = Vec2(p.x.coerceIn(clamp[0][0].toFloat(), clamp[1][0].toFloat()), p.y.coerceIn(clamp[0][1].toFloat(), clamp[1][1].toFloat()))
                }
                if (tick < rows.size) compare(rows[tick], player, result, tick)
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
        for (t in result.godot.indices) {
            val window = (t - 1..t + 1).filter { it in result.mine.indices }.map { result.mine[it] }
            if (result.godot[t] !in window) {
                result.stateMismatches++
                if (result.firstStateMismatch.isEmpty()) {
                    result.firstStateMismatch = "tick $t: (sliding, can_take_dmg) ${result.mine[t]}, Godot ${result.godot[t]}"
                }
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun compare(expected: Map<String, Any?>, player: Player, r: Result, tick: Int) {
        val pos = expected["pos"] as List<Double>
        val vel = expected["vel"] as List<Double>
        r.posError = max(r.posError, max(abs(player.position.x - pos[0]), abs(player.position.y - pos[1])))
        if (expected["sliding"] as Boolean) r.velError = max(r.velError, max(abs(player.body.velocity.x - vel[0]), abs(player.body.velocity.y - vel[1])))
        if ((expected["frame"] as Double).toInt() != player.animator.frame) r.frameMismatches++
        r.mine += player.isSliding to player.canTakeDmg
        r.godot += (expected["sliding"] as Boolean) to (expected["can_take_dmg"] as Boolean)
        val mod = expected["modulate"] as List<Double>
        val m = player.modulate
        r.modulateError = max(r.modulateError, listOf(abs(m.r - mod[0]), abs(m.g - mod[1]), abs(m.b - mod[2]), abs(m.a - mod[3])).max())
        val cooldown = max(expected["dash_cooldown_left"] as Double, 0.0)
        r.cooldownError = max(r.cooldownError, abs(max(player.dashCooldownLeft, 0.0) - cooldown))
    }

    private fun check(
        names: List<String>, maxModulateError: Double = 0.25, maxCooldownError: Double = 0.05,
        maxVelocityError: Double = 1.0, maxPositionError: Double = 1.0,
    ) {
        val failures = mutableListOf<String>()
        for (n in names) {
            val r = replay(n)
            println(
                "player %-52s pos %.4f px, vel %.3f, frames %d, state mismatches %d, modulate %.3f, cooldown %.4f s %s"
                    .format(n, r.posError, r.velError, r.frameMismatches, r.stateMismatches, r.modulateError, r.cooldownError, r.firstStateMismatch),
            )
            if (r.posError > maxPositionError) failures += "$n position error ${r.posError}"
            if (r.velError > maxVelocityError) failures += "$n velocity error ${r.velError}"
            if (r.frameMismatches != 0) failures += "$n sprite frames ${r.frameMismatches}"
            if (r.stateMismatches != 0) failures += "$n state: ${r.firstStateMismatch}"
            if (r.modulateError > maxModulateError) failures += "$n modulate error ${r.modulateError}"
            if (r.cooldownError > maxCooldownError) failures += "$n cooldown error ${r.cooldownError}"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun playerWalkingAndAnimationMatchGodot() = check(
        listOf(
            "walk_right_into_east_wall", "diagonal_up_right_slide_to_corner", "diagonal_down_left_into_corner",
            "walk_right_into_pillar", "walk_down_into_pillar_top", "diagonal_down_right_into_pillar_corner",
            "walk_left_then_stop", "walk_up_then_stop", "walk_right_through_east_opening_with_room_clamp",
        ),
    )

    @Test
    fun dashesMatchGodot() = check(
        listOf(
            "none_dash_mini_slide", "short_dash_open_floor", "short_dash_diagonal_open",
            "short_dash_into_pillar", "short_dash_up_through_top_door_with_room_clamp", "short_dash_standing_still",
        ),
    )

    @Test
    fun dashTeleportedDeepIntoTwoTileWallEndsOutsideLikeGodotWithinKnownTolerance() = check(
        listOf("short_dash_into_east_wall", "short_dash_into_east_wall_with_room_clamp"), maxPositionError = 30.0,
    )

    @Test
    fun parriesAndScriptErrorPathsMatchGodot() = check(
        listOf("normal_parry", "medium_parry_with_consumable_selected", "medium_dash_with_consumable_selected"),
    )
}
