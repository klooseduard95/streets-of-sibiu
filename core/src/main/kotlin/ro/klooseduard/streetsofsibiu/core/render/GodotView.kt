package ro.klooseduard.streetsofsibiu.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings
import ro.klooseduard.streetsofsibiu.logic.math.Vec2

/**
 * The original's 1920x1080 viewport in a FitViewport (decision Q2), with a **y-down** camera so
 * world coordinates are identical to Godot's (origin top-left, y grows downwards).
 *
 * The area outside the 16:9 view is black (letterbox/pillarbox, difference D2); inside the view,
 * empty world space shows Godot's default clear colour (0.3, 0.3, 0.3), which the project never
 * overrides (`rendering/environment/defaults/default_clear_color`).
 */
class GodotView {
    val width = GodotProjectSettings.VIEWPORT_WIDTH.toFloat()
    val height = GodotProjectSettings.VIEWPORT_HEIGHT.toFloat()

    val camera = OrthographicCamera().apply { setToOrtho(true, width, height) }
    val viewport = FitViewport(width, height, camera)

    fun resize(screenWidth: Int, screenHeight: Int) {
        viewport.update(screenWidth, screenHeight, true)
    }

    /** Clears the window black, then the view area to Godot's default clear colour. */
    fun clear() {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        viewport.apply()
        val gl = com.badlogic.gdx.Gdx.gl
        gl.glEnable(GL20.GL_SCISSOR_TEST)
        com.badlogic.gdx.graphics.glutils.HdpiUtils.glScissor(viewport.screenX, viewport.screenY, viewport.screenWidth, viewport.screenHeight)
        gl.glClearColor(CLEAR.r, CLEAR.g, CLEAR.b, 1f)
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    /** Places the camera so [center] (world coordinates) is at the middle of the view. */
    fun lookAt(center: Vec2) {
        camera.position.set(center.x, center.y, 0f)
        camera.update()
    }

    /** Screen-space camera for overlays drawn in 1920x1080 view coordinates (y-down). */
    fun useScreenSpace() {
        camera.position.set(width / 2, height / 2, 0f)
        camera.update()
    }

    companion object {
        val CLEAR = Color(0.3f, 0.3f, 0.3f, 1f)
    }
}
