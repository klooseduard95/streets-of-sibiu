package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.AttackParams
import ro.klooseduard.streetsofsibiu.logic.data.EnemyType
import ro.klooseduard.streetsofsibiu.logic.data.SpawnTables
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.nav.NavMesh
import ro.klooseduard.streetsofsibiu.logic.nav.NavQuery
import ro.klooseduard.streetsofsibiu.logic.physics.DynamicCollider
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime
import ro.klooseduard.streetsofsibiu.logic.runtime.SceneNode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class EnemyProjectile(
    val params: AttackParams,
    val angle: Double,
    val damage: Double,
    spawn: Vec2,
    private val system: EnemySystem,
) {
    var x = 0.0; private set
    var y = 0.0; private set
    var isFreed = false; private set
    var timeLeft = params.travelTime; private set
    internal var insidePlayer = false
    internal val insideWalls = mutableSetOf<Int>()

    init {
        system.shapeWidth = params.kind.width
        system.shapeHeight = params.kind.height
        x = spawn.x + cos(angle) * system.shapeWidth / 2
        y = spawn.y + sin(angle) * system.shapeWidth / 2
    }

    internal fun step() {
        x += cos(angle) * params.speedPerTick
        y += sin(angle) * params.speedPerTick
    }

    internal fun tickTimer(delta: Double) {
        timeLeft -= delta
        if (timeLeft < 0) free()
    }

    internal fun free() {
        isFreed = true
    }

    internal fun corners(): List<DoubleArray> {
        val hw = system.shapeWidth / 2
        val hh = system.shapeHeight / 2
        val c = cos(angle)
        val s = sin(angle)
        return listOf(doubleArrayOf(-hw, -hh), doubleArrayOf(hw, -hh), doubleArrayOf(hw, hh), doubleArrayOf(-hw, hh))
            .map { doubleArrayOf(x + it[0] * c - it[1] * s, y + it[0] * s + it[1] * c) }
    }
}

class EnemySystem(val runtime: GameRuntime, val player: Player) {
    val root = SceneNode()
    val enemies = mutableListOf<Enemy>()
    val projectiles = mutableListOf<EnemyProjectile>()
    var world = StaticWorld(emptyList())
        private set
    var query = NavQuery(NavMesh(emptyList()))
        private set
    var enemyCount = 0
    var shapeWidth = 20.0
    var shapeHeight = 20.0
    var onEnemySound: (EnemyType, Long) -> Unit = { _, _ -> }
    private val playerCollider = DynamicCollider(player, { player.position }, 30.0, 12.5)

    fun attach(world: StaticWorld, mesh: NavMesh) {
        this.world = world
        query = NavQuery(mesh)
        world.dynamicBodies.clear()
        world.dynamicBodies += playerCollider
        player.body.owner = player
        enemies.clear()
        projectiles.clear()
        enemyCount = 0
    }

    fun spawn(type: EnemyType, level: Int, spawnPos: Vec2?): Enemy {
        val enemy = Enemy(type, this, Enemy.statsFor(type), level, spawnPos)
        runtime.callDeferred {
            enemies += enemy
            world.dynamicBodies += enemy.collider
        }
        return enemy
    }

    fun spawnFromNode(position: Vec2, radius: Int, level: Int) {
        val table = SpawnTables.forLevel(level)
        val random = runtime.random
        repeat(random.randiRange(table.minCount, table.maxCount)) {
            val type = table.weightedTypes[random.randiRange(0, table.weightedTypes.size - 1)]
            val angle = Math.toRadians(random.randiRange(0, 359).toDouble())
            val pos = Vec2(position.x + (radius * cos(angle)).toFloat(), position.y + (radius * sin(angle)).toFloat())
            if (type == EnemyType.RAT || type == EnemyType.PIGEON || type == EnemyType.THIEF || type == EnemyType.CRIMINAL) {
                spawn(type, level, pos)
            }
        }
    }

    fun remove(enemy: Enemy) {
        enemy.free()
        enemies -= enemy
        world.dynamicBodies.remove(enemy.collider)
    }

    fun spawnProjectile(params: AttackParams, angle: Double, damage: Double, from: Vec2) {
        val projectile = EnemyProjectile(params, angle, damage, from, this)
        runtime.callDeferred { projectiles += projectile }
    }

    val bulletTargets: List<BulletTarget>
        get() = enemies.flatMap { listOf(hitboxOf(it), bodyOf(it)) }

    private val hitboxes = HashMap<Enemy, BulletTarget>()
    private val bodies = HashMap<Enemy, BulletTarget>()

    private fun hitboxOf(e: Enemy) = hitboxes.getOrPut(e) { CircleTarget(e, TargetKind.ENEMY_HITBOX, 27.0) }
    private fun bodyOf(e: Enemy) = bodies.getOrPut(e) { CircleTarget(e, TargetKind.ENEMY_BODY, 10.0) }

    private class CircleTarget(val enemy: Enemy, override val kind: TargetKind, val baseRadius: Double) : BulletTarget {
        override fun overlapsCapsule(ax: Double, ay: Double, bx: Double, by: Double, radius: Double): Boolean {
            val r = baseRadius * enemy.sizeModifier + radius
            return segmentPointDistance(ax, ay, bx, by, enemy.position.x.toDouble(), enemy.position.y.toDouble()) <= r
        }

        override fun takeDamage(amount: Double) = enemy.takeDamage(amount)
    }

    fun playerOverlapsCircle(cx: Double, cy: Double, radius: Double): Boolean {
        val p = player.position
        val py = p.y.toDouble()
        val clampedY = cy.coerceIn(py - 12.5, py + 12.5)
        val dx = p.x - cx
        val dy = py.let { clampedY - cy }
        return sqrt(dx * dx + dy * dy) < radius + 30.0
    }

    fun firstHitIsPlayer(from: Vec2, to: Vec2, self: Enemy): Boolean {
        val ox = from.x.toDouble()
        val oy = from.y.toDouble()
        val dx = (to.x - from.x).toDouble()
        val dy = (to.y - from.y).toDouble()
        var best = Double.MAX_VALUE
        var bestIsPlayer = false
        for (b in world.queryStatic(min(ox, ox + dx), min(oy, oy + dy), max(ox, ox + dx), max(oy, oy + dy))) {
            rayBox(ox, oy, dx, dy, b[0], b[1], b[2], b[3])?.let { if (it < best) { best = it; bestIsPlayer = false } }
        }
        for (e in enemies) {
            if (e === self) continue
            rayCircle(ox, oy, dx, dy, e.position.x.toDouble(), e.position.y.toDouble(), 10.0 * e.sizeModifier)?.let { if (it < best) { best = it; bestIsPlayer = false } }
        }
        val px = player.position.x.toDouble()
        val py = player.position.y.toDouble()
        val hits = listOfNotNull(
            rayBox(ox, oy, dx, dy, px - 30.0, py - 12.5, px + 30.0, py + 12.5),
            rayCircle(ox, oy, dx, dy, px, py - 12.5, 30.0),
            rayCircle(ox, oy, dx, dy, px, py + 12.5, 30.0),
        )
        hits.minOrNull()?.let { if (it < best) { best = it; bestIsPlayer = true } }
        return bestIsPlayer && best <= 1.0
    }

    fun flushPhysicsQueries() {
        for (enemy in enemies.toList()) enemy.flushHitRange()
        for (projectile in projectiles.toList()) {
            if (projectile.isFreed) continue
            val hitsPlayer = projectileOverlapsPlayer(projectile)
            if (hitsPlayer && !projectile.insidePlayer) {
                projectile.insidePlayer = true
                if (!projectile.params.isSupport) {
                    player.takeDmg(projectile.damage, 0.1)
                    if (!projectile.params.piercing) projectile.free()
                }
            } else if (!hitsPlayer) {
                projectile.insidePlayer = false
            }
            if (projectile.isFreed) continue
            val corners = projectile.corners()
            val xs = corners.map { it[0] }
            val ys = corners.map { it[1] }
            val walls = world.queryStatic(xs.min(), ys.min(), xs.max(), ys.max())
            val now = mutableSetOf<Int>()
            for (w in walls) if (polygonOverlapsBox(corners, w)) now += System.identityHashCode(w)
            val entered = now - projectile.insideWalls
            projectile.insideWalls.clear()
            projectile.insideWalls.addAll(now)
            if (entered.isNotEmpty() && !projectile.params.piercing) projectile.free()
        }
    }

    private fun projectileOverlapsPlayer(p: EnemyProjectile): Boolean {
        val corners = p.corners()
        val px = player.position.x.toDouble()
        val py = player.position.y.toDouble()
        val n = corners.size
        for (i in 0 until n) {
            val a = corners[i]
            val b = corners[(i + 1) % n]
            val d = segmentSegmentDistance(a[0], a[1], b[0], b[1], px, py - 12.5, px, py + 12.5)
            if (d <= 30.0) return true
        }
        return pointInPolygon(px, py, corners) || pointInPolygon(px, py - 12.5, corners) || pointInPolygon(px, py + 12.5, corners)
    }

    fun physicsProcess(delta: Double) {
        for (enemy in enemies.toList()) enemy.physicsProcess(delta)
        for (projectile in projectiles.toList()) {
            if (!projectile.isFreed) projectile.step()
        }
    }

    fun process(delta: Double) {
        for (projectile in projectiles.toList()) if (!projectile.isFreed) projectile.tickTimer(delta)
        projectiles.removeAll { it.isFreed }
    }

    companion object {
        fun segmentPointDistance(ax: Double, ay: Double, bx: Double, by: Double, px: Double, py: Double): Double {
            val abx = bx - ax
            val aby = by - ay
            val len2 = abx * abx + aby * aby
            val t = if (len2 == 0.0) 0.0 else (((px - ax) * abx + (py - ay) * aby) / len2).coerceIn(0.0, 1.0)
            val cx = ax + abx * t
            val cy = ay + aby * t
            return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
        }

        fun segmentSegmentDistance(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, dx: Double, dy: Double): Double {
            if (segmentsIntersect(ax, ay, bx, by, cx, cy, dx, dy)) return 0.0
            return minOf(
                segmentPointDistance(ax, ay, bx, by, cx, cy), segmentPointDistance(ax, ay, bx, by, dx, dy),
                segmentPointDistance(cx, cy, dx, dy, ax, ay), segmentPointDistance(cx, cy, dx, dy, bx, by),
            )
        }

        private fun segmentsIntersect(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, dx: Double, dy: Double): Boolean {
            fun cross(ux: Double, uy: Double, vx: Double, vy: Double) = ux * vy - uy * vx
            val d1 = cross(bx - ax, by - ay, cx - ax, cy - ay)
            val d2 = cross(bx - ax, by - ay, dx - ax, dy - ay)
            val d3 = cross(dx - cx, dy - cy, ax - cx, ay - cy)
            val d4 = cross(dx - cx, dy - cy, bx - cx, by - cy)
            return d1 * d2 < 0 && d3 * d4 < 0
        }

        fun pointInPolygon(px: Double, py: Double, poly: List<DoubleArray>): Boolean {
            var inside = false
            var j = poly.size - 1
            for (i in poly.indices) {
                val a = poly[i]
                val b = poly[j]
                if ((a[1] > py) != (b[1] > py) && px < (b[0] - a[0]) * (py - a[1]) / (b[1] - a[1]) + a[0]) inside = !inside
                j = i
            }
            return inside
        }

        fun polygonOverlapsBox(poly: List<DoubleArray>, box: DoubleArray): Boolean {
            val boxCorners = listOf(doubleArrayOf(box[0], box[1]), doubleArrayOf(box[2], box[1]), doubleArrayOf(box[2], box[3]), doubleArrayOf(box[0], box[3]))
            for (axis in 0 until 2) {
                val minP = poly.minOf { it[axis] }
                val maxP = poly.maxOf { it[axis] }
                val minB = if (axis == 0) box[0] else box[1]
                val maxB = if (axis == 0) box[2] else box[3]
                if (maxP < minB || maxB < minP) return false
            }
            for (i in poly.indices) {
                val a = poly[i]
                val b = poly[(i + 1) % poly.size]
                val nx = -(b[1] - a[1])
                val ny = b[0] - a[0]
                val pp = poly.map { it[0] * nx + it[1] * ny }
                val bb = boxCorners.map { it[0] * nx + it[1] * ny }
                if (pp.max() < bb.min() || bb.max() < pp.min()) return false
            }
            return true
        }

        fun rayBox(ox: Double, oy: Double, dx: Double, dy: Double, x0: Double, y0: Double, x1: Double, y1: Double): Double? {
            var tMin = 0.0
            var tMax = 1.0
            for ((p, d, lo, hi) in listOf(arrayOf(ox, dx, x0, x1), arrayOf(oy, dy, y0, y1))) {
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

        fun rayCircle(ox: Double, oy: Double, dx: Double, dy: Double, cx: Double, cy: Double, r: Double): Double? {
            val fx = ox - cx
            val fy = oy - cy
            val a = dx * dx + dy * dy
            if (a < 1e-24) return null
            val b = 2 * (fx * dx + fy * dy)
            val c = fx * fx + fy * fy - r * r
            if (c <= 0) return 0.0
            val disc = b * b - 4 * a * c
            if (disc < 0) return null
            val t = (-b - sqrt(disc)) / (2 * a)
            return if (t in 0.0..1.0) t else null
        }
    }
}
