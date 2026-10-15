package ro.klooseduard.streetsofsibiu.logic.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRandomTest {
    @Test
    fun randiRangeIsInclusiveAndUniform() {
        val random = GameRandom(42)
        val counts = IntArray(6)
        repeat(60_000) { counts[random.randiRange(1, 6) - 1]++ }
        counts.forEach { assertTrue("count $it", it in 9_400..10_600) }
    }

    @Test
    fun randiRangeAcceptsReversedBoundsAndEqualBounds() {
        val random = GameRandom(7)
        val values = List(2_000) { random.randiRange(5, 2) }.toSet()
        assertEquals(setOf(2, 3, 4, 5), values)
        assertEquals(3, random.randiRange(3, 3))
    }

    @Test
    fun randfStaysInUnitInterval() {
        val random = GameRandom(3)
        repeat(10_000) { assertTrue(random.randf() in 0.0..1.0) }
    }

    @Test
    fun randiIsUnsigned32Bit() {
        val random = GameRandom(9)
        repeat(10_000) { assertTrue(random.randi() in 0L..0xFFFF_FFFFL) }
    }

    @Test
    fun shuffleIsAPermutationAndEveryOrderAppears() {
        val random = GameRandom(11)
        val seen = mutableMapOf<List<Int>, Int>()
        repeat(60_000) {
            val list = mutableListOf(1, 2, 3)
            random.shuffle(list)
            seen.merge(list.toList(), 1, Int::plus)
        }
        assertEquals(6, seen.size)
        seen.values.forEach { assertTrue("count $it", it in 9_300..10_700) }
    }

    @Test
    fun seededGeneratorsAreReproducible() {
        val a = GameRandom(123)
        val b = GameRandom(123)
        assertEquals(List(50) { a.randiRange(0, 100) }, List(50) { b.randiRange(0, 100) })
    }
}
