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

    private class KeyStep(val frame: Int, val down: Boolean, val key: Int, val touchX: Int = -1, val touchY: Int = -1)

    private fun keyCode(name: String): Int {
        val code = Input.Keys.valueOf(name.lowercase().replaceFirstChar { it.uppercase() }).takeIf { it >= 0 } ?: Input.Keys.valueOf(name.uppercase())
        require(code >= 0) { "Unknown key $name" }
        return code
    }

    private val script = keyScript?.split(',')?.filter { it.isNotBlank() }?.map {
        val parts = it.trim().split(':')
        if (parts[1] == "touch") KeyStep(parts[0].toInt(), true, -2, parts[2].toInt(), parts[3].toInt())
        else if (parts[1] == "untouch") KeyStep(parts[0].toInt(), false, -2, parts[2].toInt(), parts[3].toInt())
        else KeyStep(parts[0].toInt(), parts[1] == "down", keyCode(parts[2]))
    } ?: emptyList()

    override fun render() {
        script.filter { it.frame == rendered }.forEach {
            val processor = Gdx.input.inputProcessor
            if (it.key == -2) {
                if (it.down) processor?.touchDown(it.touchX, it.touchY, 0, Input.Buttons.LEFT) else processor?.touchUp(it.touchX, it.touchY, 0, Input.Buttons.LEFT)
            } else if (it.down) processor?.keyDown(it.key) else processor?.keyUp(it.key)
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
