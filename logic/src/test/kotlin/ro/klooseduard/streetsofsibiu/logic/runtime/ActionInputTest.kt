package ro.klooseduard.streetsofsibiu.logic.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionInputTest {
    private val runtime = GameRuntime(GameRandom(1))
    private val input = ActionInput<String>(runtime)
    private val seen = mutableListOf<Boolean>()
    private var pressDuringTick = -1
    private val loop = GameLoop(runtime, object : FrameHooks {
        override fun physicsProcess(delta: Double) {
            if (runtime.physicsFrames.toInt() == pressDuringTick) input.press("blink")
            seen += input.isJustPressed("blink")
        }
    })

    @Test
    fun pressBetweenTicksIsJustPressedInTheNextTickOnly() {
        loop.advance(1.0 / 60)
        input.press("blink")
        repeat(4) { loop.advance(1.0 / 60) }
        assertEquals(listOf(false, true, false, false, false), seen)
    }

    @Test
    fun pressAndReleaseBeforeTheTickStillCountsOnce() {
        input.press("blink")
        input.release("blink")
        repeat(3) { loop.advance(1.0 / 60) }
        assertEquals(listOf(true, false, false), seen)
        assertEquals(false, input.isPressed("blink"))
    }

    @Test
    fun pressDuringATickIsSeenInTheFollowingTick() {
        pressDuringTick = 2
        repeat(5) { loop.advance(1.0 / 60) }
        assertEquals(listOf(false, false, true, false, false), seen)
    }

    @Test
    fun pressWithoutTickOnFastDisplayWaitsForTheNextTick() {
        input.press("blink")
        loop.advance(1.0 / 240)
        loop.advance(1.0 / 240)
        loop.advance(1.0 / 240)
        loop.advance(1.0 / 240)
        assertEquals(listOf(true), seen)
    }

    @Test
    fun heldStateIsImmediate() {
        input.press("move_rightward")
        assertEquals(true, input.isPressed("move_rightward"))
        input.release("move_rightward")
        assertEquals(false, input.isPressed("move_rightward"))
    }

    @Test
    fun repeatedPressWhileHeldIsNotANewEdge() {
        input.press("blink")
        loop.advance(1.0 / 60)
        input.press("blink")
        loop.advance(1.0 / 60)
        assertEquals(listOf(true, false), seen)
    }
}
