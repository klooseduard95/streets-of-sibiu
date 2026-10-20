package ro.klooseduard.streetsofsibiu.logic.runtime

import kotlin.random.Random

class GameRandom(seed: Long = System.nanoTime()) {
    private val random = Random(seed)

    fun randi(): Long = random.nextInt().toLong() and 0xFFFF_FFFFL

    fun randiRange(from: Int, to: Int): Int {
        if (from == to) return from
        val lo = minOf(from, to).toLong()
        val hi = maxOf(from, to).toLong()
        return (lo + random.nextLong(hi - lo + 1)).toInt()
    }

    fun randf(): Double = random.nextDouble()

    fun randfRange(from: Double, to: Double): Double = randf() * (to - from) + from

    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.size - 1 downTo 1) {
            val j = (randi() % (i + 1)).toInt()
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}
