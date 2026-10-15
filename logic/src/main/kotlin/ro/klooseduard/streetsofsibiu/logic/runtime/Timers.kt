package ro.klooseduard.streetsofsibiu.logic.runtime

/**
 * Godot `SceneTreeTimer` (`get_tree().create_timer(time, process_always)`).
 *
 * Semantics (scene_tree.cpp `process_timers`): `timeLeft -= delta` every frame of the matching
 * kind (idle unless [processInPhysics]); fires once when `timeLeft <= 0`, then is discarded.
 * Not advanced while the tree is paused unless [processAlways]. Not bound to any node, so it
 * keeps running when the node that created it leaves the tree.
 */
class SceneTreeTimer internal constructor(
    seconds: Double,
    val processAlways: Boolean,
    val processInPhysics: Boolean,
) {
    var timeLeft: Double = seconds
        internal set
    private val listeners = mutableListOf<() -> Unit>()

    /** Godot `timeout.connect(callable)`. */
    fun onTimeout(listener: () -> Unit): SceneTreeTimer {
        listeners += listener
        return this
    }

    internal fun fire() = listeners.toList().forEach { it() }
}

/**
 * Godot `Timer` node (scene/main/timer.cpp), idle process callback.
 *
 * - `start(t)`: if `t > 0` sets [waitTime]; then `timeLeft = waitTime`.
 * - per idle frame while its [owner] can process: `timeLeft -= delta`; when `timeLeft < 0`
 *   (strictly): repeat → `timeLeft += waitTime`, one-shot → stop; then emits timeout.
 * - `stop()`: `timeLeft = -1`. [isStopped] is `timeLeft <= 0`.
 * - [autostart] starts the timer when it is registered (Godot NOTIFICATION_READY).
 */
class NodeTimer internal constructor(
    val owner: SceneNode,
    var waitTime: Double,
    var oneShot: Boolean,
    autostart: Boolean,
) {
    var timeLeft: Double = -1.0
        private set
    private var processing = false
    private val listeners = mutableListOf<() -> Unit>()

    init {
        if (autostart) start()
    }

    fun onTimeout(listener: () -> Unit): NodeTimer {
        listeners += listener
        return this
    }

    fun start(seconds: Double = -1.0) {
        if (seconds > 0) waitTime = seconds
        timeLeft = waitTime
        processing = true
    }

    fun stop() {
        timeLeft = -1.0
        processing = false
    }

    val isStopped: Boolean get() = timeLeft <= 0

    internal fun process(delta: Double, treePaused: Boolean) {
        if (!processing || !owner.canProcess(treePaused)) return
        timeLeft -= delta
        if (timeLeft < 0) {
            if (!oneShot) timeLeft += waitTime else stop()
            listeners.toList().forEach { it() }
        }
    }
}
