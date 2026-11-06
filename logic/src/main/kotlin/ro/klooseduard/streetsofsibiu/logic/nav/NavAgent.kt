package ro.klooseduard.streetsofsibiu.logic.nav

import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import kotlin.math.sqrt

class NavAgent(
    private val query: NavQuery,
    private val position: () -> Vec2,
    private val maxPolygons: Int = 512,
    private val maxSearchDistance: Double = 500.0,
    private val pathDesiredDistance: Double = 20.0,
    private val targetDesiredDistance: Double = 10.0,
    private val pathMaxDistance: Double = 100.0,
) {
    private var target = DVec.ZERO
    private var submitted = false
    private var path: List<DVec> = emptyList()
    private var index = 0
    private var finished = false
    private var lastWaypointReached = false

    fun setTarget(p: Vec2) {
        target = DVec.of(p)
        submitted = true
        path = emptyList()
        finished = false
        lastWaypointReached = false
    }

    fun nextPathPosition(): Vec2 {
        update()
        return if (path.isEmpty()) position() else path[index].toVec2()
    }

    fun isNavigationFinished(): Boolean {
        update()
        return finished
    }

    private fun dist(a: DVec, b: DVec): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun closestOnSegment(p: DVec, a: DVec, b: DVec): DVec {
        val ab = b - a
        val denom = ab.dot(ab)
        if (denom == 0.0) return a
        val t = ((p - a).dot(ab) / denom).coerceIn(0.0, 1.0)
        return a + ab * t
    }

    private fun update() {
        if (!submitted) return
        val origin = DVec.of(position())
        var reload = false
        if (path.isEmpty()) {
            reload = true
        } else if (index > 0) {
            val p = closestOnSegment(origin, path[index - 1], path[index])
            if (dist(origin, p) >= pathMaxDistance) reload = true
        }
        if (reload) {
            path = query.path(origin, target, maxPolygons, maxSearchDistance)
            finished = false
            lastWaypointReached = false
            index = 0
        }
        if (path.isEmpty()) return
        if (finished) return
        if (dist(origin, target) < targetDesiredDistance) {
            advance(origin)
            finished = true
        } else {
            advance(origin)
            if (lastWaypointReached && targetDesiredDistance < dist(path.last(), target)) finished = true
        }
    }

    private fun advance(origin: DVec) {
        if (lastWaypointReached) return
        while (dist(origin, path[index]) < pathDesiredDistance) {
            if (index == path.size - 1) {
                lastWaypointReached = true
                break
            }
            index++
        }
    }
}
