package ro.klooseduard.streetsofsibiu.logic.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TweenTest {
    private val runtime = GameRuntime(GameRandom(1))
    private val loop = GameLoop(runtime, object : FrameHooks {})
    private fun frames(n: Int) = repeat(n) { loop.advance(1.0 / 60) }

    @Test
    fun easingMatchesGodotReferenceSamples() {
        val root = File(System.getProperty("sos.repoRoot") ?: error("sos.repoRoot not set"))
        val json = File(root, "tools/godot-reference/results/v2_defaults.json").readText()
        val names = mapOf("LINEAR" to TransitionType.LINEAR, "SINE" to TransitionType.SINE)
        val eases = mapOf("IN" to EaseType.IN, "OUT" to EaseType.OUT, "IN_OUT" to EaseType.IN_OUT, "OUT_IN" to EaseType.OUT_IN)
        var checked = 0
        for ((transName, trans) in names) for ((easeName, ease) in eases) {
            val block = Regex("\"${transName}_$easeName\": \\[([^\\]]*)]").find(json)?.groupValues?.get(1)
                ?: error("missing ${transName}_$easeName")
            val expected = block.split(',').map { it.trim().toDouble() }
            expected.forEachIndexed { i, value ->
                assertEquals("${transName}_$easeName t=${i / 10.0}", value, Easing.interpolate(0.0, 1.0, i / 10.0, 1.0, trans, ease), 1e-7)
                checked++
            }
        }
        assertEquals(88, checked)
    }

    @Test
    fun sequentialStepsCarryLeftoverDeltaAndReadStartValueWhenStepBegins() {
        var value = 0.0
        val tween = runtime.createTween(null)
        tween.tweenProperty({ value }, { value = it }, 10.0, 0.1)
        tween.tweenProperty({ value }, { value = it }, 20.0, 0.1)
        frames(7)
        assertEquals(10.0 + 10.0 * ((7.0 / 60 - 0.1) / 0.1).toFloat(), value, 1e-9)
        frames(6)
        assertEquals(20.0, value, 0.0)
        assertTrue(tween.isDead)
    }

    @Test
    fun parallelTweenersRunTogetherAndFinishedFiresOnce() {
        var alpha = 1.0
        var scale = 1.0
        var finished = 0
        val tween = runtime.createTween(SceneNode()).setTrans(TransitionType.SINE)
        tween.tweenProperty({ alpha }, { alpha = it }, 0.0, 1.0)
        tween.parallel().tweenProperty({ scale }, { scale = it }, 0.0, 1.0)
        tween.onFinished { finished++ }
        frames(30)
        val expected = 1.0 - Easing.run(TransitionType.SINE, EaseType.IN_OUT, (30.0 / 60).toFloat(), 0f, 1f, 1f)
        assertEquals(expected, alpha, 1e-6)
        assertEquals(alpha, scale, 0.0)
        frames(31)
        assertEquals(0.0, alpha, 0.0)
        assertEquals(1, finished)
        assertEquals(0, runtime.activeTweenCount)
    }

    @Test
    fun infiniteLoopKeepsBobbing() {
        var y = 0.0
        val tween = runtime.createTween(SceneNode()).setLoops().setTrans(TransitionType.SINE).setEase(EaseType.IN_OUT)
        tween.tweenProperty({ y }, { y = it }, 5.0, 1.0)
        tween.tweenProperty({ y }, { y = it }, 0.0, 1.0)
        frames(60 * 10 + 30)
        assertFalse(tween.isDead)
        assertTrue(y in 0.0..5.0)
    }

    @Test
    fun boundTweenWaitsOutsideTreeAndDiesWhenNodeIsFreed() {
        var value = 0.0
        val node = SceneNode()
        runtime.createTween(node).tweenProperty({ value }, { value = it }, 1.0, 1.0)
        frames(15)
        val partial = value
        node.setAttached(false)
        frames(30)
        assertEquals(partial, value, 0.0)
        node.setAttached(true)
        frames(1)
        assertTrue(value > partial)
        runtime.queueFree(node)
        frames(1)
        assertEquals(0, runtime.activeTweenCount)
    }

    @Test
    fun boundTweenPausesWithTreeUnlessNodeProcessesAlways() {
        var normal = 0.0
        var always = 0.0
        runtime.createTween(SceneNode()).tweenProperty({ normal }, { normal = it }, 1.0, 1.0)
        runtime.createTween(SceneNode(ProcessMode.ALWAYS)).tweenProperty({ always }, { always = it }, 1.0, 1.0)
        runtime.paused = true
        frames(30)
        assertEquals(0.0, normal, 0.0)
        assertTrue(always > 0.0)
    }

    @Test
    fun unboundTweenStopsWhilePaused() {
        var value = 0.0
        runtime.createTween(null).tweenProperty({ value }, { value = it }, 1.0, 1.0)
        runtime.paused = true
        frames(10)
        assertEquals(0.0, value, 0.0)
    }

    @Test
    fun transitionIsCapturedWhenTweenerIsAppended() {
        var a = 0.0
        var b = 0.0
        val tween = runtime.createTween(null)
        tween.tweenProperty({ a }, { a = it }, 1.0, 1.0)
        tween.setTrans(TransitionType.SINE)
        tween.parallel().tweenProperty({ b }, { b = it }, 1.0, 1.0)
        frames(15)
        assertEquals((15.0 / 60).toFloat().toDouble(), a, 1e-6)
        assertEquals(Easing.run(TransitionType.SINE, EaseType.IN_OUT, (15.0 / 60).toFloat(), 0f, 1f, 1f).toDouble(), b, 1e-6)
    }

    @Test
    fun emptyTweenIsDiscarded() {
        runtime.createTween(null)
        frames(1)
        assertEquals(0, runtime.activeTweenCount)
    }
}
