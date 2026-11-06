package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.PlayerDefs
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime

class Ghost(val position: Vec2, val frame: Int, val scale: Double) {
    var alpha = 1.0; private set
    var isFreed = false; private set

    internal fun start(runtime: GameRuntime, onGone: (Ghost) -> Unit) {
        val tween = runtime.createTween(null)
        tween.tweenProperty({ alpha }, { alpha = it }, 0.0, PlayerDefs.GHOST_FADE_TIME)
        tween.onFinished {
            isFreed = true
            onGone(this)
        }
    }
}
