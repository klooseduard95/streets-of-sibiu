package ro.klooseduard.streetsofsibiu.logic.runtime

import kotlin.random.Random

/**
 * The GDScript global random functions used by the original, with Godot's semantics
 * (core/math/random_pcg.cpp, core/variant/array.cpp).
 *
 * The generator differs from Godot's PCG32, and the original never seeds it, so sequences
 * cannot match the original run-for-run (difference D7). Ranges and algorithms do match.
 * Pass a seed in tests for reproducible results.
 */
class GameRandom(seed: Long = System.nanoTime()) {
    private val random = Random(seed)

    /** `randi()`: uniform 32-bit unsigned value, returned as a non-negative Long. */
    fun randi(): Long = random.nextInt().toLong() and 0xFFFF_FFFFL

    /** `randi_range(from, to)`: uniform and inclusive; the bounds may be given in either order. */
    fun randiRange(from: Int, to: Int): Int {
        if (from == to) return from
        val lo = minOf(from, to).toLong()
        val hi = maxOf(from, to).toLong()
        return (lo + random.nextLong(hi - lo + 1)).toInt()
    }

    /** `randf()`: uniform in [0, 1]. (Godot documents [0, 1]; hitting exactly 1.0 has negligible probability.) */
    fun randf(): Double = random.nextDouble()

    /** `randf_range(from, to)`. */
    fun randfRange(from: Double, to: Double): Double = randf() * (to - from) + from

    /** `Array.shuffle()`: Fisher–Yates from the end, `j = randi() % (i + 1)` (Godot's exact algorithm). */
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.size - 1 downTo 1) {
            val j = (randi() % (i + 1)).toInt()
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}
