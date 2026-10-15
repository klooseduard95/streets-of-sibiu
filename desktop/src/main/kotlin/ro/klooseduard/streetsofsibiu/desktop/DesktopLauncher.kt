package ro.klooseduard.streetsofsibiu.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame
import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings

/**
 * Optional system properties (development only):
 *  - `sos.window=WIDTHxHEIGHT` window size (default: half of 1920x1080).
 *  - `sos.screenshot=/path/out.png` save a screenshot after `sos.screenshotFrame` frames (default 30), then exit.
 */
fun main() {
    val (width, height) = System.getProperty("sos.window")
        ?.split('x')?.map { it.trim().toInt() }
        ?: listOf(GodotProjectSettings.VIEWPORT_WIDTH / 2, GodotProjectSettings.VIEWPORT_HEIGHT / 2)
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Streets of Sibiu (desktop dev build)")
        setWindowedMode(width, height)
        useVsync(true)
        setForegroundFPS(60)
    }
    val game = StreetsOfSibiuGame()
    val screenshotPath = System.getProperty("sos.screenshot")
    val listener = if (screenshotPath == null) game else ScreenshotHarness(
        game, screenshotPath, System.getProperty("sos.screenshotFrame")?.toInt() ?: 30,
    )
    Lwjgl3Application(listener, config)
}
