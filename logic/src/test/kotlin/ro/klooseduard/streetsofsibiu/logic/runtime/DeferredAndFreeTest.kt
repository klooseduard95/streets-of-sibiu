package ro.klooseduard.streetsofsibiu.logic.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeferredAndFreeTest {
    private val runtime = GameRuntime(GameRandom(1))

    @Test
    fun deferredCallsRunInOrderAtFlushIncludingNestedOnes() {
        val log = mutableListOf<String>()
        runtime.callDeferred { log += "a"; runtime.callDeferred { log += "nested" } }
        runtime.callDeferred { log += "b" }
        assertEquals(emptyList<String>(), log)
        runtime.flushDeferred()
        assertEquals(listOf("a", "b", "nested"), log)
    }

    @Test
    fun deferredCallFromPhysicsProcessRunsBeforeThePhysicsStep() {
        val log = mutableListOf<String>()
        val loop = GameLoop(runtime, object : FrameHooks {
            override fun physicsProcess(delta: Double) { runtime.callDeferred { log += "deferred add_child" } }
            override fun stepPhysics(delta: Double) { log += "physics step" }
        })
        loop.advance(1.0 / 60)
        assertEquals(listOf("deferred add_child", "physics step"), log)
    }

    @Test
    fun queueFreeTakesEffectAtTheEndOfTheTickNotImmediately() {
        val node = SceneNode()
        var freedDuringTick: Boolean? = null
        var listenerCalled = false
        node.onFreed { listenerCalled = true }
        val loop = GameLoop(runtime, object : FrameHooks {
            override fun physicsProcess(delta: Double) {
                runtime.queueFree(node)
                freedDuringTick = node.isFreed
            }
        })
        loop.advance(1.0 / 60)
        assertEquals(false, freedDuringTick)
        assertTrue(node.isFreed)
        assertTrue(listenerCalled)
    }

    @Test
    fun childrenOfDetachedOrFreedParentsAreOutsideTheTree() {
        val room = SceneNode()
        val enemy = SceneNode(parent = room)
        assertTrue(enemy.isInsideTree)
        room.setAttached(false)
        assertFalse(enemy.isInsideTree)
        room.setAttached(true)
        room.free()
        assertFalse(enemy.isInsideTree)
        assertTrue(enemy.isFreedOrOrphaned)
    }

    @Test
    fun processModesResolveThroughParents() {
        val root = SceneNode()
        val menu = SceneNode(ProcessMode.ALWAYS, root)
        val button = SceneNode(parent = menu)
        val room = SceneNode(ProcessMode.DISABLED, root)
        val enemy = SceneNode(parent = room)
        assertTrue(button.canProcess(treePaused = true))
        assertFalse(enemy.canProcess(treePaused = false))
        assertFalse(root.canProcess(treePaused = true))
        assertTrue(root.canProcess(treePaused = false))
    }
}
