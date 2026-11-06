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
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import ro.klooseduard.streetsofsibiu.core.StreetsOfSibiuGame
import ro.klooseduard.streetsofsibiu.core.input.AimInput
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
import ro.klooseduard.streetsofsibiu.logic.data.DashType
import ro.klooseduard.streetsofsibiu.logic.data.ParryType
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
import ro.klooseduard.streetsofsibiu.logic.data.Consumables
import ro.klooseduard.streetsofsibiu.logic.data.ItemDef
import ro.klooseduard.streetsofsibiu.logic.data.Weapons
import ro.klooseduard.streetsofsibiu.logic.world.GameItem
import ro.klooseduard.streetsofsibiu.logic.world.Enemy
import ro.klooseduard.streetsofsibiu.logic.world.EnemyProjectile
import ro.klooseduard.streetsofsibiu.logic.world.EnemySystem
import ro.klooseduard.streetsofsibiu.logic.data.EnemyTypes
import ro.klooseduard.streetsofsibiu.logic.data.EnemyType
import ro.klooseduard.streetsofsibiu.logic.nav.RoomNav
import ro.klooseduard.streetsofsibiu.logic.world.Bullet
import ro.klooseduard.streetsofsibiu.logic.world.HeldConsumable
import ro.klooseduard.streetsofsibiu.logic.world.HeldWeapon
import ro.klooseduard.streetsofsibiu.logic.world.Player
import ro.klooseduard.streetsofsibiu.logic.world.PlayerSound
import ro.klooseduard.streetsofsibiu.logic.world.Rgba
import ro.klooseduard.streetsofsibiu.logic.world.RoomCollision

class GameScreen(private val game: StreetsOfSibiuGame) : LoopScreen() {
    private val view = GodotView()
    private val batch = SpriteBatch()
    private val atlas = PaddedTileAtlas(Gdx.files.internal(RoomTileSet.TEXTURE), RoomTileSet.TILE_SIZE, RoomTileSet.ATLAS_COLUMNS, RoomTileSet.ATLAS_ROWS)
    private val roomRenderer = RoomRenderer(atlas)
    private val playerFrames: Array<TextureRegion>
    private val circle: Texture
    private val rangedRegion: TextureRegion
    private val meleeRegions = HashMap<String, TextureRegion>()
    private val itemRegions = HashMap<String, TextureRegion>()

    private val actions = ActionInput<Action>(runtime)
    private val mainBackground = SceneNode()
    private val player = Player(Vec2(996f, 545f), runtime, mainBackground)
    private val camera = CameraRig()
    private val enemies = EnemySystem(runtime, player)
    private val enemyRegions = HashMap<EnemyType, Array<TextureRegion>>()
    private val projectileRegions = HashMap<String, TextureRegion>()
    private var room: RoomDef? = null
    private var world = StaticWorld(emptyList())

    var currentLevel = 0; private set
    private var isInTransition = false
    private var blackAlpha = 1.0

    private val stage = Stage(FitViewport(UiKit.VIEW_W, UiKit.VIEW_H))
    private val blackScreen = Image(game.ui.color(Color.BLACK)).apply { setSize(UiKit.VIEW_W, UiKit.VIEW_H) }
    private val interactLabel = game.ui.label("", 16).apply { setAlignment(com.badlogic.gdx.utils.Align.center); isVisible = false }
    private val hud = HudView(game.ui, game::texture, ::selectSlot)
    private var dashButton: Actor? = null
    private var parryButton: Actor? = null
    private var reloadButton: Actor? = null
    private var interactButton: Actor? = null
    private val settingsView: SettingsView = SettingsView(game.ui, game.texture("menu.png"), game.buses, game.music::applyVolumes) { hideSettings() }
    private val pauseView: PauseView = PauseView(game.ui, onResume = ::resumeFromPause, onSettings = { settingsView.isVisible = true }, onMainMenu = ::toMainMenu)
    private val stick = TouchStick(actions, stage.viewport)
    private val aimInput = AimInput(actions, stage.viewport)

    val isTreePaused: Boolean get() = runtime.paused

    init {
        val sheet = game.texture(PlayerDefs.SPRITE_SHEET)
        val fw = sheet.width / PlayerDefs.SPRITE_HFRAMES
        val fh = sheet.height / PlayerDefs.SPRITE_VFRAMES
        playerFrames = Array(PlayerDefs.SPRITE_HFRAMES * PlayerDefs.SPRITE_VFRAMES) { i ->
            TextureRegion(sheet, (i % PlayerDefs.SPRITE_HFRAMES) * fw, (i / PlayerDefs.SPRITE_HFRAMES) * fh, fw, fh).apply { flip(false, true) }
        }
        rangedRegion = flipped(game.texture(RANGED_TEXTURE))
        circle = Texture(Pixmap(128, 128, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fillCircle(64, 64, 63) })

        stage.addActor(blackScreen)
        stage.addActor(hud)
        stage.addActor(interactLabel)
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
        if (Gdx.app.type == Application.ApplicationType.Android) addActionButtons()
        game.devDash?.let { player.selectedDash = DashType.valueOf(it.uppercase()) }
        game.devParry?.let { player.selectedParry = ParryType.entries.firstOrNull { p -> p.name.equals(it, ignoreCase = true) } }
        player.mousePosition = ::aimWorldPosition
        player.onSound = ::playPlayerSound
        player.onStatsChanged = ::updateHealthUi
        player.onWeaponSound = game::playSfx
        player.bullets.targets = { enemies.bulletTargets }
        enemies.onEnemySound = ::playEnemySound
        player.inventory.onChanged = ::refreshInventory
        game.devItems?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { id ->
            Weapons.all.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) }?.let(player.inventory::addItem)
        }
        refreshInventory()
        updateHealthUi()

        camera.enterTree()
        camera.update(player.position, TICK)
        finishLevel()
    }

    private fun addActionButtons() {
        dashButton = actionButton("DASH", Action.BLINK, 1700f, 740f, 180f)
        parryButton = actionButton("PARRY", Action.PARRY, 1500f, 820f, 150f)
        reloadButton = actionButton("R", Action.RELOAD, 1320f, 840f, 120f)
        interactButton = actionButton("E", Action.INTERACT, 1180f, 840f, 120f)
    }

    private fun actionButton(text: String, action: Action, x: Float, y: Float, size: Float): Actor =
        game.ui.godotDefaultButton(text, 30).also { button ->
            button.godotBounds(x, y, size, size)
            button.addListener(object : InputListener() {
                override fun touchDown(event: InputEvent?, bx: Float, by: Float, pointer: Int, buttonCode: Int): Boolean {
                    actions.press(action)
                    return true
                }

                override fun touchUp(event: InputEvent?, bx: Float, by: Float, pointer: Int, buttonCode: Int) = actions.release(action)
            })
            stage.addActor(button)
        }

    private fun aimWorldPosition(): Vec2 {
        val topLeft = camera.screenTopLeft
        return Vec2(topLeft.x + aimInput.aim.x, topLeft.y + aimInput.aim.y)
    }

    private fun playPlayerSound(sound: PlayerSound) = game.playSfx(
        when (sound) {
            PlayerSound.DASH -> "Music/Dash.mp3"
            PlayerSound.PARRY -> "Music/Parry.mp3"
            PlayerSound.DEATH -> "Music/PlayerDeath.mp3"
            PlayerSound.POTION -> "Music/PotionGulp.mp3"
        },
    )

    private fun finishLevel() {
        currentLevel++
        val tween = runtime.createTween(mainBackground)
        tween.tweenProperty({ blackAlpha }, { blackAlpha = it }, 1.0, 0.25)
        tween.onFinished { generateLevel() }
    }

    private fun generateLevel() {
        isInTransition = true
        val spawn = RoomCatalog.room(RoomCatalog.SPAWN_ROOM)
        runtime.callDeferred {
            room = spawn
            world = RoomCollision.world(spawn)
            player.bullets.world = world
            enemies.attach(world, RoomNav.mesh(spawn))
            spawnDevEnemies(spawn.moduleBox.center)
            camera.setLimitsFromModuleBox(spawn.moduleBox)
            camera.resetSmoothing(player.position, TICK)
            player.body.position = spawn.moduleBox.center
            spawnDevGround(spawn.moduleBox.center)
            isInTransition = false
            val fadeOut = runtime.createTween(mainBackground)
            fadeOut.tweenProperty({ blackAlpha }, { blackAlpha = it }, 0.0, 0.25)
            fadeOut.onFinished { if (pauseRequested) togglePause() }
        }
    }

    private fun spawnDevEnemies(center: Vec2) {
        game.devEnemies?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { entry ->
            val parts = entry.split('@')
            val type = EnemyType.entries.firstOrNull { it.godotName == parts[0] } ?: return@forEach
            val (dx, dy) = (parts.getOrNull(1) ?: "0,0").split(',').map { it.trim().toFloat() }
            enemies.spawn(type, currentLevel, Vec2(center.x + dx, center.y + dy))
        }
    }

    private fun playEnemySound(type: EnemyType, roll: Long) = game.playSfx(
        when (type) {
            EnemyType.RAT -> "Music/Rat.mp3"
            EnemyType.PIGEON -> "Music/Pigeon.mp3"
            else -> if (roll in 0..2) "Music/CeTelefonAi.mp3" else "Music/Grunt.mp3"
        },
    )

    private fun enemyFrames(type: EnemyType): Array<TextureRegion>? {
        val looks = EnemyTypes.looks(type) ?: return null
        return enemyRegions.getOrPut(type) {
            val texture = game.texture(looks.texture)
            val fw = texture.width / looks.hframes
            val fh = texture.height / looks.vframes
            Array(looks.hframes * looks.vframes) { i ->
                TextureRegion(texture, (i % looks.hframes) * fw, (i / looks.hframes) * fh, fw, fh).apply { flip(false, true) }
            }
        }
    }

    private fun drawEnemy(enemy: Enemy) {
        val frames = enemyFrames(enemy.type) ?: return
        val region = frames[enemy.frame.coerceIn(0, frames.size - 1)]
        val s = enemy.sizeModifier.toFloat()
        val w = region.regionWidth * s
        val h = region.regionHeight * s
        batch.setColor(1f, 1f, 1f, enemy.alpha.toFloat())
        batch.draw(region, enemy.position.x - w / 2, enemy.position.y - h / 2, w, h)
        if (enemy.healthBarAlpha > 0 && enemy.maxHp > 0) {
            val fill = (enemy.healthBarValue / enemy.maxHp).coerceIn(0.0, 1.0).toFloat()
            batch.setColor(1f, 0f, 0f, (enemy.healthBarAlpha * enemy.alpha).toFloat())
            batch.draw(game.ui.white, enemy.position.x + HEALTH_BAR_X * s, enemy.position.y + HEALTH_BAR_Y * s, HEALTH_BAR_W * s * fill, HEALTH_BAR_H * s)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawEnemyProjectile(p: EnemyProjectile) {
        val kind = p.params.kind
        val region = projectileRegions.getOrPut(kind.texture) { flipped(game.texture(kind.texture)) }
        val w = region.regionWidth.toFloat()
        val h = region.regionHeight.toFloat()
        batch.draw(region, p.x.toFloat() - w / 2, p.y.toFloat() - h / 2, w / 2, h / 2, w, h, 1f, 1f, Math.toDegrees(p.angle).toFloat() + kind.spriteRotationDeg.toFloat())
    }

    private fun spawnDevGround(center: Vec2) {
        player.interaction.clearItems()
        val all: List<ItemDef> = Weapons.all + Consumables.all
        game.devGround?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { entry ->
            val (id, offset) = entry.split('@').let { it[0] to it.getOrElse(1) { "0,0" } }
            val (dx, dy) = offset.split(',').map { it.trim().toFloat() }
            all.firstOrNull { it.id == id || it.name.equals(id, ignoreCase = true) }
                ?.let { player.interaction.spawn(GameItem(Vec2(center.x + dx, center.y + dy), it)) }
        }
    }

    private fun selectSlot(index: Int) {
        if (runtime.paused) return
        player.inventory.selected = index
    }

    private fun refreshInventory() {
        hud.setInventory(
            player.inventory.items.map { item ->
                when (item) {
                    is HeldWeapon -> item.def?.icon
                    is HeldConsumable -> item.def?.icon
                    null -> null
                }
            },
            player.inventory.selected,
        )
    }

    private fun ammoText(): String? {
        val item = player.heldItem as? HeldWeapon ?: return null
        return when {
            item.ammoCapacity == -1 -> "Ammo: ∞"
            item.isReloading -> "Reloading..."
            else -> "Ammo: %d / %d".format(java.util.Locale.ROOT, item.currentAmmo, item.totalCapacity(player))
        }
    }

    private fun updateHealthUi() {
        hud.setStats(player.hp, player.maxHp, player.shield)
        runtime.createTween(mainBackground).setTrans(TransitionType.SINE).setEase(EaseType.OUT)
            .tweenProperty({ hud.hpBarValue }, { hud.hpBarValue = it }, player.hp, 0.2)
    }

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

    override fun flushPhysicsQueries() {
        if (runtime.paused) return
        player.bullets.flushPhysicsQueries()
        player.interaction.flushPhysicsQueries()
        enemies.flushPhysicsQueries()
    }

    override fun physicsProcess(delta: Double) {
        if (!runtime.paused) {
            val p = player.body.position
            player.body.position = Vec2(
                p.x.coerceIn(camera.limitLeft + 40f, camera.limitRight - 40f),
                p.y.coerceIn(camera.limitTop + 40f, camera.limitBottom - 40f),
            )
            player.physicsProcess(actions, world, delta)
            player.bullets.physicsProcess(delta)
            enemies.physicsProcess(delta)
        }
        camera.update(player.position, delta.toFloat())
    }

    override fun process(delta: Double) {
        if (runtime.paused) return
        player.process(delta)
        enemies.process(delta)
        if (actions.isJustPressed(Action.INTERACT)) player.interaction.interactPressed()
        player.interaction.process()
    }

    override fun draw() {
        view.clear()
        view.lookAt(camera.screenCenter)
        batch.projectionMatrix = view.camera.combined
        batch.begin()
        room?.let { roomRenderer.draw(batch, it, Vec2.ZERO, camera.screenTopLeft, view.width, view.height) }
        for (enemy in enemies.enemies) drawEnemy(enemy)
        for (p in enemies.projectiles) drawEnemyProjectile(p)
        for (item in player.interaction.items) {
            val region = itemRegions.getOrPut(item.def.icon) { flipped(game.texture(item.def.icon)) }
            batch.draw(region, item.x - region.regionWidth / 2f, item.y - region.regionHeight / 2f)
        }
        for (bullet in player.bullets.bullets) drawBullet(bullet)
        for (ghost in player.ghosts) {
            val c = Rgba.GHOST_GREY
            drawPlayerSprite(ghost.frame, ghost.position, ghost.scale, c.r, c.g, c.b, ghost.alpha)
        }
        val m = player.modulate
        drawPlayerSprite(player.animator.frame, player.position, player.spriteScale, m.r, m.g, m.b, m.a)
        batch.end()

        blackScreen.color.a = blackAlpha.toFloat()
        hud.setAmmo(ammoText())
        val inter = player.interaction
        interactLabel.isVisible = inter.labelVisible && !runtime.paused
        if (interactLabel.isVisible) {
            if (interactLabel.text.toString() != inter.labelText) interactLabel.setText(inter.labelText)
            val topLeft = camera.screenTopLeft
            interactLabel.godotBounds(player.position.x - topLeft.x - 150f, player.position.y - topLeft.y + 42f, 300f, 24.7f)
        }
        dashButton?.isVisible = !runtime.paused
        parryButton?.isVisible = !runtime.paused
        reloadButton?.isVisible = !runtime.paused
        interactButton?.isVisible = !runtime.paused
        stage.act(Gdx.graphics.deltaTime)
        stage.viewport.apply()
        stage.draw()
        stage.batch.begin()
        stick.draw(stage.batch, circle)
        stage.batch.end()
    }

    private fun drawBullet(bullet: Bullet) {
        val region = if (bullet.isMelee) {
            meleeRegions.getOrPut(bullet.weaponIcon ?: MELEE_TEXTURE) { flipped(game.texture(bullet.weaponIcon ?: MELEE_TEXTURE)) }
        } else rangedRegion
        val w = region.regionWidth.toFloat()
        val h = region.regionHeight.toFloat()
        val extra = if (bullet.isMelee) 90f else 0f
        batch.draw(region, bullet.x.toFloat() - w / 2, bullet.y.toFloat() - h / 2, w / 2, h / 2, w, h, 1f, 1f, Math.toDegrees(bullet.rotation).toFloat() + extra)
    }

    private fun flipped(texture: Texture) = TextureRegion(texture).apply { flip(false, true) }

    private fun drawPlayerSprite(frameIndex: Int, at: Vec2, scale: Double, r: Double, g: Double, b: Double, a: Double) {
        val frame = playerFrames[frameIndex]
        val w = frame.regionWidth * scale.toFloat()
        val h = frame.regionHeight * scale.toFloat()
        batch.setColor(r.toFloat().coerceIn(0f, 1f), g.toFloat().coerceIn(0f, 1f), b.toFloat().coerceIn(0f, 1f), a.toFloat().coerceIn(0f, 1f))
        batch.draw(frame, at.x - w / 2f, at.y - h / 2f, w, h)
        batch.setColor(1f, 1f, 1f, 1f)
    }

    override fun show() {
        Gdx.input.inputProcessor = InputMultiplexer(stage, KeyboardInput(actions, ::togglePause, ::selectSlot), stick, aimInput)
    }

    override fun hide() {
        stick.releaseAll()
        aimInput.releaseAll()
        actions.releaseAll()
    }

    override fun pause() {
        stick.releaseAll()
        aimInput.releaseAll()
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
        private const val HEALTH_BAR_X = -38f
        private const val HEALTH_BAR_Y = 19f
        private const val HEALTH_BAR_W = 76.5f
        private const val HEALTH_BAR_H = 8.5f
        private const val RANGED_TEXTURE = "item/item_assets/bullet_player.png"
        private const val MELEE_TEXTURE = "item/item_assets/lines.png"
    }
}
