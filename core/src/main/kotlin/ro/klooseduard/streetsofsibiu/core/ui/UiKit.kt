package ro.klooseduard.streetsofsibiu.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import ro.klooseduard.streetsofsibiu.logic.GodotProjectSettings

class UiKit(val fonts: Fonts) {
    val white: Texture = Texture(Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() })
    private val styleBoxes = mutableListOf<StyleBoxFlat>()

    fun flat(
        bg: Color, corner: Int = 0, border: Int = 0, borderColor: Color = Color.CLEAR,
        shadowSize: Int = 0, shadowOffsetY: Float = 0f, shadowColor: Color = Color(0f, 0f, 0f, 0.6f),
        drawCenter: Boolean = true, expand: Float = 0f,
    ): StyleBoxFlat = StyleBoxFlat(bg, corner, border, borderColor, shadowSize, 0f, shadowOffsetY, shadowColor, drawCenter, expand).also { styleBoxes += it }

    fun color(c: Color): Drawable = ColorDrawable(c, white)

    fun godotDefaultButton(text: String, fontSize: Int): TextButton {
        val style = TextButton.TextButtonStyle().apply {
            up = flat(Color(0.1f, 0.1f, 0.1f, 0.6f), corner = 3)
            over = flat(Color(0.225f, 0.225f, 0.225f, 0.6f), corner = 3)
            down = flat(Color(0f, 0f, 0f, 0.6f), corner = 3)
            font = fonts.get(size = fontSize)
            fontColor = Color(0.875f, 0.875f, 0.875f, 1f)
            overFontColor = Color(0.95f, 0.95f, 0.95f, 1f)
            downFontColor = Color.WHITE
        }
        return TextButton(text, style)
    }

    fun menuButton(text: String, fontSize: Int = 35): TextButton {
        val shadow = Color(0f, 0f, 0f, 0.8f)
        val style = TextButton.TextButtonStyle().apply {
            up = flat(Color.WHITE, corner = 16, shadowSize = 5, shadowOffsetY = 3f, shadowColor = shadow)
            over = flat(Color(0.54816455f, 0.5481646f, 0.5481646f, 1f), corner = 16, shadowSize = 5, shadowOffsetY = 3f, shadowColor = shadow)
            down = over
            font = fonts.get(size = fontSize)
            fontColor = Color(0.0093103545f, 0.0093103545f, 0.0093103545f, 1f)
            overFontColor = Color.BLACK
            downFontColor = Color.BLACK
        }
        return TextButton(text, style)
    }

    fun label(
        text: String, size: Int = 16, color: Color = Color.WHITE, face: Fonts.Face = Fonts.Face.DEFAULT,
        outline: Int = 0, outlineColor: Color? = null, shadowY: Int = 0, lineSpacing: Int = 3, glyphColor: Color = Color.WHITE,
    ): Label = Label(text, Label.LabelStyle(fonts.get(face, size, glyphColor, outline, outlineColor, shadowY, lineSpacing), color))

    fun dispose() {
        white.dispose()
        styleBoxes.forEach { it.dispose() }
    }

    companion object {
        val VIEW_W = GodotProjectSettings.VIEWPORT_WIDTH.toFloat()
        val VIEW_H = GodotProjectSettings.VIEWPORT_HEIGHT.toFloat()
    }
}

class ShadowedLabel(
    private val text: String,
    private val font: com.badlogic.gdx.graphics.g2d.BitmapFont,
    private val textColor: Color,
    private val shadowColor: Color,
    shadowSize: Int,
    private val offsetX: Float,
    private val offsetY: Float,
) : Actor() {
    private val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout()
    private val stamps: List<Pair<Float, Float>> = run {
        val r = shadowSize / 2f
        val list = mutableListOf<Pair<Float, Float>>()
        var dy = -r
        while (dy <= r) {
            var dx = -r
            while (dx <= r) {
                if (dx * dx + dy * dy <= r * r) list += dx to dy
                dx += 1.5f
            }
            dy += 1.5f
        }
        list
    }

    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
        layout.setText(font, text)
        val tx = x + (width - layout.width) / 2
        val ty = y + (height + layout.height) / 2
        font.color = shadowColor
        for ((dx, dy) in stamps) font.draw(batch, text, tx + offsetX + dx, ty - offsetY + dy)
        font.color = textColor
        font.draw(batch, text, tx, ty)
        font.color = Color.WHITE
    }
}

fun Actor.godotBounds(x: Float, y: Float, w: Float, h: Float, parentHeight: Float = UiKit.VIEW_H) {
    setBounds(x, parentHeight - y - h, w, h)
}

fun Actor.godotCentered(left: Float, top: Float, right: Float, bottom: Float, parentW: Float = UiKit.VIEW_W, parentH: Float = UiKit.VIEW_H) {
    godotBounds(parentW / 2 + left, parentH / 2 + top, right - left, bottom - top, parentH)
}
