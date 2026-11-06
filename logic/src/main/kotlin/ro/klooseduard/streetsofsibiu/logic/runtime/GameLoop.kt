package ro.klooseduard.streetsofsibiu.logic.runtime

import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings
import kotlin.math.floor
import kotlin.math.max

interface FrameHooks {
    fun flushPhysicsQueries() {}

    fun physicsProcess(delta: Double) {}

    fun syncNavigation() {}

    fun stepPhysics(delta: Double) {}

    fun process(delta: Double) {}
}

class GameLoop(private val runtime: GameRuntime, private val hooks: FrameHooks) {
    private var accumulator = 0.0

    fun advance(frameDelta: Double): Int {
        accumulator += max(frameDelta, 0.0)
        var steps = floor(accumulator * TICKS_PER_SECOND + 1e-9).toInt()
        accumulator = max(accumulator - steps * STEP, 0.0)
        var processDelta = max(frameDelta, 0.0)
        if (steps > MAX_STEPS) {
            processDelta -= (steps - MAX_STEPS) * STEP
            steps = MAX_STEPS
        }
        repeat(steps) { runtime.runPhysicsTick(STEP, hooks) }
        runtime.runIdleFrame(processDelta, hooks)
        return steps
    }

    companion object {
        const val TICKS_PER_SECOND = GodotProjectSettings.PHYSICS_TICKS_PER_SECOND
        const val MAX_STEPS = GodotProjectSettings.MAX_PHYSICS_STEPS_PER_FRAME
        const val STEP = 1.0 / TICKS_PER_SECOND
    }
}
