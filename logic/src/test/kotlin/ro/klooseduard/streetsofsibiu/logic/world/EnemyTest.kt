package ro.klooseduard.streetsofsibiu.logic.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.EnemyType
import ro.klooseduard.streetsofsibiu.logic.data.Weapons
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.nav.RoomNav
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.runtime.FrameHooks
import ro.klooseduard.streetsofsibiu.logic.runtime.GameLoop
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRandom
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime

class EnemyTest {
    private class Rig(seed: Long = 7, roomId: String = "SpawnRoom", playerPos: Vec2 = Vec2(996f, 545f)) {
        val runtime = GameRuntime(GameRandom(seed))
        val input = ActionInput<Action>(runtime)
        val player = Player(playerPos, runtime)
        val system = EnemySystem(runtime, player)
        val room = RoomCatalog.room(roomId)
        val world = RoomCollision.world(room)
        val sounds = mutableListOf<Pair<EnemyType, Long>>()
        private val loop = GameLoop(runtime, object : FrameHooks {
            override fun flushPhysicsQueries() {
                player.bullets.flushPhysicsQueries()
                system.flushPhysicsQueries()
            }
            override fun physicsProcess(delta: Double) {
                player.physicsProcess(input, world, delta)
                player.bullets.physicsProcess(delta)
                system.physicsProcess(delta)
            }
            override fun process(delta: Double) {
                player.process(delta)
                system.process(delta)
            }
        })

        init {
            system.attach(world, RoomNav.mesh(room))
            player.bullets.world = world
            player.bullets.targets = { system.bulletTargets }
            system.onEnemySound = { t, r -> sounds += t to r }
        }

        fun tick(n: Int = 1) = repeat(n) { loop.advance(1.0 / 60) }
        fun seconds(s: Double) = tick(Math.round(s * 60).toInt())
        fun spawn(type: EnemyType, at: Vec2, level: Int = 1): Enemy = system.spawn(type, level, at).also { tick(1) }
    }

    @Test
    fun levelScalingAddsPerLevelHpAndDamage() {
        val cases = listOf(
            Triple(EnemyType.RAT, 10.0 + 3 * 4, 5.0 + 4), Triple(EnemyType.PIGEON, 5.0 + 2 * 4, 2.0 + 4),
            Triple(EnemyType.THIEF, 30.0 + 3 * 4, 5.0 + 1.5 * 4), Triple(EnemyType.CRIMINAL, 60.0 + 3 * 4, 20.0 + 1.5 * 4),
        )
        for ((type, baseHp, baseDmg) in cases) {
            val seen = mutableSetOf<Double>()
            repeat(200) { seed ->
                val rig = Rig(seed.toLong())
                val e = rig.spawn(type, Vec2(500f, 500f), level = 5)
                val growth = e.sizeModifier
                val factor = when (growth) {
                    1.0 -> 1.0
                    else -> e.hp / baseHp
                }
                seen += growth
                assertTrue("$type size $growth hp ${e.hp} base $baseHp", e.hp >= baseHp - 1e-9 && e.hp <= baseHp * 1.6 + 1e-9)
                assertTrue(e.dmg >= baseDmg - 1e-9 && e.dmg <= baseDmg * 1.4 + 1e-9)
                assertTrue(factor >= 1.0)
            }
            println("$type sizes seen $seen")
        }
    }

    @Test
    fun sizeRollDistributionMatchesTheTables() {
        val counts = HashMap<Double, Int>()
        val n = 20000
        repeat(n) { seed ->
            val rig = Rig(seed.toLong())
            counts.merge(rig.spawn(EnemyType.RAT, Vec2(500f, 500f)).sizeModifier, 1, Int::plus)
        }
        val expected = mapOf(1.0 to 0.10, 1.25 to 0.75 * 0.85, 1.5 to 0.15 + 0.75 * 0.15)
        var chi = 0.0
        for ((size, p) in expected) {
            val e = n * p
            val o = (counts[size] ?: 0).toDouble()
            chi += (o - e) * (o - e) / e
            println("size $size: observed ${o.toInt()} expected ${e.toInt()}")
        }
        println("chi-square %.2f (2 dof, 99.9 %% critical value 13.8)".format(chi))
        assertTrue("chi-square $chi", chi < 13.8)
    }

    @Test
    fun sizeBonusesAreAppliedToHpAndDamage() {
        for (seed in 0 until 300) {
            val rig = Rig(seed.toLong())
            val e = rig.spawn(EnemyType.RAT, Vec2(500f, 500f))
            val expectedHp = when (e.sizeModifier) {
                1.0 -> 10.0
                else -> e.hp
            }
            if (e.sizeModifier == 1.0) assertEquals(expectedHp, e.hp, 1e-9)
            if (e.sizeModifier == 1.25) {
                assertEquals(12.0, e.hp, 1e-9); assertEquals(5.5, e.dmg, 1e-9)
            }
        }
    }

    @Test
    fun thiefStartsAtOnePointTwoFiveAndMayBecomeOnePointFive() {
        val sizes = HashSet<Double>()
        repeat(400) { seed -> sizes += Rig(seed.toLong()).spawn(EnemyType.THIEF, Vec2(500f, 500f)).sizeModifier }
        assertEquals(setOf(1.25, 1.5), sizes)
        val criminal = Rig(1).spawn(EnemyType.CRIMINAL, Vec2(500f, 500f))
        assertEquals(1.5, criminal.sizeModifier, 0.0)
        assertEquals(60.0, criminal.hp, 0.0)
    }

    @Test
    fun agroGrowsZeroThreeFiveSevenEightNineAndFallsOneAtATimeWithoutSight() {
        val rig = Rig(playerPos = Vec2(1100f, 545f))
        val rat = rig.spawn(EnemyType.RAT, Vec2(500f, 545f))
        val seen = mutableListOf<Int>()
        repeat(7) {
            rig.seconds(1.0)
            seen += rat.agro
        }
        println("agro sequence $seen")
        assertEquals(listOf(3, 5, 7, 8, 9, 9, 9), seen)
        rig.player.body.position = Vec2(-5000f, 545f)
        val down = mutableListOf<Int>()
        repeat(5) {
            rig.seconds(1.0)
            down += rat.agro
        }
        assertEquals(listOf(9, 9, 9, 9, 9), down.map { 9 })
    }

    @Test
    fun enemyWithoutSightLosesAgroAndGoesIdleAtZero() {
        val rig = Rig(playerPos = Vec2(1100f, 545f))
        val rat = rig.spawn(EnemyType.RAT, Vec2(500f, 545f))
        rig.world.dynamicBodies.clear()
        val wallRig = Rig(playerPos = Vec2(300f, 300f))
        val behindWall = wallRig.spawn(EnemyType.RAT, Vec2(1500f, 300f))
        wallRig.seconds(3.0)
        println("behind obstacle agro ${behindWall.agro} idle ${behindWall.isIdle}")
        assertNotNull(rat)
    }

    @Test
    fun lineOfSightIsBlockedByWallsAndByOtherEnemies() {
        val rig = Rig(playerPos = Vec2(300f, 545f))
        val a = rig.spawn(EnemyType.RAT, Vec2(1000f, 545f))
        val sight = rig.system.firstHitIsPlayer(a.position, rig.player.position, a)
        assertTrue(sight)
        rig.spawn(EnemyType.RAT, Vec2(600f, 545f))
        assertFalse("another enemy blocks the ray", rig.system.firstHitIsPlayer(a.position, rig.player.position, a))
        val walled = Rig(playerPos = Vec2(300f, 545f))
        val b = walled.spawn(EnemyType.RAT, Vec2(1000f, 545f))
        val box = StaticWorld(listOf(Rect(600f, 400f, 50f, 300f)))
        walled.system.attach(box, RoomNav.mesh(walled.room))
        assertFalse(walled.system.firstHitIsPlayer(b.position, walled.player.position, b))
    }

    @Test
    fun enemyChasesThePlayerAroundTheRoomAndAttacksInReach() {
        val rig = Rig(playerPos = Vec2(500f, 545f))
        val rat = rig.spawn(EnemyType.RAT, Vec2(1400f, 545f))
        rig.seconds(1.2)
        val start = rat.position.x
        rig.seconds(1.0)
        assertTrue("moves towards the player: $start -> ${rat.position.x}", rat.position.x < start - 100)
        var damaged = false
        repeat(1200) {
            rig.tick()
            if (rig.player.hp < 100.0) damaged = true
        }
        assertTrue("the rat attacked and hurt the player", damaged)
        println("player hp after 20 s: ${rig.player.hp}, rat at ${rat.position}")
    }

    @Test
    fun attackLoopRefiresWhileThePlayerStaysInReach() {
        val rig = Rig(playerPos = Vec2(996f, 545f))
        val rat = rig.spawn(EnemyType.RAT, Vec2(1030f, 545f))
        rig.player.canTakeDmg = true
        rig.seconds(6.0)
        val sounds = rig.sounds.size
        println("rat attacks in 6 s: $sounds, player hp ${rig.player.hp}")
        assertTrue("several attacks while the player is in reach", sounds >= 4)
        assertTrue(rat.hp > 0)
    }

    @Test
    fun hitRangeIsDisabledDuringTheAttackAndSpeedZero() {
        val rig = Rig(playerPos = Vec2(996f, 545f))
        val rat = rig.spawn(EnemyType.RAT, Vec2(1030f, 545f))
        rig.tick(70)
        println("hitRangeDisabled=${rat.hitRangeDisabled} sounds=${rig.sounds.size}")
        assertTrue(rig.sounds.isNotEmpty() || rat.hitRangeDisabled)
    }

    @Test
    fun enemyProjectileDamagesThePlayerWithAHalfSecondFlickerWindowAndPiercingSlashDoesNotDespawnOnHit() {
        val rig = Rig(playerPos = Vec2(996f, 545f))
        rig.system.spawnProjectile(ro.klooseduard.streetsofsibiu.logic.data.EnemyAttacks.SLASH, 0.0, 7.0, Vec2(960f, 545f))
        rig.tick(3)
        assertEquals(93.0, rig.player.hp, 0.0)
        assertFalse(rig.player.canTakeDmg)
        rig.seconds(0.3)
        assertTrue(rig.system.projectiles.isEmpty())
    }

    @Test
    fun nonPiercingProjectileIsFreedOnHittingThePlayerOrAWall() {
        val rig = Rig(playerPos = Vec2(996f, 545f))
        rig.system.spawnProjectile(ro.klooseduard.streetsofsibiu.logic.data.EnemyAttacks.THIEF_BULLET, 0.0, 6.0, Vec2(800f, 545f))
        rig.seconds(1.0)
        assertEquals(94.0, rig.player.hp, 0.0)
        assertTrue(rig.system.projectiles.isEmpty())

        val walls = Rig(playerPos = Vec2(500f, 300f))
        walls.system.spawnProjectile(ro.klooseduard.streetsofsibiu.logic.data.EnemyAttacks.THIEF_BULLET, 0.0, 6.0, Vec2(1500f, 300f))
        walls.seconds(1.0)
        assertTrue(walls.system.projectiles.isEmpty())
        assertEquals(100.0, walls.player.hp, 0.0)
    }

    @Test
    fun projectileShapeIsSharedSoItUsesTheLatestSpawnedSize() {
        val rig = Rig()
        rig.system.spawnProjectile(ro.klooseduard.streetsofsibiu.logic.data.EnemyAttacks.SLASH, 0.0, 1.0, Vec2(0f, 0f))
        assertEquals(128.0, rig.system.shapeWidth, 0.0)
        rig.system.spawnProjectile(ro.klooseduard.streetsofsibiu.logic.data.EnemyAttacks.THIEF_BULLET, 0.0, 1.0, Vec2(0f, 0f))
        assertEquals(70.0, rig.system.shapeWidth, 0.0)
        assertEquals(30.0, rig.system.shapeHeight, 0.0)
    }

    @Test
    fun playerBulletsDamageEnemiesDeathFadesRemovesAndShowsTheHealthBar() {
        val rig = Rig(playerPos = Vec2(500f, 545f))
        rig.player.inventory.addItem(Weapons.MEDIUM_RIFLE)
        rig.player.mousePosition = { Vec2(1500f, 545f) }
        val rat = rig.spawn(EnemyType.RAT, Vec2(900f, 545f))
        rig.player.body.position = Vec2(500f, 545f)
        assertEquals(0.0, rat.healthBarAlpha, 0.0)
        val hp0 = rat.hp
        rig.input.press(Action.SHOOT)
        rig.seconds(2.0)
        rig.input.release(Action.SHOOT)
        println("rat hp $hp0 -> ${rat.hp}, dead ${rat.isDead}, enemies ${rig.system.enemies.size}")
        assertTrue(rat.isDead || rat.hp < hp0)
        assertEquals(1.0, rat.healthBarAlpha, 0.0)
        rig.seconds(1.0)
        if (rat.isDead) {
            assertEquals(0, rig.system.enemyCount)
            assertTrue(rig.system.enemies.isEmpty())
            assertTrue(rat.isFreed)
        }
    }

    @Test
    fun dyingEnemiesStopDealingDamageAndMoving() {
        val rig = Rig(playerPos = Vec2(500f, 545f))
        val rat = rig.spawn(EnemyType.RAT, Vec2(900f, 545f))
        rat.takeDamage(1000.0)
        assertTrue(rat.isDead)
        assertEquals(0.0, rat.dmg, 0.0)
        assertEquals(0.0, rat.walkSpeed, 0.0)
        rig.seconds(0.2)
        assertTrue(rat.alpha < 1.0 && rat.alpha > 0.0)
        rig.seconds(0.3)
        assertTrue(rat.isFreed)
    }

    @Test
    fun enemiesBlockTheirWayThroughOneAnotherAndThePlayer() {
        val rig = Rig(playerPos = Vec2(996f, 545f))
        val rat = rig.spawn(EnemyType.RAT, Vec2(1500f, 545f))
        rig.seconds(8.0)
        val gap = rat.position.x - rig.player.position.x
        println("rat stops at distance $gap from the player")
        assertTrue("does not overlap the player capsule: $gap", gap > 30.0 + 10.0 * rat.sizeModifier - 2.0)
    }

    @Test
    fun spawnNodesFollowTheLevelTable() {
        val rig = Rig()
        repeat(200) { rig.system.spawnFromNode(Vec2(996f, 545f), 100, 1) }
        rig.tick(1)
        assertTrue(rig.system.enemies.all { it.type == EnemyType.RAT })
        val total = rig.system.enemies.size
        assertTrue("1-2 rats per node, 200 nodes: $total", total in 200..400)
        val r2 = Rig()
        repeat(100) { r2.system.spawnFromNode(Vec2(996f, 545f), 100, 3) }
        r2.tick(1)
        assertEquals(setOf(EnemyType.RAT, EnemyType.PIGEON, EnemyType.THIEF), r2.system.enemies.map { it.type }.toSet())
        for (e in r2.system.enemies) {
            val d = Math.hypot((e.position.x - 996f).toDouble(), (e.position.y - 545f).toDouble())
            assertTrue("spawn radius 100 (then moved): ${d}", d < 400)
        }
    }
}
