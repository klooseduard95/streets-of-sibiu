package ro.klooseduard.streetsofsibiu.core

import com.badlogic.gdx.Game

/** Application entry point shared by the Android and desktop launchers. */
class StreetsOfSibiuGame : Game() {
    override fun create() {
        setScreen(MenuBackdropScreen())
    }

    override fun dispose() {
        screen?.dispose()
    }
}
