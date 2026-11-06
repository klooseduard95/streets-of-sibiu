package ro.klooseduard.streetsofsibiu.logic.runtime

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class TransitionType { LINEAR, SINE }

enum class EaseType { IN, OUT, IN_OUT, OUT_IN }

enum class TweenPauseMode { BOUND, STOP, PROCESS }

object Easing {
    fun run(trans: TransitionType, ease: EaseType, time: Float, initial: Float, delta: Float, duration: Float): Float {
        if (duration == 0f) return initial + delta
        return when (trans) {
            TransitionType.LINEAR -> delta * time / duration + initial
            TransitionType.SINE -> when (ease) {
                EaseType.IN -> sineIn(time, initial, delta, duration)
                EaseType.OUT -> sineOut(time, initial, delta, duration)
                EaseType.IN_OUT -> (-delta / 2 * (cos(PI * time / duration) - 1) + initial).toFloat()
                EaseType.OUT_IN -> if (time < duration / 2) {
                    sineOut(time * 2, initial, delta / 2, duration)
                } else {
                    val half = delta / 2
                    sineIn(time * 2 - duration, initial + half, half, duration)
                }
            }
        }
    }

    private fun sineIn(t: Float, b: Float, c: Float, d: Float): Float =
        (-c * cos(t / d * (PI / 2)) + c + b).toFloat()

    private fun sineOut(t: Float, b: Float, c: Float, d: Float): Float =
        (c * sin(t / d * (PI / 2)) + b).toFloat()

    fun interpolate(initial: Double, delta: Double, time: Double, duration: Double, trans: TransitionType, ease: EaseType): Double {
        val t = run(trans, ease, time.toFloat(), 0f, 1f, duration.toFloat())
        return initial + delta * t
    }
}

class Tween internal constructor(private val bound: SceneNode?) {
    var processInPhysics = false
    var pauseMode = TweenPauseMode.BOUND
    private var defaultTransition = TransitionType.LINEAR
    private var defaultEase = EaseType.IN_OUT
    private var parallelNext = false
    private var loops = 1
    private var loopsDone = 0
    private val steps = mutableListOf<MutableList<PropertyTweener<*>>>()
    private var currentStep = -1
    private var started = false
    private var running = true

    var isDead = false
        private set
    private val finishedListeners = mutableListOf<() -> Unit>()

    fun setTrans(trans: TransitionType): Tween = apply { defaultTransition = trans }
    fun setEase(ease: EaseType): Tween = apply { defaultEase = ease }

    fun parallel(): Tween = apply { parallelNext = true }

    fun chain(): Tween = apply { parallelNext = false }

    fun setLoops(count: Int = 0): Tween = apply { loops = count }

    fun onFinished(listener: () -> Unit): Tween = apply { finishedListeners += listener }

    fun kill() {
        running = false
        isDead = true
    }

    fun <V> tweenProperty(
        get: () -> V,
        set: (V) -> Unit,
        finalValue: V,
        duration: Double,
        lerp: (from: V, to: V, weight: Float) -> V,
    ): PropertyTweener<V> {
        check(!started) { "Can't append to a Tween that has started" }
        val tweener = PropertyTweener(get, set, finalValue, duration, lerp, defaultTransition, defaultEase)
        if (parallelNext) {
            currentStep = maxOf(currentStep, 0)
        } else {
            currentStep++
        }
        parallelNext = false
        while (steps.size <= currentStep) steps += mutableListOf<PropertyTweener<*>>()
        steps[currentStep] += tweener
        return tweener
    }

    fun tweenProperty(get: () -> Double, set: (Double) -> Unit, finalValue: Double, duration: Double): PropertyTweener<Double> =
        tweenProperty(get, set, finalValue, duration) { a, b, t -> a + (b - a) * t }

    internal fun canProcess(treePaused: Boolean): Boolean {
        if (bound != null && pauseMode == TweenPauseMode.BOUND && !bound.isFreedOrOrphaned) {
            return bound.isInsideTree && bound.canProcess(treePaused)
        }
        return !treePaused || pauseMode == TweenPauseMode.PROCESS
    }

    internal fun step(delta: Double): Boolean {
        if (isDead) return false
        if (bound != null) {
            if (bound.isFreedOrOrphaned) return false
            if (!bound.isInsideTree) return true
        }
        if (!running) return true
        if (!started) {
            if (steps.isEmpty()) {
                isDead = true
                return false
            }
            currentStep = 0
            loopsDone = 0
            startStep()
            started = true
        }
        var remaining = delta
        while (running && remaining > 0) {
            var stepDelta = remaining
            var stepActive = false
            for (tweener in steps[currentStep]) {
                val result = tweener.step(remaining)
                stepActive = result.active || stepActive
                stepDelta = min(result.remainingDelta, stepDelta)
            }
            remaining = stepDelta
            if (!stepActive) {
                currentStep++
                if (currentStep == steps.size) {
                    loopsDone++
                    if (loopsDone == loops) {
                        running = false
                        isDead = true
                        finishedListeners.toList().forEach { it() }
                        break
                    } else {
                        currentStep = 0
                        startStep()
                    }
                } else {
                    startStep()
                }
            }
        }
        return true
    }

    private fun startStep() = steps[currentStep].forEach { it.start() }
}

internal data class TweenerStep(val active: Boolean, val remainingDelta: Double)

class PropertyTweener<V> internal constructor(
    private val get: () -> V,
    private val set: (V) -> Unit,
    private val finalValue: V,
    private val duration: Double,
    private val lerp: (V, V, Float) -> V,
    private var trans: TransitionType,
    private var ease: EaseType,
) {
    private var initialValue: V? = null
    private var elapsed = 0.0
    private var finished = false

    fun setTrans(value: TransitionType): PropertyTweener<V> = apply { trans = value }
    fun setEase(value: EaseType): PropertyTweener<V> = apply { ease = value }

    internal fun start() {
        elapsed = 0.0
        finished = false
        initialValue = get()
    }

    @Suppress("UNCHECKED_CAST")
    internal fun step(delta: Double): TweenerStep {
        if (finished) return TweenerStep(false, delta)
        elapsed += delta
        val time = min(elapsed, duration)
        return if (time < duration) {
            val weight = Easing.run(trans, ease, time.toFloat(), 0f, 1f, duration.toFloat())
            set(lerp(initialValue as V, finalValue, weight))
            TweenerStep(true, 0.0)
        } else {
            set(finalValue)
            finished = true
            TweenerStep(false, elapsed - duration)
        }
    }
}
