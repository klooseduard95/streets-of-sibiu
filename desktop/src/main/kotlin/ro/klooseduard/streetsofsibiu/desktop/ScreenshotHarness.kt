package ro.klooseduard.streetsofsibiu.desktop

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO

class ScreenshotHarness(
    private val game: ApplicationListener,
    private val path: String,
    private val frames: Int,
    keyScript: String? = null,
) : ApplicationListener by game {
    private var rendered = 0

    private class KeyStep(val frame: Int, val down: Boolean, val key: Int)

    private fun keyCode(name: String): Int {
        val code = Input.Keys.valueOf(name.lowercase().replaceFirstChar { it.uppercase() }).takeIf { it >= 0 } ?: Input.Keys.valueOf(name.uppercase())
        require(code >= 0) { "Unknown key $name" }
        return code
    }

    private val script = keyScript?.split(',')?.filter { it.isNotBlank() }?.map {
        val (frame, direction, name) = it.trim().split(':')
        KeyStep(frame.toInt(), direction == "down", keyCode(name))
    } ?: emptyList()

    override fun render() {
        script.filter { it.frame == rendered }.forEach {
            val processor = Gdx.input.inputProcessor
            if (it.down) processor?.keyDown(it.key) else processor?.keyUp(it.key)
        }
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
