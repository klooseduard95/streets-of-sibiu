package ro.klooseduard.streetsofsibiu.logic.math

import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    fun length(): Float = sqrt(x * x + y * y)

    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}

data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val center: Vec2 get() = Vec2(x + width / 2, y + height / 2)

    fun overlaps(o: Rect): Boolean = x < o.right && o.x < right && y < o.bottom && o.y < bottom

    fun contains(p: Vec2): Boolean = p.x >= x && p.x <= right && p.y >= y && p.y <= bottom

    fun translated(d: Vec2) = Rect(x + d.x, y + d.y, width, height)

    companion object {
        fun centered(center: Vec2, width: Float, height: Float) =
            Rect(center.x - width / 2, center.y - height / 2, width, height)
    }
}
