package ro.klooseduard.streetsofsibiu.core.render

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Godot `TileSetAtlasSource` with `use_texture_padding = true` (the default, tile_set.cpp
 * `_create_padded_image_texture`): every tile is copied into a grid of (size + 2) cells at
 * (ax * (size + 2) + 1, ay * (size + 2) + 1), and its edge rows/columns and corner pixels are
 * duplicated outward. Sampling at a tile border (e.g. the camera on a half pixel) then returns
 * the tile's own edge instead of the neighbouring atlas tile, exactly as in Godot.
 */
class PaddedTileAtlas(file: FileHandle, private val size: Int, columns: Int, rows: Int) {
    val texture: Texture
    private val regions: Array<Array<TextureRegion>>

    init {
        val src = Pixmap(file)
        val cell = size + 2
        val dst = Pixmap(columns * cell, rows * cell, src.format)
        dst.blending = Pixmap.Blending.None
        for (ax in 0 until columns) for (ay in 0 until rows) {
            val sx = ax * size
            val sy = ay * size
            val bx = ax * cell + 1
            val by = ay * cell + 1
            dst.drawPixmap(src, sx, sy, size, size, bx, by, size, size)
            dst.drawPixmap(src, sx, sy, size, 1, bx, by - 1, size, 1)                       // top
            dst.drawPixmap(src, sx, sy + size - 1, size, 1, bx, by + size, size, 1)         // bottom
            dst.drawPixmap(src, sx, sy, 1, size, bx - 1, by, 1, size)                       // left
            dst.drawPixmap(src, sx + size - 1, sy, 1, size, bx + size, by, 1, size)         // right
            dst.drawPixel(bx - 1, by - 1, src.getPixel(sx, sy))                             // corners
            dst.drawPixel(bx + size, by - 1, src.getPixel(sx + size - 1, sy))
            dst.drawPixel(bx - 1, by + size, src.getPixel(sx, sy + size - 1))
            dst.drawPixel(bx + size, by + size, src.getPixel(sx + size - 1, sy + size - 1))
        }
        texture = Texture(dst).apply { setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) }
        src.dispose()
        dst.dispose()
        regions = Array(columns) { ax ->
            Array(rows) { ay ->
                TextureRegion(texture, ax * cell + 1, ay * cell + 1, size, size).apply { flip(false, true) } // y-down camera
            }
        }
    }

    fun region(ax: Int, ay: Int): TextureRegion = regions[ax][ay]

    fun dispose() = texture.dispose()
}
