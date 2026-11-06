package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class TargetKind { ENEMY_HITBOX, ENEMY_BODY }

interface BulletTarget {
    val kind: TargetKind
    fun overlapsCapsule(ax: Double, ay: Double, bx: Double, by: Double, radius: Double): Boolean
    fun takeDamage(amount: Double)
}

class Bullet(
    position: Vec2,
    val dirX: Double,
    val dirY: Double,
    val speed: Double,
    val dmg: Double,
    val maxRange: Double,
    val isMelee: Boolean,
    val weaponIcon: String? = null,
) {
    var x = position.x.toDouble(); private set
    var y = position.y.toDouble(); private set
    var traveled = 0.0; private set
    var isFreed = false; private set
    var onTargetEntered: () -> Unit = {}
    internal val insideTargets = mutableSetOf<Any>()
    internal var insideWalls = mutableSetOf<Int>()

    val rotation: Double get() = kotlin.math.atan2(dirY, dirX)

    internal fun segment(): DoubleArray {
        val half = if (isMelee) MELEE_HALF_SEGMENT else RANGED_HALF_SEGMENT
        val ax = if (isMelee) -dirY else dirX
        val ay = if (isMelee) dirX else dirY
        return doubleArrayOf(x - ax * half, y - ay * half, x + ax * half, y + ay * half)
    }

    val radius: Double get() = if (isMelee) MELEE_RADIUS else RANGED_RADIUS

    internal fun step(delta: Double) {
        val move = speed * delta
        x += dirX * move
        y += dirY * move
        traveled += move
        if (traveled > maxRange) free()
    }

    internal fun free() {
        isFreed = true
    }

    companion object {
        const val RANGED_RADIUS = 11.0
        const val RANGED_HALF_SEGMENT = 5.0
        const val MELEE_RADIUS = 36.0
        const val MELEE_HALF_SEGMENT = 10.0
    }
}

class BulletSystem(private val runtime: GameRuntime) {
    val bullets = mutableListOf<Bullet>()
    var world = StaticWorld(emptyList())
    var targets: () -> List<BulletTarget> = { emptyList() }

    fun spawn(bullet: Bullet) {
        runtime.callDeferred { bullets += bullet }
    }

    fun clear() = bullets.clear()

    fun flushPhysicsQueries() {
        for (bullet in bullets.toList()) {
            if (bullet.isFreed) continue
            val seg = bullet.segment()
            val r = bullet.radius
            val nearWalls = world.query(
                min(seg[0], seg[2]) - r, min(seg[1], seg[3]) - r, max(seg[0], seg[2]) + r, max(seg[1], seg[3]) + r,
            )
            val wallsNow = mutableSetOf<Int>()
            for (box in nearWalls) if (segmentBoxDistance(seg, box) <= r) wallsNow += System.identityHashCode(box)
            val newWalls = wallsNow - bullet.insideWalls
            bullet.insideWalls = wallsNow
            val hits = targets().filter { it.overlapsCapsule(seg[0], seg[1], seg[2], seg[3], r) }
            val hitsSet = hits.toSet()
            val newTargets = hits.filter { it !in bullet.insideTargets }
            bullet.insideTargets.retainAll(hitsSet)
            bullet.insideTargets.addAll(hits)
            for (target in newTargets) {
                bullet.onTargetEntered()
                enter(bullet, target)
                if (bullet.isFreed) break
            }
            if (!bullet.isFreed) for (wall in newWalls) {
                bullet.free()
                break
            }
        }
    }

    private fun enter(bullet: Bullet, target: BulletTarget) {
        if (target.kind == TargetKind.ENEMY_HITBOX) {
            target.takeDamage(bullet.dmg)
        } else if (bullet.isMelee) {
            return
        } else {
            bullet.free()
        }
        if (!bullet.isMelee) bullet.free()
    }

    fun physicsProcess(delta: Double) {
        for (bullet in bullets.toList()) {
            if (!bullet.isFreed) bullet.step(delta)
        }
        bullets.removeAll { it.isFreed }
    }

    companion object {
        fun segmentBoxDistance(seg: DoubleArray, box: DoubleArray): Double {
            var best = Double.MAX_VALUE
            val steps = 8
            for (i in 0..steps) {
                val t = i.toDouble() / steps
                val px = seg[0] + (seg[2] - seg[0]) * t
                val py = seg[1] + (seg[3] - seg[1]) * t
                val dx = max(max(box[0] - px, 0.0), px - box[2])
                val dy = max(max(box[1] - py, 0.0), py - box[3])
                best = min(best, sqrt(dx * dx + dy * dy))
            }
            return best
        }
    }
}
