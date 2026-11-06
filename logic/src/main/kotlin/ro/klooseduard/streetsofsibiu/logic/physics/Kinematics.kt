package ro.klooseduard.streetsofsibiu.logic.physics

import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class BodyShape(val radius: Double, val height: Double = radius * 2) {
    val halfSegment: Double get() = max(0.0, height / 2 - radius)
}

class StaticWorld(solids: List<Rect>) {
    private val rects = solids.map { doubleArrayOf(it.x.toDouble(), it.y.toDouble(), it.right.toDouble(), it.bottom.toDouble()) }

    fun query(x0: Double, y0: Double, x1: Double, y1: Double): List<DoubleArray> =
        rects.filter { it[0] <= x1 && it[2] >= x0 && it[1] <= y1 && it[3] >= y0 }

    val size: Int get() = rects.size
}

data class MotionResult(
    val collided: Boolean,
    val travel: DVec,
    val remainder: DVec,
    val normal: DVec = DVec.ZERO,
    val depth: Double = 0.0,
    val safeFraction: Double = 1.0,
    val unsafeFraction: Double = 1.0,
)

data class DVec(val x: Double, val y: Double) {
    operator fun plus(o: DVec) = DVec(x + o.x, y + o.y)
    operator fun minus(o: DVec) = DVec(x - o.x, y - o.y)
    operator fun times(s: Double) = DVec(x * s, y * s)
    fun dot(o: DVec) = x * o.x + y * o.y
    fun length() = sqrt(x * x + y * y)
    fun normalized(): DVec = length().let { if (it == 0.0) ZERO else DVec(x / it, y / it) }
    fun slide(n: DVec) = this - n * dot(n)
    fun isZeroApprox() = abs(x) < CMP_EPSILON && abs(y) < CMP_EPSILON
    fun toVec2() = Vec2(x.toFloat(), y.toFloat())

    companion object {
        val ZERO = DVec(0.0, 0.0)
        fun of(v: Vec2) = DVec(v.x.toDouble(), v.y.toDouble())
    }
}

internal const val CMP_EPSILON = 0.00001

object Kinematics {
    private const val MIN_CONTACT_DEPTH_FACTOR = 0.05
    private const val MARGIN_MIN = 0.0001

    private class Contact(val signedDistance: Double, val normal: DVec)

    private fun contact(shape: BodyShape, c: DVec, b: DoubleArray): Contact {
        val h = shape.halfSegment
        val r = shape.radius
        val s0 = c.y - h
        val s1 = c.y + h
        val dx = when {
            c.x < b[0] -> b[0] - c.x
            c.x > b[2] -> c.x - b[2]
            else -> 0.0
        }
        val dy = when {
            s1 < b[1] -> b[1] - s1
            s0 > b[3] -> s0 - b[3]
            else -> 0.0
        }
        if (dx > 0 || dy > 0) {
            val d = sqrt(dx * dx + dy * dy)
            val nx = if (c.x < b[0]) -dx else if (c.x > b[2]) dx else 0.0
            val ny = if (s1 < b[1]) -dy else if (s0 > b[3]) dy else 0.0
            return Contact(d - r, DVec(nx / d, ny / d))
        }
        val pushLeft = (c.x + r) - b[0]
        val pushRight = b[2] - (c.x - r)
        val pushUp = (s1 + r) - b[1]
        val pushDown = b[3] - (s0 - r)
        val m = min(min(pushLeft, pushRight), min(pushUp, pushDown))
        val n = when (m) {
            pushLeft -> DVec(-1.0, 0.0)
            pushRight -> DVec(1.0, 0.0)
            pushUp -> DVec(0.0, -1.0)
            else -> DVec(0.0, 1.0)
        }
        return Contact(-m, n)
    }

    private fun timeOfImpact(shape: BodyShape, c: DVec, m: DVec, b: DoubleArray): Double? {
        val h = shape.halfSegment
        val r = shape.radius
        val ey0 = b[1] - h
        val ey1 = b[3] + h
        var best: Double? = null
        fun consider(t: Double?) {
            if (t != null && t >= 0.0 && t <= 1.0 && (best == null || t < best!!)) best = t
        }
        consider(rayBox(c, m, b[0] - r, ey0, b[2] + r, ey1))
        consider(rayBox(c, m, b[0], ey0 - r, b[2], ey1 + r))
        for ((px, py) in listOf(b[0] to ey0, b[2] to ey0, b[0] to ey1, b[2] to ey1)) consider(rayCircle(c, m, px, py, r))
        return best
    }

    private fun rayBox(c: DVec, m: DVec, x0: Double, y0: Double, x1: Double, y1: Double): Double? {
        var tMin = 0.0
        var tMax = 1.0
        for ((p, d, lo, hi) in listOf(arrayOf(c.x, m.x, x0, x1), arrayOf(c.y, m.y, y0, y1))) {
            if (abs(d) < 1e-12) {
                if (p < lo || p > hi) return null
            } else {
                var t0 = (lo - p) / d
                var t1 = (hi - p) / d
                if (t0 > t1) { val tmp = t0; t0 = t1; t1 = tmp }
                tMin = max(tMin, t0)
                tMax = min(tMax, t1)
                if (tMin > tMax) return null
            }
        }
        return tMin
    }

    private fun rayCircle(c: DVec, m: DVec, px: Double, py: Double, r: Double): Double? {
        val fx = c.x - px
        val fy = c.y - py
        val a = m.x * m.x + m.y * m.y
        val cc = fx * fx + fy * fy - r * r
        if (cc <= 0) return 0.0
        if (a < 1e-24) return null
        val bb = 2 * (fx * m.x + fy * m.y)
        val disc = bb * bb - 4 * a * cc
        if (disc < 0) return null
        val t = (-bb - sqrt(disc)) / (2 * a)
        return if (t in 0.0..1.0) t else null
    }

    fun testBodyMotion(world: StaticWorld, shape: BodyShape, from: DVec, motion: DVec, marginIn: Double, recoveryAsCollision: Boolean = true): MotionResult {
        val margin = max(marginIn, MARGIN_MIN)
        val minContactDepth = margin * MIN_CONTACT_DEPTH_FACTOR
        val ext = shape.radius + margin
        val extY = shape.halfSegment + shape.radius + margin
        var pos = from
        var recovered = false

        repeat(4) {
            val nearby = world.query(pos.x - ext, pos.y - extY, pos.x + ext, pos.y + extY)
            val contacts = nearby.map { contact(shape, pos, it) }.filter { it.signedDistance < margin }
            if (contacts.isEmpty()) return@repeat
            recovered = true
            var recover = DVec.ZERO
            for (ct in contacts) {
                val into = ct.normal * -1.0
                val depth = (margin - ct.signedDistance) + into.dot(recover)
                if (depth > minContactDepth + CMP_EPSILON) recover -= into * ((depth - minContactDepth) * 0.4)
            }
            if (recover == DVec.ZERO) return@repeat
            pos += recover
        }

        val motionLength = motion.length()
        var safe = 1.0
        var unsafe = 1.0
        val candidates = world.query(
            min(pos.x, pos.x + motion.x) - ext, min(pos.y, pos.y + motion.y) - extY,
            max(pos.x, pos.x + motion.x) + ext, max(pos.y, pos.y + motion.y) + extY,
        )
        for (b in candidates) {
            val toi = timeOfImpact(shape, pos, motion, b) ?: continue
            if (contact(shape, pos, b).signedDistance < 0 || toi == 0.0 && motionLength == 0.0) {
                safe = 0.0; unsafe = 0.0
                break
            }
            var low = 0.0
            var hi = 1.0
            var coeff = 0.5
            for (k in 0 until 8) {
                val fraction = low + (hi - low) * coeff
                if (toi <= fraction) {
                    hi = fraction
                    coeff = if (k == 0 || low > 0.0) 0.5 else 0.25
                } else {
                    low = fraction
                    coeff = if (k == 0 || hi < 1.0) 0.5 else 0.75
                }
            }
            if (low < safe) {
                safe = low
                unsafe = hi
            }
        }

        if ((recoveryAsCollision && recovered) || safe < 1) {
            val ugt = pos + motion * unsafe
            val minAllowed = min(motionLength, minContactDepth)
            var bestLen = 0.0
            var bestNormal = DVec.ZERO
            for (b in world.query(ugt.x - ext, ugt.y - extY, ugt.x + ext, ugt.y + extY)) {
                val ct = contact(shape, ugt, b)
                val len = margin - ct.signedDistance
                if (ct.signedDistance >= margin || len < minAllowed || len <= bestLen) continue
                bestLen = len
                bestNormal = ct.normal
            }
            if (bestLen != 0.0) {
                val travel = motion * safe + (pos - from)
                return MotionResult(true, travel, motion - motion * safe, bestNormal, bestLen, safe, unsafe)
            }
        }
        return MotionResult(false, pos + motion - from, DVec.ZERO)
    }
}
