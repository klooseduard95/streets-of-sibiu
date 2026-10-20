package ro.klooseduard.streetsofsibiu.logic.runtime

class ActionInput<A>(private val runtime: GameRuntime) {
    private class State(var pressed: Boolean = false, var pressedPhysicsFrame: Long = -1, var pressedProcessFrame: Long = -1,
                        var releasedPhysicsFrame: Long = -1, var releasedProcessFrame: Long = -1)

    private val states = HashMap<A, State>()

    fun press(action: A) {
        val state = states.getOrPut(action) { State() }
        if (!state.pressed) {
            state.pressedPhysicsFrame = runtime.physicsFrames + 1
            state.pressedProcessFrame = runtime.processFrames
        }
        state.pressed = true
    }

    fun release(action: A) {
        val state = states.getOrPut(action) { State() }
        if (state.pressed) {
            state.releasedPhysicsFrame = runtime.physicsFrames + 1
            state.releasedProcessFrame = runtime.processFrames
        }
        state.pressed = false
    }

    fun releaseAll() = states.keys.toList().forEach(::release)

    fun isPressed(action: A): Boolean = states[action]?.pressed ?: false

    fun isJustPressed(action: A): Boolean {
        val state = states[action] ?: return false
        return if (runtime.isInPhysicsFrame) state.pressedPhysicsFrame == runtime.physicsFrames
        else state.pressedProcessFrame == runtime.processFrames
    }

    fun isJustReleased(action: A): Boolean {
        val state = states[action] ?: return false
        return if (runtime.isInPhysicsFrame) state.releasedPhysicsFrame == runtime.physicsFrames
        else state.releasedProcessFrame == runtime.processFrames
    }
}
