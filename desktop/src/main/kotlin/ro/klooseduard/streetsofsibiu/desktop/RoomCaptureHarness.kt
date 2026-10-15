package ro.klooseduard.streetsofsibiu.desktop

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import ro.klooseduard.streetsofsibiu.core.screens.RoomViewerScreen
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.math.Vec2

/**
 * Development tool: renders every room with the camera snapped to (a) the module-box centre and
 * (b) the room origin (clamped by the limits), writes `<dir>/<ROOM>_<centre|corner>.png`, exits.
 * Compared with the Godot captures by tools/compare_room_screens.py.
 */
class RoomCaptureHarness(private val dir: String) : ApplicationAdapter() {
    private lateinit var viewer: RoomViewerScreen
    private val jobs = RoomCatalog.all.flatMap { room -> listOf(room.id to "centre", room.id to "corner") }
    private var index = 0
    private var framesOnJob = 0

    override fun create() {
        viewer = RoomViewerScreen()
        viewer.showOverlay = false
        viewer.resize(Gdx.graphics.width, Gdx.graphics.height)
        startJob()
    }

    private fun startJob() {
        val (id, view) = jobs[index]
        val focus = if (view == "centre") RoomCatalog.room(id).moduleBox.center else Vec2(0f, 0f)
        viewer.showRoom(id, focus)
        framesOnJob = 0
    }

    override fun resize(width: Int, height: Int) = viewer.resize(width, height)

    override fun render() {
        // One physics tick per frame: like Godot, reset_smoothing() only shows on the next camera update.
        viewer.render(1f / 60f)
        if (++framesOnJob == 3) {
            val (id, view) = jobs[index]
            val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
            PixmapIO.writePNG(Gdx.files.absolute("$dir/${id}_$view.png"), pixmap, -1, true)
            pixmap.dispose()
            if (++index == jobs.size) {
                Gdx.app.log("RoomCaptureHarness", "wrote ${jobs.size} captures to $dir")
                Gdx.app.exit()
            } else {
                startJob()
            }
        }
    }
}
