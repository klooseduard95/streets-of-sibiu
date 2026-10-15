package ro.klooseduard.streetsofsibiu.logic.runtime

/**
 * The parts of Godot's `SceneTree` and `MessageQueue` the game logic relies on:
 * pause state, frame counters, scene timers, node timers, tweens, deferred calls and
 * `queue_free`. Driven by [GameLoop] in Godot's frame order.
 */
class GameRuntime(val random: GameRandom = GameRandom()) {
    /** Godot `get_tree().paused`. */
    var paused = false

    /** Godot `Engine.get_physics_frames()`; incremented at the start of each physics tick. */
    var physicsFrames = 0L
        private set

    /** Godot `Engine.get_process_frames()`; incremented at the end of each idle frame. */
    var processFrames = 0L
        private set

    /** Godot `Engine.is_in_physics_frame()`. */
    var isInPhysicsFrame = false
        private set

    private val sceneTimers = mutableListOf<SceneTreeTimer>()
    private val nodeTimers = mutableListOf<NodeTimer>()
    private val tweens = mutableListOf<Tween>()
    private val deferred = ArrayDeque<() -> Unit>()
    private val deleteQueue = mutableListOf<SceneNode>()

    /** Godot `get_tree().create_timer(seconds, process_always, process_in_physics)`. */
    fun createTimer(seconds: Double, processAlways: Boolean, processInPhysics: Boolean = false): SceneTreeTimer =
        SceneTreeTimer(seconds, processAlways, processInPhysics).also { sceneTimers += it }

    /** A `Timer` node owned by [owner] (registered = entered the tree). */
    fun createNodeTimer(owner: SceneNode, waitTime: Double = 1.0, oneShot: Boolean = false, autostart: Boolean = false): NodeTimer =
        NodeTimer(owner, waitTime, oneShot, autostart).also { nodeTimers += it }

    /** Godot `node.create_tween()` ([bound] = node) or `get_tree().create_tween()` (null). */
    fun createTween(bound: SceneNode?): Tween = Tween(bound).also { tweens += it }

    /** Godot `call_deferred`: runs at the next message-queue flush. */
    fun callDeferred(action: () -> Unit) {
        deferred.addLast(action)
    }

    /** Godot `queue_free`: the node is freed at the next delete-queue flush. */
    fun queueFree(node: SceneNode) {
        if (node !in deleteQueue) deleteQueue += node
    }

    /** One physics tick in the order of Godot 4.6.1 `Main::iteration` + `SceneTree::physics_process`. */
    internal fun runPhysicsTick(step: Double, hooks: FrameHooks) {
        isInPhysicsFrame = true
        physicsFrames++
        hooks.flushPhysicsQueries()          // area enter/exit callbacks from the previous step
        hooks.physicsProcess(step)           // nodes' _physics_process
        flushDeferred()
        processSceneTimers(step, physicsFrame = true)
        processTweens(step, physicsFrame = true)
        flushDeleteQueue()
        hooks.syncNavigation()
        flushDeferred()
        hooks.stepPhysics(step)              // integrate, compute overlaps for the next flush
        flushDeferred()
        isInPhysicsFrame = false
    }

    /** One idle frame in the order of Godot 4.6.1 `SceneTree::process`. */
    internal fun runIdleFrame(delta: Double, hooks: FrameHooks) {
        flushDeferred()
        hooks.process(delta)                 // nodes' _process
        processNodeTimers(delta)             // Timer nodes (idle callback)
        flushDeferred()
        processSceneTimers(delta, physicsFrame = false)
        processTweens(delta, physicsFrame = false)
        flushDeleteQueue()
        flushDeferred()
        processFrames++
    }

    /** Runs queued calls in FIFO order, including calls queued during the flush (Godot behaviour). */
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

    /** Timers created while iterating are skipped until the next pass (scene_tree.cpp). */
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

    /** For tests and diagnostics. */
    val activeTweenCount: Int get() = tweens.size
    val activeSceneTimerCount: Int get() = sceneTimers.size
}
