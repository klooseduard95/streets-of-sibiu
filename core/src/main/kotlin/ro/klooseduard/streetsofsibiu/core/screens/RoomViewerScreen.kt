package ro.klooseduard.streetsofsibiu.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ro.klooseduard.streetsofsibiu.core.render.GodotView
import ro.klooseduard.streetsofsibiu.core.render.PaddedTileAtlas
import ro.klooseduard.streetsofsibiu.core.render.RoomRenderer
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomDef
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomTileSet
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.world.CameraRig

class RoomViewerScreen(startRoom: String = RoomCatalog.SPAWN_ROOM) : LoopScreen() {
    private val view = GodotView()
    private val batch = SpriteBatch()
    private val tileset = PaddedTileAtlas(
        Gdx.files.internal(RoomTileSet.TEXTURE), RoomTileSet.TILE_SIZE, RoomTileSet.ATLAS_COLUMNS, RoomTileSet.ATLAS_ROWS,
    )
    private val renderer = RoomRenderer(tileset)
    private val font = BitmapFont(true).apply { data.setScale(2f) }
    private val rig = CameraRig()

    private var roomIndex = RoomCatalog.all.indexOfFirst { it.id == startRoom }.coerceAtLeast(0)
    val room: RoomDef get() = RoomCatalog.all[roomIndex]

    var target = Vec2.ZERO
    private var dragX = 0f
    private var dragY = 0f
    private var dragged = false

    init {
        target = room.moduleBox.center
        rig.enterTree()
        rig.update(target, TICK)
        showRoom(roomIndex)
    }

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.N, Input.Keys.RIGHT_BRACKET -> showRoom(roomIndex + 1)
                    Input.Keys.P, Input.Keys.LEFT_BRACKET -> showRoom(roomIndex - 1)
                    else -> return false
                }
                return true
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                dragX = screenX.toFloat(); dragY = screenY.toFloat(); dragged = false
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                val scale = view.width / view.viewport.screenWidth.coerceAtLeast(1)
                target = Vec2(target.x - (screenX - dragX) * scale, target.y - (screenY - dragY) * scale)
                if (kotlin.math.abs(screenX - dragX) + kotlin.math.abs(screenY - dragY) > 4) dragged = true
                dragX = screenX.toFloat(); dragY = screenY.toFloat()
                return true
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (!dragged) {
                    val w = Gdx.graphics.width
                    if (screenX < w * 0.15f) showRoom(roomIndex - 1) else if (screenX > w * 0.85f) showRoom(roomIndex + 1)
                }
                return true
            }
        }
    }

    fun showRoom(index: Int, focus: Vec2? = null) {
        roomIndex = Math.floorMod(index, RoomCatalog.all.size)
        rig.setLimitsFromModuleBox(room.moduleBox)
        target = focus ?: room.moduleBox.center
        rig.resetSmoothing(target, TICK)
    }

    fun showRoom(id: String, focus: Vec2? = null) = showRoom(RoomCatalog.all.indexOfFirst { it.id == id }, focus)

    override fun physicsProcess(delta: Double) {
        val step = (PAN_SPEED * delta).toFloat()
        var dx = 0f
        var dy = 0f
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) dx -= step
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) dx += step
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) dy -= step
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) dy += step
        if (dx != 0f || dy != 0f) target = Vec2(target.x + dx, target.y + dy)
        rig.update(target, delta.toFloat())
    }

    var showOverlay = true

    override fun draw() {
        view.clear()
        view.lookAt(rig.screenCenter)
        batch.projectionMatrix = view.camera.combined
        batch.begin()
        renderer.draw(batch, room, Vec2.ZERO, rig.screenTopLeft, view.width, view.height)
        batch.end()
        if (showOverlay) {
            view.useScreenSpace()
            batch.projectionMatrix = view.camera.combined
            batch.begin()
            font.draw(batch, "${roomIndex + 1}/${RoomCatalog.all.size}  ${room.id}   (N/P or tap edges: room, arrows/drag: move)", 20f, 20f)
            batch.end()
        }
    }

    override fun resize(width: Int, height: Int) = view.resize(width, height)

    override fun dispose() {
        batch.dispose()
        tileset.dispose()
        font.dispose()
    }

    companion object {
        private const val TICK = (1.0 / 60).toFloat()
        private const val PAN_SPEED = 1500.0
    }
}
