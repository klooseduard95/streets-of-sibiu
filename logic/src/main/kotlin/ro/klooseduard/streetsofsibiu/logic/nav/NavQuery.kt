package ro.klooseduard.streetsofsibiu.logic.nav

import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val FLT_MAX = 3.4028234663852886e38
private const val CMP_EPSILON = 0.00001

private fun isEqualApprox(a: Double, b: Double): Boolean {
    if (a == b) return true
    var tolerance = CMP_EPSILON * abs(a)
    if (tolerance < CMP_EPSILON) tolerance = CMP_EPSILON
    return abs(a - b) < tolerance
}

private fun isEqualApprox(a: DVec, b: DVec) = isEqualApprox(a.x, b.x) && isEqualApprox(a.y, b.y)

private fun cross(a: DVec, b: DVec) = a.x * b.y - a.y * b.x

private fun threePointsCross(a: DVec, b: DVec, c: DVec) = -cross(c - a, b - a)

private fun f(x: Double): Double = x.toFloat().toDouble()

private fun fv(v: DVec) = DVec(f(v.x), f(v.y))

private fun dist(a: DVec, b: DVec): Double {
    val dx = f(a.x - b.x)
    val dy = f(a.y - b.y)
    return f(sqrt(f(f(dx * dx) + f(dy * dy))))
}

private fun distSq(a: DVec, b: DVec): Double {
    val d = a - b
    return d.x * d.x + d.y * d.y
}

private fun closestPointToSegment(p: DVec, a: DVec, b: DVec): DVec {
    val ab = b - a
    val denom = ab.dot(ab)
    if (denom == 0.0) return a
    val t = f((p - a).dot(ab) / denom)
    return when {
        t <= 0.0 -> a
        t >= 1.0 -> b
        else -> fv(a + ab * t)
    }
}

private fun closestPointToTriangle(v0: DVec, v1: DVec, v2: DVec, p: DVec): DVec {
    val edge0 = v1 - v0
    val edge1 = v2 - v0
    val vv = v0 - p
    val a = edge0.dot(edge0)
    val b = edge0.dot(edge1)
    val c = edge1.dot(edge1)
    val d = edge0.dot(vv)
    val e = edge1.dot(vv)
    val det = a * c - b * b
    var s = b * e - c * d
    var t = b * d - a * e
    fun clamp(x: Double) = x.coerceIn(0.0, 1.0)
    if (s + t < det) {
        if (s < 0.0) {
            if (t < 0.0) {
                if (d < 0.0) {
                    s = clamp(-d / a); t = 0.0
                } else {
                    s = 0.0; t = clamp(-e / c)
                }
            } else {
                s = 0.0; t = clamp(-e / c)
            }
        } else if (t < 0.0) {
            s = clamp(-d / a); t = 0.0
        } else {
            val inv = 1.0 / det
            s *= inv; t *= inv
        }
    } else {
        if (s < 0.0) {
            val tmp0 = b + d
            val tmp1 = c + e
            if (tmp1 > tmp0) {
                val numer = tmp1 - tmp0
                val denom = a - 2 * b + c
                s = clamp(numer / denom); t = 1 - s
            } else {
                t = clamp(-e / c); s = 0.0
            }
        } else if (t < 0.0) {
            if (a + d > b + e) {
                val numer = c + e - b - d
                val denom = a - 2 * b + c
                s = clamp(numer / denom); t = 1 - s
            } else {
                s = clamp(-d / a); t = 0.0
            }
        } else {
            val numer = c + e - b - d
            val denom = a - 2 * b + c
            s = clamp(numer / denom); t = 1.0 - s
        }
    }
    return v0 + edge0 * s + edge1 * t
}

private class CorridorPoly {
    var poly: NavPolygon? = null
    var heapIndex = -1
    var backId = -1
    var backEdge = -1
    var backStart = DVec.ZERO
    var backEnd = DVec.ZERO
    var entry = DVec.ZERO
    var traveled = 0.0
    var toDestination = 0.0

    val total: Double get() = f(traveled + toDestination)

    fun reset() {
        poly = null
        heapIndex = -1
        backId = -1
        backEdge = -1
        traveled = FLT_MAX
        toDestination = 0.0
    }
}

private class NavHeap {
    private val buffer = ArrayList<CorridorPoly>()

    val isEmpty get() = buffer.isEmpty()

    private fun lessThan(a: CorridorPoly, b: CorridorPoly): Boolean {
        val fa = a.total
        val fb = b.total
        return if (fa != fb) fa > fb else a.toDestination > b.toDestination
    }

    private fun setIndex(p: CorridorPoly, i: Int) {
        p.heapIndex = i
    }

    fun push(e: CorridorPoly) {
        buffer += e
        setIndex(e, buffer.size - 1)
        shiftUp(buffer.size - 1)
    }

    fun pop(): CorridorPoly {
        val value = buffer[0]
        setIndex(value, -1)
        if (buffer.size > 1) {
            buffer[0] = buffer[buffer.size - 1]
            setIndex(buffer[0], 0)
            buffer.removeAt(buffer.size - 1)
            shiftDown(0)
        } else {
            buffer.removeAt(buffer.size - 1)
        }
        return value
    }

    fun shift(index: Int) {
        if (!shiftUp(index)) shiftDown(index)
    }

    fun clear() {
        buffer.forEach { setIndex(it, -1) }
        buffer.clear()
    }

    private fun shiftUp(index: Int): Boolean {
        val value = buffer[index]
        var current = index
        var parent = (current - 1) / 2
        while (current > 0 && lessThan(buffer[parent], value)) {
            buffer[current] = buffer[parent]
            setIndex(buffer[current], current)
            current = parent
            parent = (current - 1) / 2
        }
        return if (current != index) {
            buffer[current] = value
            setIndex(value, current)
            true
        } else false
    }

    private fun shiftDown(index: Int): Boolean {
        val value = buffer[index]
        var current = index
        var child = 2 * current + 1
        while (child < buffer.size) {
            if (child + 1 < buffer.size && lessThan(buffer[child], buffer[child + 1])) child++
            if (lessThan(buffer[child], value)) break
            buffer[current] = buffer[child]
            setIndex(buffer[current], current)
            current = child
            child = 2 * current + 1
        }
        return if (current != index) {
            buffer[current] = value
            setIndex(value, current)
            true
        } else false
    }
}

class NavQuery(private val mesh: NavMesh) {
    fun path(start: DVec, target: DVec, maxPolygons: Int = 0, maxDistance: Double = 0.0): List<DVec> {
        var beginPoly: NavPolygon? = null
        var endPoly: NavPolygon? = null
        var beginD = FLT_MAX
        var endD = FLT_MAX
        var beginPos = DVec.ZERO
        var endPos = DVec.ZERO
        for (p in mesh.polygons) {
            for (i in 2 until p.vertices.size) {
                val v0 = p.vertices[0]
                val v1 = p.vertices[i - 1]
                val v2 = p.vertices[i]
                var point = closestPointToTriangle(v0, v1, v2, start)
                var d = dist(point, start)
                if (d < beginD) {
                    beginD = d; beginPoly = p; beginPos = point
                }
                point = closestPointToTriangle(v0, v1, v2, target)
                d = dist(point, target)
                if (d < endD) {
                    endD = d; endPoly = p; endPos = point
                }
            }
        }
        if (beginPoly == null || endPoly == null) return emptyList()
        if (beginPoly === endPoly) return listOf(beginPos, endPos)

        val polys = List(mesh.polygons.size) { CorridorPoly() }
        polys.forEach { it.reset() }
        val heap = NavHeap()
        val begin = polys[beginPoly.id]
        begin.poly = beginPoly
        begin.entry = beginPos
        begin.backStart = beginPos
        begin.backEnd = beginPos
        begin.traveled = 0.0

        var leastId = beginPoly.id
        var found = false
        var reachableEnd: NavPolygon? = null
        var distanceToReachableEnd = FLT_MAX
        var isReachable = true
        var searchMaxReached = false
        val maxDistSq = maxDistance * maxDistance
        val hasMaxDistance = maxDistSq > 0.0
        val hasMaxPolygons = maxPolygons > 0
        val hasMax = hasMaxPolygons || hasMaxDistance
        var processed = 0
        var curEnd: NavPolygon = endPoly
        var endPoint = endPos

        while (true) {
            val least = polys[leastId]
            processed++
            for (c in mesh.connections[least.poly!!.id]) {
                val newEntry = closestPointToSegment(least.entry, c.pathwayStart, c.pathwayEnd)
                val newTraveled = f(f(dist(least.entry, newEntry) + 0.0) + least.traveled)
                val neighbor = polys[c.polygon.id]
                if (newTraveled < neighbor.traveled) {
                    neighbor.backId = leastId
                    neighbor.backEdge = c.edge
                    neighbor.backStart = c.pathwayStart
                    neighbor.backEnd = c.pathwayEnd
                    neighbor.traveled = newTraveled
                    neighbor.toDestination = dist(newEntry, endPoint)
                    neighbor.entry = newEntry
                    if (neighbor.heapIndex != -1) {
                        heap.shift(neighbor.heapIndex)
                    } else {
                        neighbor.poly = c.polygon
                        heap.push(neighbor)
                    }
                }
            }
            if (hasMax && !searchMaxReached) {
                if (hasMaxPolygons && processed >= maxPolygons) {
                    searchMaxReached = true
                    heap.clear()
                } else if (hasMaxDistance && distSq(beginPos, least.entry) > maxDistSq) {
                    searchMaxReached = true
                    heap.clear()
                }
            }
            if (heap.isEmpty) {
                if (!isReachable) break
                isReachable = false
                val re = reachableEnd ?: break
                curEnd = re
                var bestD = FLT_MAX
                for (i in 2 until re.vertices.size) {
                    val sp = closestPointToTriangle(re.vertices[0], re.vertices[i - 1], re.vertices[i], target)
                    val d = distSq(sp, target)
                    if (d < bestD) {
                        endPoint = sp; bestD = d
                    }
                }
                var onStart = false
                for (i in 2 until beginPoly.vertices.size) {
                    val sp = closestPointToTriangle(beginPoly.vertices[0], beginPoly.vertices[i - 1], beginPoly.vertices[i], target)
                    val d = distSq(sp, target)
                    if (d < bestD) {
                        endPoint = sp; bestD = d; onStart = true
                    }
                }
                if (onStart) return listOf(beginPos, endPoint)
                polys.forEach {
                    it.poly = null
                    it.traveled = FLT_MAX
                }
                polys[beginPoly.id].poly = beginPoly
                polys[beginPoly.id].traveled = 0.0
                leastId = beginPoly.id
                reachableEnd = null
            } else {
                val popped = heap.pop()
                leastId = popped.poly!!.id
                if (isReachable) {
                    val d = distSq(polys[leastId].entry, target)
                    if (distanceToReachableEnd > d) {
                        distanceToReachableEnd = d
                        reachableEnd = polys[leastId].poly
                    }
                }
                if (polys[leastId].poly === curEnd) {
                    found = true
                    break
                }
            }
        }

        if (!found) {
            var bestD = FLT_MAX
            var ep = endPoint
            for (i in 2 until beginPoly.vertices.size) {
                val sp = closestPointToTriangle(beginPoly.vertices[0], beginPoly.vertices[i - 1], beginPoly.vertices[i], target)
                val d = distSq(sp, target)
                if (d < bestD) {
                    ep = sp; bestD = d
                }
            }
            return listOf(beginPos, ep)
        }

        val path = funnel(polys, leastId, endPoint, curEnd, beginPos, beginPoly)
        return path.reversed()
    }

    private fun funnel(polys: List<CorridorPoly>, leastId: Int, endPoint: DVec, endPoly: NavPolygon, beginPoint: DVec, beginPoly: NavPolygon): List<DVec> {
        val points = ArrayList<DVec>()
        var apexId = leastId
        val back = closestPointToSegment(endPoint, polys[apexId].backStart, polys[apexId].backEnd)
        if (isEqualApprox(endPoint, back)) {
            if (polys[apexId].backId != -1) apexId = polys[apexId].backId
        }
        var apexPoint = endPoint
        var leftId = apexId
        var leftPortal = apexPoint
        var rightId = apexId
        var rightPortal = apexPoint
        var pId = apexId
        points += endPoint

        fun clip(fromId: Int, to: DVec, toId: Int) {
            val from = points[points.size - 1]
            if (isEqualApprox(from, to)) return
            val diff = from - to
            val ortho = DVec(diff.y, -diff.x)
            val orthoLen = ortho.length()
            val normal = DVec(-ortho.x / orthoLen, -ortho.y / orthoLen)
            val d = normal.dot(from)
            var cur = fromId
            while (cur != toId) {
                val ps = polys[cur].backStart
                val pe = polys[cur].backEnd
                cur = polys[cur].backId
                if (!isEqualApprox(ps, pe)) {
                    val seg = ps - pe
                    val den = normal.dot(seg)
                    if (abs(den) >= CMP_EPSILON) {
                        val distance = (normal.dot(ps) - d) / den
                        if (distance >= -CMP_EPSILON && distance <= 1.0 + CMP_EPSILON) {
                            val inters = ps - seg * distance
                            if (!isEqualApprox(inters, to) && !isEqualApprox(inters, points[points.size - 1])) points += inters
                        }
                    }
                }
            }
        }

        while (pId != -1) {
            var left = polys[pId].backStart
            var right = polys[pId].backEnd
            if (threePointsCross(apexPoint, left, right) < 0) {
                val t = left; left = right; right = t
            }
            var skip = false
            if (threePointsCross(apexPoint, leftPortal, left) >= 0) {
                if (leftPortal == apexPoint || threePointsCross(apexPoint, left, rightPortal) > 0) {
                    leftId = pId
                    leftPortal = left
                } else {
                    clip(apexId, rightPortal, rightId)
                    apexPoint = rightPortal
                    pId = rightId
                    leftId = pId
                    apexId = pId
                    leftPortal = apexPoint
                    rightPortal = apexPoint
                    points += apexPoint
                    skip = true
                }
            }
            if (!skip && threePointsCross(apexPoint, rightPortal, right) <= 0) {
                if (rightPortal == apexPoint || threePointsCross(apexPoint, right, leftPortal) < 0) {
                    rightId = pId
                    rightPortal = right
                } else {
                    clip(apexId, leftPortal, leftId)
                    apexPoint = leftPortal
                    pId = leftId
                    rightId = pId
                    apexId = pId
                    rightPortal = apexPoint
                    leftPortal = apexPoint
                    points += apexPoint
                }
            }
            pId = if (polys[pId].backId != -1) polys[pId].backId else -1
        }
        if (points[points.size - 1] != beginPoint) points += beginPoint
        return points
    }
}
