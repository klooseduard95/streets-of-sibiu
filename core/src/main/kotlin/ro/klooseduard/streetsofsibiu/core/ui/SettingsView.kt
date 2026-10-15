package ro.klooseduard.streetsofsibiu.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import ro.klooseduard.streetsofsibiu.core.audio.AudioBuses
import kotlin.math.roundToInt

/**
 * scenes/Settings.tscn: full-screen menu.png background, " Go Back" button, a default-theme panel
 * with "Optons" (sic) and three bus sliders (h_slider.gd: value = db_to_linear(bus dB), step 0.05;
 * changing it sets bus dB = linear_to_db(value)). Volumes are not saved, as in the original.
 */
class SettingsView(ui: UiKit, background: Texture, private val buses: AudioBuses, onChanged: () -> Unit, onBack: () -> Unit) : Group() {
    private val grabber = Texture(Pixmap(16, 16, Pixmap.Format.RGBA8888).apply {
        blending = Pixmap.Blending.None
        for (y in 0 until 16) for (x in 0 until 16) {
            val d = kotlin.math.sqrt((x + 0.5f - 8f) * (x + 0.5f - 8f) + (y + 0.5f - 8f) * (y + 0.5f - 8f))
            val a = (7.5f - d).coerceIn(0f, 1f) * 0.75f
            drawPixel(x, y, Color.rgba8888(Color(254 / 255f, 254 / 255f, 254 / 255f, a)))
        }
    })

    init {
        setSize(UiKit.VIEW_W, UiKit.VIEW_H)
        addActor(Image(background).apply { setSize(UiKit.VIEW_W, UiKit.VIEW_H) })
        addActor(ui.menuButton(" Go Back").apply {
            godotBounds(83f, 87f, 326f, 116f)
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) = onBack() })
        })
        val panel = Image(ui.flat(Color(0.1f, 0.1f, 0.1f, 0.6f))).apply { godotCentered(-301.5f, -177.5f, 301.5f, 177.5f) }
        addActor(panel)
        val box = Table().apply { godotCentered(-264.5f, -148.5f, 264.5f, 148.5f); top() }
        box.add(ui.label("Optons", 40).apply { setAlignment(Align.center) }).growX().row()
        for ((name, bus) in listOf("Master Volume" to "Master", "Music Volume  " to "Music", "SFX Volume      " to "SFX")) {
            val style = Slider.SliderStyle(
                StyleBoxLine(Color.WHITE, 6f, ui.white),
                TextureRegionDrawable(grabber),
            ).apply { knobBefore = StyleBoxLine(Color(0.13997257f, 0.16064858f, 0.99999994f, 1f), 6f, ui.white) }
            val slider = Slider(0f, 1f, 0.05f, false, style)
            val db = when (bus) { "Master" -> buses.masterDb; "Music" -> buses.musicDb; else -> buses.sfxDb }
            slider.value = ((AudioBuses.dbToLinear(db) / 0.05).roundToInt() * 0.05).toFloat()
            slider.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    val newDb = AudioBuses.linearToDb(slider.value.toDouble())
                    when (bus) { "Master" -> buses.masterDb = newDb; "Music" -> buses.musicDb = newDb; else -> buses.sfxDb = newDb }
                    onChanged()
                }
            })
            val row = Table()
            row.add(ui.label(name, 22)).left()
            row.add(slider).growX().padLeft(4f)
            box.add(row).growX().padTop(20f).row()
        }
        addActor(box)
    }

    fun disposeTextures() = grabber.dispose()
}
