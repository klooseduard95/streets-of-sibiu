package ro.klooseduard.streetsofsibiu.logic.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class GameLoopTest {
    private class Recorder : FrameHooks {
        val calls = mutableListOf<String>()
        var physicsDeltaSum = 0.0
        var idleDeltas = mutableListOf<Double>()
        override fun flushPhysicsQueries() { calls += "queries" }
        override fun physicsProcess(delta: Double) { calls += "physics"; physicsDeltaSum += delta }
        override fun syncNavigation() { calls += "navigation" }
        override fun stepPhysics(delta: Double) { calls += "step" }
        override fun process(delta: Double) { calls += "process"; idleDeltas += delta }
    }

    private fun run(frameDelta: Double, frames: Int): Pair<List<Int>, Recorder> {
        val recorder = Recorder()
        val loop = GameLoop(GameRuntime(GameRandom(1)), recorder)
        return List(frames) { loop.advance(frameDelta) } to recorder
    }

    @Test
    fun sixtyHertzDisplayRunsOneTickPerFrame() {
        val (steps, _) = run(1.0 / 60, 600)
        assertEquals(List(600) { 1 }, steps)
    }

    @Test
    fun hundredTwentyHertzDisplayRunsSixtyTicksPerSecond() {
        val (steps, _) = run(1.0 / 120, 1200)
        assertEquals(600, steps.sum())
        assert(steps.all { it in 0..1 })
    }

    @Test
    fun thirtyHertzDisplayRunsTwoTicksPerFrame() {
        val (steps, _) = run(1.0 / 30, 300)
        assertEquals(List(300) { 2 }, steps)
    }

    @Test
    fun longFrameIsCappedAtEightTicksAndExcessIsDropped() {
        val recorder = Recorder()
        val loop = GameLoop(GameRuntime(GameRandom(1)), recorder)
        assertEquals(8, loop.advance(1.0))
        // Godot: process_step -= (steps - max) * physics_step  ->  1.0 - 52/60
        assertEquals(1.0 - 52.0 / 60.0, recorder.idleDeltas.single(), 1e-12)
        // The dropped time is not carried over.
        assertEquals(1, loop.advance(1.0 / 60))
    }

    @Test
    fun frameOrderMatchesGodotMainIteration() {
        val recorder = Recorder()
        val loop = GameLoop(GameRuntime(GameRandom(1)), recorder)
        loop.advance(2.0 / 60)
        assertEquals(
            listOf("queries", "physics", "navigation", "step", "queries", "physics", "navigation", "step", "process"),
            recorder.calls,
        )
    }

    @Test
    fun physicsFrameCounterIncrementsAtTickStart() {
        val runtime = GameRuntime(GameRandom(1))
        val seen = mutableListOf<Long>()
        val loop = GameLoop(runtime, object : FrameHooks {
            override fun physicsProcess(delta: Double) { seen += runtime.physicsFrames; assert(runtime.isInPhysicsFrame) }
        })
        repeat(3) { loop.advance(1.0 / 60) }
        assertEquals(listOf(1L, 2L, 3L), seen)
        assertEquals(3L, runtime.processFrames)
    }
}
