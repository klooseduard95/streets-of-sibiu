package ro.klooseduard.streetsofsibiu.logic.runtime

import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings
import kotlin.math.floor
import kotlin.math.max

/** What the world does at each point of Godot's frame order. All default to no-ops. */
interface FrameHooks {
    /** Emit area enter/exit callbacks computed by the previous [stepPhysics]. */
    fun flushPhysicsQueries() {}

    /** Entities' `_physics_process`. */
    fun physicsProcess(delta: Double) {}

    /** Navigation map synchronisation. */
    fun syncNavigation() {}

    /** Physics server step: move areas, compute overlaps. */
    fun stepPhysics(delta: Double) {}

    /** Entities' `_process`. */
    fun process(delta: Double) {}
}

/**
 * Fixed-step driver equivalent to Godot's `Main::iteration`: 60 physics ticks per second,
 * at most 8 per frame (excess time is dropped and removed from the idle delta, as in Godot),
 * then one idle frame.
 *
 * Difference (D12): Godot's `MainTimerSync` jitter fix, which only smooths *which* rendered
 * frame a tick lands on, is not replicated. The tick rate and the per-tick logic are identical.
 */
class GameLoop(private val runtime: GameRuntime, private val hooks: FrameHooks) {
    private var accumulator = 0.0

    /** Advances by one rendered frame of [frameDelta] seconds; returns the physics ticks run. */
    fun advance(frameDelta: Double): Int {
        accumulator += max(frameDelta, 0.0)
        // Tiny epsilon so exact 1/60 increments are not lost to floating-point rounding.
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
