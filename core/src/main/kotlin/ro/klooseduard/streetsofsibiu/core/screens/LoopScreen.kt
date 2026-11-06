package ro.klooseduard.streetsofsibiu.core.screens

import com.badlogic.gdx.ScreenAdapter
import ro.klooseduard.streetsofsibiu.logic.runtime.FrameHooks
import ro.klooseduard.streetsofsibiu.logic.runtime.GameLoop
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime

abstract class LoopScreen : ScreenAdapter(), FrameHooks {
    protected val runtime = GameRuntime()
    private val loop = GameLoop(runtime, this)

    final override fun render(delta: Float) {
        loop.advance(delta.toDouble())
        draw()
    }

    protected abstract fun draw()
}
