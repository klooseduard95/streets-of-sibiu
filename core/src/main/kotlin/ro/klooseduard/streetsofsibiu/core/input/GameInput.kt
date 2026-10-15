package ro.klooseduard.streetsofsibiu.core.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.utils.viewport.Viewport
import com.badlogic.gdx.math.Vector2
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.world.Action
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

/** project.godot input map on a keyboard/mouse (desktop, or Android with a keyboard). */
class KeyboardInput(
    private val actions: ActionInput<Action>,
    private val onEscape: () -> Unit,
    private val onSlot: (Int) -> Unit = {},
) : InputAdapter() {
    private val keys = mapOf(
        Input.Keys.W to Action.MOVE_FORWARD, Input.Keys.S to Action.MOVE_BACKWARD,
        Input.Keys.A to Action.MOVE_LEFTWARD, Input.Keys.D to Action.MOVE_RIGHTWARD,
        Input.Keys.SPACE to Action.BLINK, Input.Keys.E to Action.INTERACT, Input.Keys.R to Action.RELOAD,
        Input.Keys.NUM_1 to Action.SLOT_1, Input.Keys.NUM_2 to Action.SLOT_2, Input.Keys.NUM_3 to Action.SLOT_3,
        Input.Keys.NUM_4 to Action.SLOT_4, Input.Keys.NUM_5 to Action.SLOT_5,
    )

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.P || keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
            onEscape()   // pause_menu.gd handles "escape" (P) in _input, immediately; Back/Esc map to it (D3)
            return true
        }
        val action = keys[keycode] ?: return false
        actions.press(action)
        // player.gd _unhandled_input: slot_1..slot_5 select the inventory slot immediately.
        slotIndex(action)?.let(onSlot)
        return true
    }

    private fun slotIndex(action: Action): Int? = when (action) {
        Action.SLOT_1 -> 0
        Action.SLOT_2 -> 1
        Action.SLOT_3 -> 2
        Action.SLOT_4 -> 3
        Action.SLOT_5 -> 4
        else -> null
    }

    override fun keyUp(keycode: Int): Boolean {
        val action = keys[keycode] ?: return false
        actions.release(action)
        return true
    }
}

/**
 * Decision Q1 (D1 scope: movement only): a floating virtual stick on the left half of the screen,
 * quantised to the original's 8 digital directions (WASD combinations), with a dead zone.
 */
class TouchStick(private val actions: ActionInput<Action>, private val viewport: Viewport) : InputAdapter() {
    private var pointer = -1
    private val origin = Vector2()
    private val knob = Vector2()
    private val held = mutableSetOf<Action>()
    val isActive get() = pointer >= 0

    private fun toView(screenX: Int, screenY: Int): Vector2 = viewport.unproject(Vector2(screenX.toFloat(), screenY.toFloat()))

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val p = toView(screenX, screenY)
        if (this.pointer >= 0 || p.x > VIEW_W / 2) return false
        this.pointer = pointer
        origin.set(p)
        knob.set(p)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer != this.pointer) return false
        val p = toView(screenX, screenY)
        val dx = p.x - origin.x
        val dy = p.y - origin.y
        val len = sqrt(dx * dx + dy * dy)
        knob.set(if (len > RADIUS) origin.cpy().add(dx / len * RADIUS, dy / len * RADIUS) else p)
        val wanted = mutableSetOf<Action>()
        if (len > DEAD_ZONE) {
            // y-up view coordinates: sector 0 = right, counter-clockwise in 45° steps.
            val sector = Math.floorMod(floor((atan2(dy, dx) + Math.PI / 8) / (Math.PI / 4)).toInt(), 8)
            when (sector) {
                0 -> wanted += Action.MOVE_RIGHTWARD
                1 -> wanted += listOf(Action.MOVE_RIGHTWARD, Action.MOVE_FORWARD)
                2 -> wanted += Action.MOVE_FORWARD
                3 -> wanted += listOf(Action.MOVE_LEFTWARD, Action.MOVE_FORWARD)
                4 -> wanted += Action.MOVE_LEFTWARD
                5 -> wanted += listOf(Action.MOVE_LEFTWARD, Action.MOVE_BACKWARD)
                6 -> wanted += Action.MOVE_BACKWARD
                7 -> wanted += listOf(Action.MOVE_RIGHTWARD, Action.MOVE_BACKWARD)
            }
        }
        (held - wanted).forEach(actions::release)
        (wanted - held).forEach(actions::press)
        held.clear(); held += wanted
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer != this.pointer) return false
        held.forEach(actions::release)
        held.clear()
        this.pointer = -1
        return true
    }

    fun releaseAll() {
        held.forEach(actions::release)
        held.clear()
        pointer = -1
    }

    /** Draws the stick in view coordinates (y-up stage camera). */
    fun draw(batch: Batch, circle: Texture) {
        if (!isActive) return
        val old = batch.color.cpy()
        batch.color = Color(1f, 1f, 1f, 0.25f)
        batch.draw(circle, origin.x - RADIUS, origin.y - RADIUS, RADIUS * 2, RADIUS * 2)
        batch.color = Color(1f, 1f, 1f, 0.5f)
        batch.draw(circle, knob.x - 50f, knob.y - 50f, 100f, 100f)
        batch.color = old
    }

    companion object {
        private const val VIEW_W = 1920f
        const val RADIUS = 150f
        private const val DEAD_ZONE = 30f
    }
}
