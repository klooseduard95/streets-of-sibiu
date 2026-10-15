package ro.klooseduard.streetsofsibiu.logic.runtime

/** Godot `Node.process_mode` values used by the original project. */
enum class ProcessMode {
    /** Takes the parent's mode; a root node with INHERIT behaves as [PAUSABLE]. */
    INHERIT,

    /** Processes only while the game is not paused. */
    PAUSABLE,

    /** Processes even while paused (pause menu, upgrade window, music, camera). */
    ALWAYS,

    /** Never processes (rooms and the player during a warp fade). */
    DISABLED,
}

/**
 * The part of a Godot node's lifecycle that timers and tweens depend on: whether it is inside
 * the tree, whether it has been freed, and whether it may process given the pause state.
 *
 * Rooms in the original are detached with `remove_child` when inactive; their children keep
 * their state but stop processing. This is modelled by [insideTree] on the room and by the
 * parent chain for everything inside it.
 */
open class SceneNode(
    var processMode: ProcessMode = ProcessMode.INHERIT,
    parent: SceneNode? = null,
) {
    var parent: SceneNode? = parent
        private set

    private var attached = true

    /** True once the node has been freed (Godot `queue_free` flushed). Final. */
    var isFreed = false
        private set

    private val freeListeners = mutableListOf<() -> Unit>()

    /** Godot `is_inside_tree()`: attached itself and every ancestor attached. */
    val isInsideTree: Boolean
        get() = !isFreed && attached && (parent?.isInsideTree ?: true)

    /** Godot `remove_child` / `add_child` on this node (it keeps its state while detached). */
    fun setAttached(value: Boolean) {
        attached = value
    }

    fun reparent(newParent: SceneNode?) {
        parent = newParent
    }

    /** Resolves INHERIT through the parent chain. */
    val effectiveProcessMode: ProcessMode
        get() = when (processMode) {
            ProcessMode.INHERIT -> parent?.effectiveProcessMode ?: ProcessMode.PAUSABLE
            else -> processMode
        }

    /** Godot `Node.can_process()` for a node inside the tree. */
    fun canProcess(treePaused: Boolean): Boolean {
        if (!isInsideTree) return false
        return when (effectiveProcessMode) {
            ProcessMode.ALWAYS -> true
            ProcessMode.DISABLED -> false
            else -> !treePaused
        }
    }

    fun onFreed(listener: () -> Unit) {
        freeListeners += listener
    }

    /** Frees immediately. Prefer [GameRuntime.queueFree], which mirrors Godot's `queue_free`. */
    fun free() {
        if (isFreed) return
        isFreed = true
        freeListeners.forEach { it() }
        freeListeners.clear()
    }

    /** True if this node or any ancestor has been freed. */
    val isFreedOrOrphaned: Boolean
        get() = isFreed || (parent?.isFreedOrOrphaned ?: false)
}
