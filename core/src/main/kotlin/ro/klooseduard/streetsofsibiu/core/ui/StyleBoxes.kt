package ro.klooseduard.streetsofsibiu.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class StyleBoxFlat(
    private val bg: Color,
    private val cornerRadius: Int = 0,
    private val borderWidth: Int = 0,
    private val borderColor: Color = Color.CLEAR,
    private val shadowSize: Int = 0,
    private val shadowOffsetX: Float = 0f,
    private val shadowOffsetY: Float = 0f,
    private val shadowColor: Color = Color(0f, 0f, 0f, 0.6f),
    private val drawCenter: Boolean = true,
    private val expandMargin: Float = 0f,
) : BaseDrawable() {
    private val textures = mutableListOf<Texture>()
    private val body: NinePatch = patch(cornerRadius, 0, fill = true)
    private val shadow: NinePatch? = if (shadowSize > 0) patch(cornerRadius, shadowSize, fill = false) else null

    private fun patch(radius: Int, feather: Int, fill: Boolean): NinePatch {
        val corner = radius + feather + max(borderWidth, 1)
        val size = corner * 2 + 2
        val pm = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pm.blending = Pixmap.Blending.None
        val inner = feather.toFloat()
        val outer = size - feather.toFloat()
        for (y in 0 until size) for (x in 0 until size) {
            val d = roundedRectDistance(x + 0.5f, y + 0.5f, inner, inner, outer, outer, radius.toFloat())
            val c = Color()
            if (fill) {
                val coverage = (0.5f - d).coerceIn(0f, 1f)
                if (coverage > 0f) {
                    val inBorder = borderWidth > 0 && d > -borderWidth
                    c.set(if (inBorder) borderColor else if (drawCenter) bg else Color.CLEAR)
                    c.a *= coverage
                }
            } else {
                val a = if (d <= 0f) 1f else (1f - d / feather).coerceIn(0f, 1f)
                c.set(shadowColor)
                c.a *= a
            }
            pm.drawPixel(x, y, Color.rgba8888(c))
        }
        val tex = Texture(pm).also { textures += it }
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pm.dispose()
        return NinePatch(tex, corner, corner, corner, corner)
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        val ex = expandMargin
        if (shadow != null) {
            val s = shadowSize.toFloat()
            shadow.draw(batch, x - ex - s + shadowOffsetX, y - ex - s - shadowOffsetY, width + 2 * (ex + s), height + 2 * (ex + s))
        }
        body.draw(batch, x - ex, y - ex, width + 2 * ex, height + 2 * ex)
    }

    fun dispose() = textures.forEach { it.dispose() }

    companion object {
        fun roundedRectDistance(px: Float, py: Float, x0: Float, y0: Float, x1: Float, y1: Float, r: Float): Float {
            val cx = (x0 + x1) / 2
            val cy = (y0 + y1) / 2
            val hx = (x1 - x0) / 2 - r
            val hy = (y1 - y0) / 2 - r
            val qx = kotlin.math.abs(px - cx) - hx
            val qy = kotlin.math.abs(py - cy) - hy
            val ox = max(qx, 0f)
            val oy = max(qy, 0f)
            return sqrt(ox * ox + oy * oy) + min(max(qx, qy), 0f) - r
        }
    }
}

class StyleBoxLine(private val color: Color, private val thickness: Float, private val white: Texture, private val vertical: Boolean = false) : BaseDrawable() {
    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        val old = batch.color.cpy()
        batch.color = color
        if (vertical) batch.draw(white, x + (width - thickness) / 2, y, thickness, height)
        else batch.draw(white, x, y + (height - thickness) / 2, width, thickness)
        batch.color = old
    }
}

class ColorDrawable(private val color: Color, private val white: Texture) : BaseDrawable() {
    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        val old = batch.color.cpy()
        batch.setColor(color.r * old.r, color.g * old.g, color.b * old.b, color.a * old.a)
        batch.draw(white, x, y, width, height)
        batch.color = old
    }
}

object EmptyDrawable : BaseDrawable(), Drawable
