package ro.klooseduard.streetsofsibiu.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame
import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings

/**
 * Optional system properties (development only), passed as Gradle properties to `:desktop:run`:
 *  - `sos.screen=menu|rooms` start screen; `sos.room=<ROOM_ID>` start room for the room viewer.
 *  - `sos.window=WIDTHxHEIGHT` window size (default: half of 1920x1080).
 *  - `sos.screenshot=/path/out.png` save a screenshot after `sos.screenshotFrame` frames (default 30), then exit.
 *  - `sos.captureRooms=/dir` render every room (centre and top-left views) to PNG files, then exit.
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
    val captureDir = System.getProperty("sos.captureRooms")
    if (captureDir != null) {
        Lwjgl3Application(RoomCaptureHarness(captureDir), config)
        return
    }
    val game = StreetsOfSibiuGame(System.getProperty("sos.screen") ?: "menu", System.getProperty("sos.room"))
    val screenshotPath = System.getProperty("sos.screenshot")
    val listener = if (screenshotPath == null) game else ScreenshotHarness(
        game, screenshotPath, System.getProperty("sos.screenshotFrame")?.toInt() ?: 30,
    )
    Lwjgl3Application(listener, config)
}
