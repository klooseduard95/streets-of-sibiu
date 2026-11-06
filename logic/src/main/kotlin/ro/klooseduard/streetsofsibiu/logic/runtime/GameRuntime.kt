package ro.klooseduard.streetsofsibiu.logic.runtime

class GameRuntime(val random: GameRandom = GameRandom()) {
    var paused = false

    var clockMs: () -> Long = { System.nanoTime() / 1_000_000 }

    fun ticksMsec(): Long = clockMs()

    var physicsFrames = 0L
        private set

    var processFrames = 0L
        private set

    var isInPhysicsFrame = false
        private set

    private val sceneTimers = mutableListOf<SceneTreeTimer>()
    private val nodeTimers = mutableListOf<NodeTimer>()
    private val tweens = mutableListOf<Tween>()
    private val deferred = ArrayDeque<() -> Unit>()
    private val deleteQueue = mutableListOf<SceneNode>()

    fun createTimer(seconds: Double, processAlways: Boolean, processInPhysics: Boolean = false): SceneTreeTimer =
        SceneTreeTimer(seconds, processAlways, processInPhysics).also { sceneTimers += it }

    fun createNodeTimer(owner: SceneNode, waitTime: Double = 1.0, oneShot: Boolean = false, autostart: Boolean = false): NodeTimer =
        NodeTimer(owner, waitTime, oneShot, autostart).also { nodeTimers += it }

    fun createTween(bound: SceneNode?): Tween = Tween(bound).also { tweens += it }

    fun callDeferred(action: () -> Unit) {
        deferred.addLast(action)
    }

    fun queueFree(node: SceneNode) {
        if (node !in deleteQueue) deleteQueue += node
    }

    internal fun runPhysicsTick(step: Double, hooks: FrameHooks) {
        isInPhysicsFrame = true
        physicsFrames++
        hooks.flushPhysicsQueries()
        hooks.physicsProcess(step)
        flushDeferred()
        processSceneTimers(step, physicsFrame = true)
        processTweens(step, physicsFrame = true)
        flushDeleteQueue()
        hooks.syncNavigation()
        flushDeferred()
        hooks.stepPhysics(step)
        flushDeferred()
        isInPhysicsFrame = false
    }

    internal fun runIdleFrame(delta: Double, hooks: FrameHooks) {
        flushDeferred()
        hooks.process(delta)
        processNodeTimers(delta)
        flushDeferred()
        processSceneTimers(delta, physicsFrame = false)
        processTweens(delta, physicsFrame = false)
        flushDeleteQueue()
        flushDeferred()
        processFrames++
    }

    fun flushDeferred() {
        while (deferred.isNotEmpty()) deferred.removeFirst().invoke()
    }

    private fun flushDeleteQueue() {
        if (deleteQueue.isEmpty()) return
        val nodes = deleteQueue.toList()
        deleteQueue.clear()
        nodes.forEach { it.free() }
        nodeTimers.removeAll { it.owner.isFreedOrOrphaned }
    }

    private fun processSceneTimers(delta: Double, physicsFrame: Boolean) {
        val snapshot = sceneTimers.toList()
        for (timer in snapshot) {
            if ((paused && !timer.processAlways) || timer.processInPhysics != physicsFrame) continue
            timer.timeLeft -= delta
            if (timer.timeLeft <= 0) {
                timer.fire()
                sceneTimers.remove(timer)
            }
        }
    }

    private fun processNodeTimers(delta: Double) {
        nodeTimers.toList().forEach { it.process(delta, paused) }
    }

    private fun processTweens(delta: Double, physicsFrame: Boolean) {
        val snapshot = tweens.toList()
        for (tween in snapshot) {
            if (!tween.isDead && (!tween.canProcess(paused) || tween.processInPhysics != physicsFrame)) continue
            if (!tween.step(delta)) tweens.remove(tween)
        }
    }

    val activeTweenCount: Int get() = tweens.size
    val activeSceneTimerCount: Int get() = sceneTimers.size
}
