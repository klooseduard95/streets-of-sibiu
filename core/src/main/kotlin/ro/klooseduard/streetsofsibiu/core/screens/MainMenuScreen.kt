package ro.klooseduard.streetsofsibiu.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame
import ro.klooseduard.streetsofsibiu.core.ui.Fonts
import ro.klooseduard.streetsofsibiu.core.ui.ShadowedLabel
import ro.klooseduard.streetsofsibiu.core.ui.UiKit
import ro.klooseduard.streetsofsibiu.core.ui.godotBounds
import ro.klooseduard.streetsofsibiu.core.ui.godotCentered

class MainMenuScreen(private val game: StreetsOfSibiuGame) : ScreenAdapter() {
    private val ui = game.ui
    private val stage = Stage(FitViewport(UiKit.VIEW_W, UiKit.VIEW_H))
    private val tutorial = Group()

    init {
        stage.addActor(Image(game.texture("menu.png")).apply { setSize(UiKit.VIEW_W, UiKit.VIEW_H) })

        stage.addActor(
            ShadowedLabel("Streets of Sibiu", game.fonts.get(Fonts.Face.MONTSERRAT, 100, lineSpacing = 3), Color.WHITE, Color.BLACK, 17, 0f, 5f)
                .apply { godotCentered(-412.25f, -422f, 412.25f, -149f) },
        )

        val buttons = Table().apply { godotCentered(-314.5f, -134.75f, 314.5f, 134.75f) }
        val buttonHeight = (269.5f - 2 * 26f) / 3f
        fun menu(text: String, action: () -> Unit) = ui.menuButton(text).apply {
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) = action() })
        }
        buttons.add(menu("Start Game") { tutorial.isVisible = true }).size(629f, buttonHeight).row()
        buttons.add(menu("Options") { game.showOptions() }).size(629f, buttonHeight).padTop(26f).row()
        buttons.add(menu("Exit") { Gdx.app.exit() }).size(629f, buttonHeight).padTop(26f).row()
        stage.addActor(buttons)

        stage.addActor(menu("Cool thingo") { game.crtEnabled = !game.crtEnabled }.apply {
            val w = maxOf(190f, prefWidth)
            godotBounds(1604f, 942f, w, 49f)
        })

        tutorial.setSize(UiKit.VIEW_W, UiKit.VIEW_H)
        tutorial.isVisible = false
        tutorial.addActor(Image(ui.color(Color(0f, 0f, 0f, 0.23529412f))).apply { setSize(UiKit.VIEW_W, UiKit.VIEW_H) })
        tutorial.addActor(Image(ui.color(Color(0f, 0f, 0f, 0.67058825f))).apply { godotCentered(-500f, -300f, 500f, 300f) })
        val box = Table().apply { godotCentered(-480f, -280f, 480f, 280f); top(); defaults().spaceBottom(4f) }
        box.add().height(100f).row()
        box.add(ui.label(TUTORIAL_TEXT, 16).apply { setAlignment(Align.center) }).center().row()
        box.add().height(50f).row()
        box.add(ui.godotDefaultButton("Start", 30).apply {
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) = game.showGame() })
        }).size(150f, 75f).center().row()
        tutorial.addActor(box)
        stage.addActor(tutorial)
    }

    fun showTutorial() {
        tutorial.isVisible = true
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

    override fun dispose() = stage.dispose()

    companion object {
        const val TUTORIAL_TEXT = "TUTORIAL\n\n-WASD to move\n-Left Click to shoot\n-Right Click to Parry once you unlock it\n" +
            "-Space to Dash once you unlock it\n-1,2,3,4,5 Inventory slots \n-E to interact\n-R to reload\n-P to pause"
    }
}
