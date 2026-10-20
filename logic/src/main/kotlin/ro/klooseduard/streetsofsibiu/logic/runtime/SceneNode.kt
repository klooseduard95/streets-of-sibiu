package ro.klooseduard.streetsofsibiu.logic.runtime

enum class ProcessMode {
    INHERIT,

    PAUSABLE,

    ALWAYS,

    DISABLED,
}

open class SceneNode(
    var processMode: ProcessMode = ProcessMode.INHERIT,
    parent: SceneNode? = null,
) {
    var parent: SceneNode? = parent
        private set

    private var attached = true

    var isFreed = false
        private set

    private val freeListeners = mutableListOf<() -> Unit>()

    val isInsideTree: Boolean
        get() = !isFreed && attached && (parent?.isInsideTree ?: true)

    fun setAttached(value: Boolean) {
        attached = value
    }

    fun reparent(newParent: SceneNode?) {
        parent = newParent
    }

    val effectiveProcessMode: ProcessMode
        get() = when (processMode) {
            ProcessMode.INHERIT -> parent?.effectiveProcessMode ?: ProcessMode.PAUSABLE
            else -> processMode
        }

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

    fun free() {
        if (isFreed) return
        isFreed = true
        freeListeners.forEach { it() }
        freeListeners.clear()
    }

    val isFreedOrOrphaned: Boolean
        get() = isFreed || (parent?.isFreedOrOrphaned ?: false)
}
