package ro.klooseduard.streetsofsibiu.desktop

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO

class ScreenshotHarness(
    private val game: ApplicationListener,
    private val path: String,
    private val frames: Int,
) : ApplicationListener by game {
    private var rendered = 0

    override fun render() {
        game.render()
        if (++rendered == frames) {
            val width = Gdx.graphics.backBufferWidth
            val height = Gdx.graphics.backBufferHeight
            val pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height)
            PixmapIO.writePNG(Gdx.files.absolute(path), pixmap, -1, true)
            pixmap.dispose()
            Gdx.app.log("ScreenshotHarness", "wrote $path (${width}x$height)")
            Gdx.app.exit()
        }
    }
}
