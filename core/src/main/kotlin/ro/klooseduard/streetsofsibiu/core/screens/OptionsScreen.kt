package ro.klooseduard.streetsofsibiu.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame
import ro.klooseduard.streetsofsibiu.core.ui.SettingsView
import ro.klooseduard.streetsofsibiu.core.ui.UiKit

/** Options from the main menu: Settings.tscn as its own scene; Go Back returns to the main menu. */
class OptionsScreen(private val game: StreetsOfSibiuGame) : ScreenAdapter() {
    private val stage = Stage(FitViewport(UiKit.VIEW_W, UiKit.VIEW_H))
    private val view = SettingsView(game.ui, game.texture("menu.png"), game.buses, game.music::applyVolumes) { game.showMainMenu() }

    init {
        stage.addActor(view)
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        stage.act(delta)
        stage.viewport.apply()
        stage.draw()
    }

    override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)

    override fun dispose() {
        stage.dispose()
        view.disposeTextures()
    }
}
