package ro.klooseduard.streetsofsibiu.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import ro.klooseduard.streetsofsibiu.logic.data.WeaponDef

/**
 * inventory/information_ui.tscn (top-left: HP bar, shield, ammo) and inventory/inventory_ui.tscn
 * (bottom-left, scale 0.75: three weapon slots, two consumable slots). Geometry from the scenes.
 */
class HudView(private val ui: UiKit, textures: (String) -> Texture, private val onSlotTapped: (Int) -> Unit = {}) : Group() {
    private val hpLabel: Label
    private val shieldLabel: Label
    private val ammoLabel: Label
    /** ProgressBar.value (tweened by the game on health_changed). */
    var hpBarValue = 0.0
    var hpBarMax = 100.0
    private val slotImages = mutableListOf<Image>()
    private val emptySlot = textures("item/item_assets/item icons/invslot.png")
    private val iconFor = textures

    init {
        setSize(UiKit.VIEW_W, UiKit.VIEW_H)
        // information_ui
        addActor(Image(ui.color(Color(0.5019608f, 0.5019608f, 0.5019608f, 0.5019608f))).apply { godotBounds(0f, 0f, 410f, 115f) })
        addActor(object : com.badlogic.gdx.scenes.scene2d.Actor() {
            val bg = ui.flat(Color(0.2665752f, 0.26657522f, 0.26657507f, 1f))
            val fill = ui.flat(Color(0.9582167f, 0.180915f, 0.21840319f, 1f))
            override fun draw(batch: Batch, parentAlpha: Float) {
                bg.draw(batch, x, y, width, height)
                val w = (width * (hpBarValue / hpBarMax).coerceIn(0.0, 1.0)).toFloat()
                if (w > 0) fill.draw(batch, x, y, w, height)
            }
        }.apply { godotBounds(5f, 10f, 400f, 20f) })
        hpLabel = ui.label("", 16).apply { godotBounds(155f, 8.5f, 100f, 23f); setAlignment(Align.left) }
        addActor(hpLabel)
        addActor(Image(StyleBoxLine(Color(0.5f, 0.5f, 0.5f, 1f), 1f, ui.white)).apply { godotBounds(9f, 34f, 392f, 20f) })
        addActor(Image(textures("item/item_assets/item icons/Shield.png")).apply { setScaling(Scaling.fit); godotBounds(5f, 58f, 100f, 50f) })
        shieldLabel = ui.label("", 16).apply { godotBounds(109f, 58f, 120f, 50f); setAlignment(Align.left) }
        addActor(shieldLabel)
        ammoLabel = ui.label("", 16).apply { godotBounds(240f, 58f, 160f, 50f); setAlignment(Align.left); isVisible = false }
        addActor(ammoLabel)

        // inventory_ui: Control at bottom-left with scale 0.75; HBox (30,-290)-(630,-30) unscaled.
        val s = 0.75f
        fun place(img: Image, x: Float, y: Float, w: Float, h: Float) {
            img.godotBounds(30f * s + x * s, 1080f - 290f * s + y * s, w * s, h * s)
            addActor(img)
        }
        for (i in 0..2) {
            val img = Image(emptySlot).apply { setScaling(Scaling.fit) }
            slotImages += img
            place(img, i * 132f, 132f, 128f, 128f)
        }
        place(Image(StyleBoxLine(Color(0.5f, 0.5f, 0.5f, 1f), 1f, ui.white, vertical = true)), 396f, 4f, 75f, 252f)   // VSeparator, separation 75
        for (i in 0..1) {
            val img = Image(emptySlot)
            slotImages += img
            place(img, 475f, i * 132f, 125f, 128f)   // clip_contents: the 128 px slot is clipped at the HBox's 600 px edge
        }
    }

    init {
        // Touch scheme (Q1): tapping a slot selects it, like the 1-5 keys.
        slotImages.forEachIndexed { i, img ->
            img.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) = onSlotTapped(i)
            })
        }
    }

    fun setStats(hp: Double, maxHp: Double, shield: Double) {
        hpLabel.setText("HP: %d / %d".format(java.util.Locale.ROOT, hp.toInt(), maxHp.toInt()))
        hpBarMax = maxHp
        shieldLabel.setText("Shield: %d".format(java.util.Locale.ROOT, shield.toInt()))
    }

    /** inventory_ui.gd update_ui(): icon or empty slot, empty alpha 0.2, selected slot yellow. */
    fun setInventory(items: List<WeaponDef?>, selected: Int) {
        items.forEachIndexed { i, item ->
            val img = slotImages[i]
            if (item != null) {
                img.drawable = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(iconFor(item.icon))
                img.color = Color(1f, 1f, 1f, 1f)
            } else {
                img.drawable = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(emptySlot)
                img.color = Color(1f, 1f, 1f, 0.2f)
            }
            if (i == selected) img.color = Color(1f, 1f, 0f, 1f)
        }
    }
}
