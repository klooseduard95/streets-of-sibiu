package ro.klooseduard.streetsofsibiu.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import ro.klooseduard.streetsofsibiu.core.audio.AudioBuses
import ro.klooseduard.streetsofsibiu.core.audio.MusicDirector
import ro.klooseduard.streetsofsibiu.core.screens.GameScreen
import ro.klooseduard.streetsofsibiu.core.screens.MainMenuScreen
import ro.klooseduard.streetsofsibiu.core.screens.OptionsScreen
import ro.klooseduard.streetsofsibiu.core.screens.RoomViewerScreen
import ro.klooseduard.streetsofsibiu.core.ui.Fonts
import ro.klooseduard.streetsofsibiu.core.ui.UiKit
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRandom

class StreetsOfSibiuGame(
    private val startScreen: String = "menu",
    private val startRoom: String? = null,
    val devDash: String? = null,
    val devParry: String? = null,
    val devItems: String? = null,
    val devGround: String? = null,
) : Game() {
    lateinit var fonts: Fonts; private set
    lateinit var ui: UiKit; private set
    val buses = AudioBuses()
    lateinit var music: MusicDirector; private set
    private val random = GameRandom()
    private val textures = HashMap<String, Texture>()
    private val sounds = HashMap<String, Sound>()

    var crtEnabled = false

    fun texture(path: String): Texture = textures.getOrPut(path) {
        Texture(Gdx.files.internal(path)).apply { setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) }
    }

    fun playSfx(path: String) {
        sounds.getOrPut(path) { Gdx.audio.newSound(Gdx.files.internal(path)) }.play(buses.sfxLinear.toFloat())
    }

    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        fonts = Fonts()
        ui = UiKit(fonts)
        music = MusicDirector(buses) { random.randiRange(1, 250) }
        music.start()
        when (startScreen) {
            "rooms" -> switchTo(if (startRoom != null) RoomViewerScreen(startRoom) else RoomViewerScreen())
            "game" -> showGame()
            "options" -> showOptions()
            "tutorial" -> switchTo(MainMenuScreen(this).apply { showTutorial() })
            "paused" -> switchTo(GameScreen(this).apply { pauseWhenReady() })
            else -> showMainMenu()
        }
    }

    private fun switchTo(next: Screen) {
        val old = screen
        setScreen(next)
        old?.dispose()
    }

    fun showMainMenu() = switchTo(MainMenuScreen(this))
    fun showOptions() = switchTo(OptionsScreen(this))
    fun showGame() = switchTo(GameScreen(this))

    override fun render() {
        super.render()
        music.treePaused = (screen as? GameScreen)?.isTreePaused ?: false
        music.update(Gdx.graphics.deltaTime)
    }

    override fun pause() {
        super.pause()
        music.pause()
    }

    override fun resume() {
        super.resume()
        music.resume()
    }

    override fun dispose() {
        screen?.dispose()
        textures.values.forEach { it.dispose() }
        sounds.values.forEach { it.dispose() }
        ui.dispose()
        fonts.dispose()
        music.dispose()
    }
}
