package ro.klooseduard.streetsofsibiu.core.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.world.Action

class AimInput(private val actions: ActionInput<Action>, private val viewport: Viewport) : InputAdapter() {
    val aim = Vector2(VIEW_W * 0.75f, VIEW_H / 2)
    private var pointer = -1

    private fun toView(screenX: Int, screenY: Int): Vector2 = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat()))

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        aim.set(toView(screenX, screenY))
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.RIGHT) {
            actions.press(Action.PARRY)
            return true
        }
        val p = toView(screenX, screenY)
        if (this.pointer < 0 && (p.x > VIEW_W / 2 || isMouse())) {
            this.pointer = pointer
            aim.set(p)
            actions.press(Action.SHOOT)
            return true
        }
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer != this.pointer) return false
        aim.set(toView(screenX, screenY))
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.RIGHT) {
            actions.release(Action.PARRY)
            return true
        }
        if (pointer != this.pointer) return false
        this.pointer = -1
        actions.release(Action.SHOOT)
        return true
    }

    fun releaseAll() {
        pointer = -1
        actions.release(Action.PARRY)
        actions.release(Action.SHOOT)
    }

    private fun isMouse() = com.badlogic.gdx.Gdx.app.type != com.badlogic.gdx.Application.ApplicationType.Android

    companion object {
        private const val VIEW_W = 1920f
        private const val VIEW_H = 1080f
    }
}
