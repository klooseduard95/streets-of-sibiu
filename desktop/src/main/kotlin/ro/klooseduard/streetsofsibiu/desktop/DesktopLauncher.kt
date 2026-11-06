package ro.klooseduard.streetsofsibiu.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame
import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings

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
    val game = StreetsOfSibiuGame(System.getProperty("sos.screen") ?: "menu", System.getProperty("sos.room"), System.getProperty("sos.dash"), System.getProperty("sos.parry"), System.getProperty("sos.items"), System.getProperty("sos.ground"), System.getProperty("sos.enemies"))
    val screenshotPath = System.getProperty("sos.screenshot")
    val listener = if (screenshotPath == null) game else ScreenshotHarness(
        game, screenshotPath, System.getProperty("sos.screenshotFrame")?.toInt() ?: 30, System.getProperty("sos.keys"),
    )
    Lwjgl3Application(listener, config)
}
