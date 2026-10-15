package ro.klooseduard.streetsofsibiu.core

import com.badlogic.gdx.Game
import ro.klooseduard.streetsofsibiu.core.screens.RoomViewerScreen

/**
 * Application entry point shared by the Android and desktop launchers.
 * [startScreen]: "menu" (default) or "rooms" (Phase 4 room viewer, a development screen).
 */
class StreetsOfSibiuGame(private val startScreen: String = "menu", private val startRoom: String? = null) : Game() {
    override fun create() {
        setScreen(
            when (startScreen) {
                "rooms" -> if (startRoom != null) RoomViewerScreen(startRoom) else RoomViewerScreen()
                else -> MenuBackdropScreen()
            },
        )
    }

    override fun dispose() {
        screen?.dispose()
    }
}
