package ro.klooseduard.streetsofsibiu.logic.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimersTest {
    private val runtime = GameRuntime(GameRandom(1))
    private val loop = GameLoop(runtime, object : FrameHooks {})
    private fun frames(n: Int) = repeat(n) { loop.advance(1.0 / 60) }

    /** Expected frames computed with Godot's double arithmetic (time_left -= 1/60; fire at <= 0). */
    @Test
    fun sceneTimerFiresOnTheSameFrameAsGodot() {
        val fired = mutableMapOf<Double, Int>()
        var frame = 0
        for (seconds in listOf(0.1, 0.2, 0.25, 1.0)) runtime.createTimer(seconds, processAlways = false).onTimeout { fired[seconds] = frame }
        repeat(70) { frame++; loop.advance(1.0 / 60) }
        assertEquals(mapOf(0.1 to 7, 0.2 to 13, 0.25 to 16, 1.0 to 60), fired)
        assertEquals(0, runtime.activeSceneTimerCount)
    }

    @Test
    fun pausableSceneTimerStopsWhilePausedButProcessAlwaysRuns() {
        var pausable = false
        var always = false
        runtime.createTimer(0.25, processAlways = false).onTimeout { pausable = true }
        runtime.createTimer(0.25, processAlways = true).onTimeout { always = true }
        runtime.paused = true
        frames(30)
        assertTrue(always)
        assertFalse(pausable)
        runtime.paused = false
        frames(16)
        assertTrue(pausable)
    }

    @Test
    fun timerCreatedInsideTimeoutWaitsForTheNextFrame() {
        val log = mutableListOf<String>()
        runtime.createTimer(0.01, processAlways = false).onTimeout {
            log += "first"
            runtime.createTimer(0.0, processAlways = false).onTimeout { log += "second" }
        }
        frames(1)
        assertEquals(listOf("first"), log)
        frames(1)
        assertEquals(listOf("first", "second"), log)
    }

    @Test
    fun nodeTimerUsesStrictLessThanAndAddsWaitTimeWhenRepeating() {
        val owner = SceneNode()
        val firedAt = mutableListOf<Int>()
        var frame = 0
        runtime.createNodeTimer(owner, waitTime = 0.15, autostart = true).onTimeout { firedAt += frame }
        repeat(30) { frame++; loop.advance(1.0 / 60) }
        assertEquals(listOf(10, 19, 28), firedAt)  // Godot double arithmetic, strict < 0
    }

    @Test
    fun oneShotNodeTimerStopsAndReportsStopped() {
        val owner = SceneNode()
        var count = 0
        val timer = runtime.createNodeTimer(owner, oneShot = true).onTimeout { count++ }
        assertTrue(timer.isStopped)
        timer.start(0.5)
        assertFalse(timer.isStopped)
        assertEquals(0.5, timer.waitTime, 0.0)
        frames(60)
        assertEquals(1, count)
        assertTrue(timer.isStopped)
        assertEquals(-1.0, timer.timeLeft, 0.0)
    }

    @Test
    fun nodeTimerFreezesWhileOwnerIsDetachedDisabledOrPaused() {
        val room = SceneNode()
        val enemy = SceneNode(parent = room)
        val timer = runtime.createNodeTimer(enemy, waitTime = 1.0, autostart = true)
        frames(30)
        val left = timer.timeLeft
        room.setAttached(false)          // room removed from the tree (inactive room)
        frames(30)
        assertEquals(left, timer.timeLeft, 0.0)
        room.setAttached(true)
        room.processMode = ProcessMode.DISABLED   // warp fade
        frames(30)
        assertEquals(left, timer.timeLeft, 0.0)
        room.processMode = ProcessMode.INHERIT
        runtime.paused = true
        frames(30)
        assertEquals(left, timer.timeLeft, 0.0)
        runtime.paused = false
        frames(1)
        assertTrue(timer.timeLeft < left)
    }

    @Test
    fun alwaysModeNodeTimerRunsWhilePaused() {
        val menu = SceneNode(ProcessMode.ALWAYS)
        val timer = runtime.createNodeTimer(menu, waitTime = 1.0, autostart = true)
        runtime.paused = true
        frames(10)
        assertTrue(timer.timeLeft < 1.0)
    }
}
