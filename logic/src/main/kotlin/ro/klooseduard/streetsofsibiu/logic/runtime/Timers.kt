package ro.klooseduard.streetsofsibiu.logic.runtime

class SceneTreeTimer internal constructor(
    seconds: Double,
    val processAlways: Boolean,
    val processInPhysics: Boolean,
) {
    var timeLeft: Double = seconds
        internal set
    private val listeners = mutableListOf<() -> Unit>()

    fun onTimeout(listener: () -> Unit): SceneTreeTimer {
        listeners += listener
        return this
    }

    internal fun fire() = listeners.toList().forEach { it() }
}

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
