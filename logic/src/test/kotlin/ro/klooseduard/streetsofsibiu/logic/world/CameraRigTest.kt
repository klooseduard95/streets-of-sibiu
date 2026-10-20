package ro.klooseduard.streetsofsibiu.logic.world

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.parity.Original
import java.io.File

class CameraRigTest {
    private val json = File(Original.repoRoot, "tools/godot-reference/results/v4_camera.json").readText().replace(Regex("\\s+"), "")
    private val dt = (1.0 / 60).toFloat()

    private fun pairs(key: String): List<Pair<Double, Double>> {
        val block = Regex("\"$key\":\\[((?:\\[[^\\]]*\\],?)*)\\]").find(json)!!.groupValues[1]
        return Regex("\\[(-?[0-9.e+-]+),(-?[0-9.e+-]+)\\]").findAll(block).map { it.groupValues[1].toDouble() to it.groupValues[2].toDouble() }.toList()
    }

    private fun pair(key: String): Pair<Double, Double> =
        Regex("\"${Regex.escape(key)}\":\\[(-?[0-9.e+-]+),(-?[0-9.e+-]+)\\]").find(json)!!.let { it.groupValues[1].toDouble() to it.groupValues[2].toDouble() }

    private fun assertCenter(msg: String, expected: Pair<Double, Double>, actual: Vec2) {
        assertEquals("$msg x", expected.first, actual.x.toDouble(), 1e-3)
        assertEquals("$msg y", expected.second, actual.y.toDouble(), 1e-3)
    }

    @Test
    fun limitsWithoutSmoothingMatchGodot() {
        val rig = CameraRig().apply { smoothingEnabled = false; limitSmoothed = true }
        rig.enterTree()
        rig.limitLeft = 19; rig.limitTop = 24; rig.limitRight = 1973; rig.limitBottom = 1065
        for (target in listOf(Vec2(996f, 545f), Vec2(100f, 100f), Vec2(1900f, 1000f), Vec2(996f, 0f), Vec2(996f, 2000f))) {
            repeat(3) { rig.update(target, dt) }
            assertCenter("$target", pair("(${target.x.toDouble()},${target.y.toDouble()})"), rig.screenCenter)
        }
        rig.limitLeft = 0; rig.limitTop = 0; rig.limitRight = 1500; rig.limitBottom = 900
        for (target in listOf(Vec2(750f, 450f), Vec2(0f, 0f))) {
            repeat(3) { rig.update(target, dt) }
            assertCenter("small $target", pair("both_axes_small(${target.x.toDouble()},${target.y.toDouble()})"), rig.screenCenter)
        }
    }

    @Test
    fun smoothingTrackMatchesGodotTickForTick() {
        val rig = CameraRig().apply { smoothingEnabled = true; limitSmoothed = true }
        rig.enterTree()
        rig.limitLeft = -10000; rig.limitTop = -10000; rig.limitRight = 10000; rig.limitBottom = 10000
        val origin = Vec2(0f, 0f)
        repeat(3) { rig.update(origin, dt) }
        rig.resetSmoothing(origin, dt)
        repeat(2) { rig.update(origin, dt) }
        val target = Vec2(600f, 300f)
        val expected = pairs("smoothing_track_after_jump")
        assertEquals(40, expected.size)
        expected.forEachIndexed { i, e -> rig.update(target, dt); assertCenter("tick $i", e, rig.screenCenter) }

        rig.limitRight = 1500
        val expected2 = pairs("smoothing_track_after_limit_change")
        assertEquals(30, expected2.size)
        expected2.forEachIndexed { i, e -> rig.update(target, dt); assertCenter("limit tick $i", e, rig.screenCenter) }

        rig.resetSmoothing(target, dt)
        rig.update(target, dt)
        assertCenter("after reset", pair("after_reset_smoothing"), rig.screenCenter)
    }

    @Test
    fun roomLimitsTruncateLikeGodot() {
        val expected = mapOf(
            "SpawnRoom" to listOf(19, 24, 1973, 1065),
            "FIGHTROOM_12_MEDIUM" to listOf(41, 24, 2051, 2961),
            "FIGHTROOM_12_BIG" to listOf(62, -635, 4845, 2935),
            "FIGHTROOM_13_BIG" to listOf(62, -557, 4845, 2946),
            "FIGHTROOM_34_BIG" to listOf(39, 24, 8244, 1065),
            "FIGHTROOMBIGENEMY_1" to listOf(51, 43, 2048, 2061),
            "FIGHTROOM_124_SMALL" to listOf(24, 16, 1976, 1072),
        )
        for ((id, ltrb) in expected) {
            val rig = CameraRig()
            rig.setLimitsFromModuleBox(RoomCatalog.room(id).moduleBox)
            assertEquals(id, ltrb, listOf(rig.limitLeft, rig.limitTop, rig.limitRight, rig.limitBottom))
        }
    }

    @Test
    fun spawnRoomViewIsCentredVerticallyBecauseItIsShorterThanTheScreen() {
        val rig = CameraRig()
        rig.setLimitsFromModuleBox(RoomCatalog.room("SpawnRoom").moduleBox)
        rig.enterTree()
        val centre = RoomCatalog.room("SpawnRoom").moduleBox.center
        rig.update(centre, dt)
        repeat(120) { rig.update(centre, dt) }
        assertEquals(544.5, rig.screenCenter.y.toDouble(), 1e-3)
    }
}
