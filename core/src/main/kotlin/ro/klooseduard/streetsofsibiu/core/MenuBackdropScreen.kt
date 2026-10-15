package ro.klooseduard.streetsofsibiu.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings

/**
 * Phase 1 placeholder: shows the original main-menu background (`menu.png`) in a
 * 1920x1080 FitViewport, letterboxed on screens that are not 16:9.
 */
class MenuBackdropScreen : ScreenAdapter() {
    private val worldWidth = GodotProjectSettings.VIEWPORT_WIDTH.toFloat()
    private val worldHeight = GodotProjectSettings.VIEWPORT_HEIGHT.toFloat()

    private val viewport = FitViewport(worldWidth, worldHeight)
    private val batch = SpriteBatch()
    private val background = Texture(Gdx.files.internal("menu.png")).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        batch.draw(background, 0f, 0f, worldWidth, worldHeight)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        background.dispose()
    }
}
