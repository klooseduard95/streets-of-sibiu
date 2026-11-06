package ro.klooseduard.streetsofsibiu.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener

class PauseView(ui: UiKit, onResume: () -> Unit, onSettings: () -> Unit, onMainMenu: () -> Unit) : Group() {
    private val stats: Label = ui.label("", 28, face = Fonts.Face.JETBRAINS_MONO_NL, outline = 3, outlineColor = Color(1f, 0.85882354f, 0f, 1f), lineSpacing = 4)

    init {
        setSize(UiKit.VIEW_W, UiKit.VIEW_H)
        addActor(Image(ui.flat(Color(0.1f, 0.1f, 0.1f, 0.6f))).apply { godotCentered(-350f, -315f, 350f, 315f) })
        val buttons = Table()
        fun button(text: String, action: () -> Unit) = ui.godotDefaultButton(text, 32).apply {
            addListener(object : ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) = action() })
        }
        buttons.add(button("Resume", onResume)).fillX().row()
        buttons.add(button("Settings", onSettings)).fillX().padTop(80f).row()
        buttons.add(button("Main Menu", onMainMenu)).fillX().padTop(80f).row()
        val row = Table().apply { godotCentered(-350f, -315f, 350f, 315f) }
        row.add().width(15f)
        row.add(buttons).center()
        row.add(Image(StyleBoxLine(Color(0.5f, 0.5f, 0.5f, 1f), 1f, ui.white, vertical = true))).width(20f).growY().padTop(4f).padBottom(4f)
        row.add(stats).expandX().center()
        row.add().width(20f)
        addActor(row)
    }

    fun setStats(level: Int, hp: Double, maxHp: Double, shield: Double) {
        stats.setText(
            "Player Statistics\n" +
                "--------------------------\n" +
                "Level: %d/8\n".format(java.util.Locale.ROOT, level) +
                "HP: %d / %d\n".format(java.util.Locale.ROOT, hp.toInt(), maxHp.toInt()) +
                "Shield: %d\n".format(java.util.Locale.ROOT, shield.toInt()) +
                "Bonus Damage: %d%%\n".format(java.util.Locale.ROOT, 0) +
                "Speed mult: %.2fX\n".format(java.util.Locale.ROOT, 1.0) +
                "Critical Chance: %d%%\n".format(java.util.Locale.ROOT, 0) +
                "Enemy Slowness: %d%%\n".format(java.util.Locale.ROOT, 0) +
                "Damage Reduction: %d\n".format(java.util.Locale.ROOT, 0) +
                "Bonus Ammo Capacity: %d\n".format(java.util.Locale.ROOT, 0) +
                "Sniper Cooldown Reduction: -%.2fs\n".format(java.util.Locale.ROOT, 0.0),
        )
    }
}
