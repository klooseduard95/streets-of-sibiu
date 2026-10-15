package ro.klooseduard.streetsofsibiu.core.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

/**
 * Fonts used by the original UI, rendered with FreeType at the exact sizes the scenes use.
 * - Open Sans SemiBold: Godot 4.6.1's built-in default theme font (thirdparty/fonts), size 16 by default.
 * - Montserrat Regular: main-menu title (SystemFont "Montserrat", decision Q6/O1).
 * - JetBrains Mono NL Regular: pause/death statistics (SystemFont "JetBrains Mono NL").
 */
class Fonts {
    enum class Face(val file: String) {
        DEFAULT("fonts/OpenSans-SemiBold.ttf"),
        MONTSERRAT("fonts/Montserrat-Regular.ttf"),
        JETBRAINS_MONO_NL("fonts/JetBrainsMonoNL-Regular.ttf"),
    }

    private data class Key(val face: Face, val size: Int, val outline: Int, val outlineColor: Color?, val shadowY: Int, val color: Color, val lineSpacing: Int)

    private val generators = HashMap<Face, FreeTypeFontGenerator>()
    private val cache = HashMap<Key, BitmapFont>()

    fun get(
        face: Face = Face.DEFAULT,
        size: Int = 16,
        color: Color = Color.WHITE,
        outline: Int = 0,
        outlineColor: Color? = null,
        shadowY: Int = 0,
        lineSpacing: Int = 3,
    ): BitmapFont = cache.getOrPut(Key(face, size, outline, outlineColor, shadowY, color, lineSpacing)) {
        val generator = generators.getOrPut(face) { FreeTypeFontGenerator(Gdx.files.internal(face.file)) }
        generator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            this.size = size
            this.color = color
            hinting = FreeTypeFontGenerator.Hinting.Slight
            minFilter = Texture.TextureFilter.Linear
            magFilter = Texture.TextureFilter.Linear
            if (outline > 0) {
                borderWidth = outline / 2f   // Godot outline_size is the full width of the outline
                borderColor = outlineColor ?: Color.BLACK
            }
            if (shadowY != 0) {
                shadowOffsetX = 0
                shadowOffsetY = shadowY
                shadowColor = Color(0f, 0f, 0f, 1f)
            }
            incremental = true
        }).also { it.data.setLineHeight(it.data.lineHeight + lineSpacing) }   // Godot Label line_spacing
    }

    fun dispose() {
        cache.values.forEach { it.dispose() }
        generators.values.forEach { it.dispose() }
    }
}
