package ro.klooseduard.streetsofsibiu.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings
import ro.klooseduard.streetsofsibiu.logic.math.Vec2

class GodotView {
    val width = GodotProjectSettings.VIEWPORT_WIDTH.toFloat()
    val height = GodotProjectSettings.VIEWPORT_HEIGHT.toFloat()

    val camera = OrthographicCamera().apply { setToOrtho(true, width, height) }
    val viewport = FitViewport(width, height, camera)

    fun resize(screenWidth: Int, screenHeight: Int) {
        viewport.update(screenWidth, screenHeight, true)
    }

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

    fun lookAt(center: Vec2) {
        camera.position.set(center.x, center.y, 0f)
        camera.update()
    }

    fun useScreenSpace() {
        camera.position.set(width / 2, height / 2, 0f)
        camera.update()
    }

    companion object {
        val CLEAR = Color(0.3f, 0.3f, 0.3f, 1f)
    }
}
