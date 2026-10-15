package ro.klooseduard.streetsofsibiu.core.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame
import ro.klooseduard.streetsofsibiu.core.input.KeyboardInput
import ro.klooseduard.streetsofsibiu.core.input.TouchStick
import ro.klooseduard.streetsofsibiu.core.render.GodotView
import ro.klooseduard.streetsofsibiu.core.render.PaddedTileAtlas
import ro.klooseduard.streetsofsibiu.core.render.RoomRenderer
import ro.klooseduard.streetsofsibiu.core.ui.HudView
import ro.klooseduard.streetsofsibiu.core.ui.PauseView
import ro.klooseduard.streetsofsibiu.core.ui.SettingsView
import ro.klooseduard.streetsofsibiu.core.ui.UiKit
import ro.klooseduard.streetsofsibiu.core.ui.godotBounds
import ro.klooseduard.streetsofsibiu.logic.data.PlayerDefs
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomDef
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomTileSet
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.runtime.EaseType
import ro.klooseduard.streetsofsibiu.logic.runtime.SceneNode
import ro.klooseduard.streetsofsibiu.logic.runtime.TransitionType
import ro.klooseduard.streetsofsibiu.logic.world.Action
import ro.klooseduard.streetsofsibiu.logic.world.CameraRig
import ro.klooseduard.streetsofsibiu.logic.world.Player
import ro.klooseduard.streetsofsibiu.logic.world.RoomCollision

/**
 * MainBackground.tscn at the start of the game (D1 scope): level 1 begins with the black screen,
 * the spawn room is placed, the camera anchors to it and the screen fades in; the player walks
 * with the ported move_and_slide and the room clamp. Level generation, enemies, items and warps
 * follow in later phases (the doors' Preventers block the openings until then).
 */
class GameScreen(private val game: StreetsOfSibiuGame) : LoopScreen() {
    private val view = GodotView()
    private val batch = SpriteBatch()
    private val atlas = PaddedTileAtlas(Gdx.files.internal(RoomTileSet.TEXTURE), RoomTileSet.TILE_SIZE, RoomTileSet.ATLAS_COLUMNS, RoomTileSet.ATLAS_ROWS)
    private val roomRenderer = RoomRenderer(atlas)
    private val playerFrames: Array<TextureRegion>
    private val circle: Texture

    private val actions = ActionInput<Action>(runtime)
    private val mainBackground = SceneNode()          // MainBackground (pausable)
    private val player = Player(Vec2(996f, 545f))     // Player position in MainBackground.tscn
    private val camera = CameraRig()                  // CoolCamera, process mode ALWAYS
    private var room: RoomDef? = null
    private var world = StaticWorld(emptyList())

    var currentLevel = 0; private set
    private var isInTransition = false
    private var blackAlpha = 1.0                     // Overlay/BlackScreen modulate:a (starts opaque)

    private val stage = Stage(FitViewport(UiKit.VIEW_W, UiKit.VIEW_H))
    private val blackScreen = Image(game.ui.color(Color.BLACK)).apply { setSize(UiKit.VIEW_W, UiKit.VIEW_H) }
    private val hud = HudView(game.ui, game::texture, ::selectSlot)
    private var selectedSlot = 0
    private val settingsView: SettingsView = SettingsView(game.ui, game.texture("menu.png"), game.buses, game.music::applyVolumes) { hideSettings() }
    private val pauseView: PauseView = PauseView(game.ui, onResume = ::resumeFromPause, onSettings = { settingsView.isVisible = true }, onMainMenu = ::toMainMenu)
    private val stick = TouchStick(actions, stage.viewport)

    val isTreePaused: Boolean get() = runtime.paused

    init {
        val sheet = game.texture(PlayerDefs.SPRITE_SHEET)
        val fw = sheet.width / PlayerDefs.SPRITE_HFRAMES
        val fh = sheet.height / PlayerDefs.SPRITE_VFRAMES
        playerFrames = Array(PlayerDefs.SPRITE_HFRAMES * PlayerDefs.SPRITE_VFRAMES) { i ->
            TextureRegion(sheet, (i % PlayerDefs.SPRITE_HFRAMES) * fw, (i / PlayerDefs.SPRITE_HFRAMES) * fh, fw, fh).apply { flip(false, true) }
        }
        circle = Texture(Pixmap(128, 128, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fillCircle(64, 64, 63) })

        stage.addActor(blackScreen)      // CanvasLayer order: Overlay before the HUD layers
        stage.addActor(hud)
        pauseView.isVisible = false
        settingsView.isVisible = false
        stage.addActor(pauseView)
        stage.addActor(settingsView)
        if (Gdx.app.type == Application.ApplicationType.Android) {
            stage.addActor(game.ui.godotDefaultButton("II", 32).apply {
                godotBounds(1820f, 20f, 80f, 80f)
                addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) = togglePause() })
            })
        }
        hud.setInventory(PlayerDefs.INITIAL_INVENTORY, selected = 0)
        updateHealthUi()

        // Camera enters the tree with the player, then MainBackground._ready() starts level 1.
        camera.enterTree()
        camera.update(player.position, TICK)
        finishLevel()
    }

    /** main_background.gd finishLevel(): black fade-in, build the level, fade out. */
    private fun finishLevel() {
        currentLevel++
        val tween = runtime.createTween(mainBackground)
        tween.tweenProperty({ blackAlpha }, { blackAlpha = it }, 1.0, 0.25)
        tween.onFinished { generateLevel() }
    }

    /** main_background.gd generateLevel() for D1: the spawn room only (generation is Phase 10). */
    private fun generateLevel() {
        isInTransition = true
        val spawn = RoomCatalog.room(RoomCatalog.SPAWN_ROOM)
        // add_child.call_deferred(activeRoom); await activeRoom.ready
        runtime.callDeferred {
            room = spawn
            world = RoomCollision.world(spawn)
            camera.setLimitsFromModuleBox(spawn.moduleBox)       // updateCameraAnchor
            camera.resetSmoothing(player.position, TICK)
            player.body.position = spawn.moduleBox.center        // getRoomCenter()
            isInTransition = false
            val fadeOut = runtime.createTween(mainBackground)
            fadeOut.tweenProperty({ blackAlpha }, { blackAlpha = it }, 0.0, 0.25)
            fadeOut.onFinished { if (pauseRequested) togglePause() }
        }
    }

    /** InventoryComponent.selected_slot_index setter (equip/unequip has no effect in D1: no weapons yet). */
    private fun selectSlot(index: Int) {
        if (runtime.paused) return
        selectedSlot = index
        hud.setInventory(PlayerDefs.INITIAL_INVENTORY, selectedSlot)
    }

    private fun updateHealthUi() {
        hud.setStats(player.hp, player.maxHp, player.shield)
        // information_ui.gd: tween ProgressBar.value to hp over 0.2 s, sine, ease out.
        runtime.createTween(mainBackground).setTrans(TransitionType.SINE).setEase(EaseType.OUT)
            .tweenProperty({ hud.hpBarValue }, { hud.hpBarValue = it }, player.hp, 0.2)
    }

    // ------------------------------------------------------------------ pause (intended pause_menu.gd, D-B1)

    private fun togglePause() {
        if (isInTransition) return
        if (pauseView.isVisible) {
            resumeFromPause()
        } else if (!runtime.paused) {
            pauseView.setStats(currentLevel, player.hp, player.maxHp, player.shield)
            pauseView.isVisible = true
            runtime.paused = true
        }
    }

    private var pauseRequested = false

    /** Development option: open the pause menu as soon as the level-start transition is over. */
    fun pauseWhenReady() {
        pauseRequested = true
    }

    private fun hideSettings() {
        settingsView.isVisible = false
    }

    private fun resumeFromPause() {
        settingsView.isVisible = false
        pauseView.isVisible = false
        runtime.paused = false
    }

    private fun toMainMenu() {
        runtime.paused = false
        game.showMainMenu()
    }

    // ------------------------------------------------------------------ frame hooks

    override fun physicsProcess(delta: Double) {
        // MainBackground._physics_process (parent, runs before the player): clamp to camera limits ± 40.
        if (!runtime.paused) {
            val p = player.body.position
            player.body.position = Vec2(
                p.x.coerceIn(camera.limitLeft + 40f, camera.limitRight - 40f),
                p.y.coerceIn(camera.limitTop + 40f, camera.limitBottom - 40f),
            )
            player.physicsProcess(actions, world, delta)
        }
        camera.update(player.position, delta.toFloat())   // ALWAYS: keeps smoothing while paused
    }

    override fun process(delta: Double) {
        if (!runtime.paused) player.process(delta)
    }

    override fun draw() {
        view.clear()
        view.lookAt(camera.screenCenter)
        batch.projectionMatrix = view.camera.combined
        batch.begin()
        room?.let { roomRenderer.draw(batch, it, Vec2.ZERO, camera.screenTopLeft, view.width, view.height) }
        val frame = playerFrames[player.animator.frame]
        batch.draw(frame, player.position.x - frame.regionWidth / 2f, player.position.y - frame.regionHeight / 2f)
        batch.end()

        blackScreen.color.a = blackAlpha.toFloat()
        stage.act(Gdx.graphics.deltaTime)
        stage.viewport.apply()
        stage.draw()
        stage.batch.begin()
        stick.draw(stage.batch, circle)
        stage.batch.end()
    }

    override fun show() {
        Gdx.input.inputProcessor = InputMultiplexer(stage, KeyboardInput(actions, ::togglePause, ::selectSlot), stick)
    }

    override fun hide() {
        stick.releaseAll()
        actions.releaseAll()
    }

    /** Android backgrounding (D3): open the pause menu so the game does not run on unattended. */
    override fun pause() {
        stick.releaseAll()
        actions.releaseAll()
        if (!runtime.paused && !isInTransition) togglePause()
    }

    override fun resize(width: Int, height: Int) {
        view.resize(width, height)
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        batch.dispose()
        atlas.dispose()
        circle.dispose()
        stage.dispose()
        settingsView.disposeTextures()
    }

    companion object {
        private const val TICK = (1.0 / 60).toFloat()
    }
}
