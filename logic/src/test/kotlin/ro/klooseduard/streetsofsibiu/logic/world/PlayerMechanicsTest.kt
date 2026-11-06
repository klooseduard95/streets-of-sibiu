package ro.klooseduard.streetsofsibiu.logic.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.DashType
import ro.klooseduard.streetsofsibiu.logic.data.ParryType
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.runtime.FrameHooks
import ro.klooseduard.streetsofsibiu.logic.runtime.GameLoop
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRandom
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime
import kotlin.math.abs

class PlayerMechanicsTest {
    private class Rig(dash: DashType = DashType.NONE, parry: ParryType? = null, start: Vec2 = Vec2(500f, 550f)) {
        val runtime = GameRuntime(GameRandom(1))
        val input = ActionInput<Action>(runtime)
        val player = Player(start, runtime).also { it.selectedDash = dash; it.selectedParry = parry }
        val world = RoomCollision.world(RoomCatalog.room("SpawnRoom"))
        val sounds = mutableListOf<PlayerSound>()
        private val loop = GameLoop(runtime, object : FrameHooks {
            override fun physicsProcess(delta: Double) = player.physicsProcess(input, world, delta)
            override fun process(delta: Double) = player.process(delta)
        })

        init {
            player.onSound = { sounds += it }
        }

        fun tick(n: Int = 1) = repeat(n) { loop.advance(1.0 / 60) }
        fun seconds(s: Double) = tick(Math.round(s * 60).toInt())
        fun hold(vararg actions: Action) = actions.forEach(input::press)
        fun release(vararg actions: Action) = actions.forEach(input::release)
        fun tap(action: Action) {
            input.press(action)
            tick()
            input.release(action)
        }
        fun untilSlideEnds(limit: Int = 200) {
            var n = 0
            while (player.isSliding && n++ < limit) tick()
        }
    }

    @Test
    fun shortDashTeleportsTwoHundredPixelsThenSlidesAboutTwoHundredFortyAndLasts0Point4Seconds() {
        val rig = Rig(DashType.SHORT)
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(2)
        rig.release(Action.MOVE_RIGHTWARD)
        rig.hold(Action.MOVE_RIGHTWARD)
        val before = rig.player.position.x
        rig.tap(Action.BLINK)
        assertTrue(rig.player.isSliding)
        var ticks = 0
        while (rig.player.isSliding) {
            rig.tick()
            ticks++
        }
        val travelled = rig.player.position.x - before
        println("short dash total %.2f px in %d ticks".format(travelled, ticks))
        assertEquals(440.0, travelled.toDouble(), 20.0)
        assertTrue("slide ticks $ticks", ticks in 22..26)
    }

    @Test
    fun dashCooldownsMatchTheTable() {
        for ((dash, cooldown) in listOf(DashType.SHORT to 1.0, DashType.MEDIUM to 0.5, DashType.LONG to 2.0)) {
            val rig = Rig(dash)
            rig.player.mousePosition = { Vec2(900f, 550f) }
            rig.hold(Action.MOVE_RIGHTWARD)
            rig.tick()
            rig.tap(Action.BLINK)
            assertEquals("$dash cooldown", cooldown, rig.player.dashCooldownLeft, 0.05)
            var elapsed = 1
            while (rig.player.isSliding) {
                rig.tick()
                elapsed++
            }
            if (rig.player.dashCooldownLeft > 0) {
                rig.tap(Action.BLINK)
                elapsed++
                assertFalse("$dash: no new dash while cooling down", rig.player.isSliding)
            }
            while (rig.player.dashCooldownLeft > 0) {
                rig.tick()
                elapsed++
            }
            assertEquals("$dash cooldown length", cooldown, elapsed / 60.0, 0.06)
            rig.tap(Action.BLINK)
            assertTrue("$dash: dash available again", rig.player.isSliding)
        }
    }

    @Test
    fun noneDashIsASpammableMiniSlideWithNoCooldownAndNoTeleport() {
        val rig = Rig(DashType.NONE)
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(3)
        val before = rig.player.position.x
        rig.tap(Action.BLINK)
        assertTrue(rig.player.isSliding)
        assertTrue(rig.player.dashCooldownLeft <= 0)
        rig.untilSlideEnds()
        val travelled = rig.player.position.x - before
        println("none dash %.1f px".format(travelled))
        assertEquals(42.0, travelled.toDouble(), 12.0)
        assertTrue(rig.player.canTakeDmg)
        rig.tap(Action.BLINK)
        assertTrue("second dash straight away", rig.player.isSliding)
    }

    @Test
    fun dashWithoutDirectionDoesNotTeleportButStillStartsASlideForShortAndMedium() {
        val rig = Rig(DashType.SHORT)
        rig.tick(2)
        val before = rig.player.position
        rig.tap(Action.BLINK)
        assertEquals(before, rig.player.position)
        assertTrue(rig.player.isSliding)
        assertTrue(rig.player.dashCooldownLeft <= 0)
        rig.untilSlideEnds()
        assertTrue(rig.player.canTakeDmg)
    }

    @Test
    fun shortDashIsInvulnerableWhileSlidingWithBlueTintThenWhiteAgain() {
        val rig = Rig(DashType.SHORT)
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(2)
        rig.tap(Action.BLINK)
        assertFalse(rig.player.canTakeDmg)
        rig.tick(8)
        assertEquals(Rgba.DASH_BLUE.b, rig.player.modulate.b, 0.001)
        assertEquals(0.6, rig.player.modulate.a, 0.001)
        rig.untilSlideEnds()
        assertTrue(rig.player.canTakeDmg)
        rig.tick(10)
        assertEquals(Rgba.WHITE, rig.player.modulate)
    }

    @Test
    fun mediumDashB2FixInvulnerabilityEndsWithTheSlideAndThereIsNoBlueTint() {
        val rig = Rig(DashType.MEDIUM)
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(2)
        rig.tap(Action.BLINK)
        assertFalse(rig.player.canTakeDmg)
        rig.untilSlideEnds()
        assertTrue("B2: can_take_dmg restored when the slide ends", rig.player.canTakeDmg)
        assertEquals(Rgba.WHITE, rig.player.modulate)
        rig.player.takeDmg(10.0)
        assertEquals(90.0, rig.player.hp, 0.0)
    }

    @Test
    fun mediumDashAddsTwoAmmoCappedAtBaseCapacity() {
        val rig = Rig(DashType.MEDIUM)
        val gun = HeldWeapon(currentAmmo = 10, ammoCapacity = 15, dmg = 4.0)
        rig.player.heldItem = gun
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(2)
        rig.tap(Action.BLINK)
        assertEquals(12, gun.currentAmmo)
        rig.untilSlideEnds()
        rig.seconds(0.6)
        rig.tap(Action.BLINK)
        assertEquals(14, gun.currentAmmo)
        rig.untilSlideEnds()
        rig.seconds(0.6)
        rig.tap(Action.BLINK)
        assertEquals(15, gun.currentAmmo)
        val punch = HeldWeapon(currentAmmo = -1, ammoCapacity = -1, dmg = 2.0)
        val rig2 = Rig(DashType.MEDIUM)
        rig2.player.heldItem = punch
        rig2.hold(Action.MOVE_RIGHTWARD)
        rig2.tick(2)
        rig2.tap(Action.BLINK)
        assertEquals(-1, punch.currentAmmo)
    }

    @Test
    fun longDashTeleportsToTheCursorWithoutDirectionSlowsDownForOneSecondAndGivesNoInvulnerability() {
        val rig = Rig(DashType.LONG)
        rig.player.mousePosition = { Vec2(1200f, 700f) }
        rig.tick(2)
        rig.tap(Action.BLINK)
        assertEquals(1200f, rig.player.position.x, 1.0f)
        assertEquals(700f, rig.player.position.y, 1.0f)
        assertTrue(rig.player.canTakeDmg)
        assertEquals(PlayerDefs_BASE * 0.66, rig.player.speed, 0.001)
        rig.untilSlideEnds()
        assertTrue(rig.player.canTakeDmg)
        rig.seconds(0.8)
        assertEquals(PlayerDefs_BASE * 0.66, rig.player.speed, 0.001)
        rig.seconds(0.4)
        assertEquals(PlayerDefs_BASE, rig.player.speed, 0.001)
    }

    @Test
    fun dashPlaysTheSoundEvenWhenNothingHappens() {
        val rig = Rig(DashType.SHORT)
        rig.tap(Action.BLINK)
        assertEquals(listOf(PlayerSound.DASH), rig.sounds)
    }

    @Test
    fun ghostTrailStartsWithOneGhostThenOneEvery0Point15SecondsAndEachFadesOutIn0Point75Seconds() {
        val rig = Rig(DashType.SHORT)
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(2)
        rig.tap(Action.BLINK)
        assertEquals("immediate ghost", 1, rig.player.ghosts.size)
        val first = rig.player.ghosts.first()
        assertTrue(first.alpha > 0.9)
        rig.seconds(0.3)
        assertTrue("ghosts while sliding ${rig.player.ghosts.size}", rig.player.ghosts.size in 2..3)
        assertTrue(first.alpha < 0.9 && first.alpha > 0.0)
        rig.untilSlideEnds()
        val spawned = rig.player.ghosts.size
        rig.seconds(0.3)
        assertEquals("no new ghosts after the slide", spawned, rig.player.ghosts.size.coerceAtMost(spawned))
        rig.seconds(1.0)
        assertTrue("all ghosts faded and removed", rig.player.ghosts.isEmpty())
        assertTrue(first.isFreed)
    }

    @Test
    fun parryDoesNothingUntilUnlocked() {
        val rig = Rig(parry = null)
        rig.tap(Action.PARRY)
        assertTrue(rig.player.canTakeDmg)
        assertTrue(rig.sounds.isEmpty())
        assertTrue(rig.player.parryCooldownLeft <= 0)
    }

    @Test
    fun normalParryInvulnerableFor0Point2SecondsCooldownOneSecondGreenFlash() {
        val rig = Rig(parry = ParryType.NORMAL)
        rig.tap(Action.PARRY)
        assertEquals(listOf(PlayerSound.PARRY), rig.sounds)
        assertFalse(rig.player.canTakeDmg)
        assertEquals(1.0, rig.player.parryCooldownLeft, 0.05)
        rig.tick(4)
        assertTrue("green during flash", rig.player.modulate.g == 1.0 && rig.player.modulate.r < 1.0)
        rig.seconds(0.25)
        assertTrue(rig.player.canTakeDmg)
        rig.tap(Action.PARRY)
        assertEquals("cooling down: no second parry", 1, rig.sounds.size)
        rig.seconds(0.9)
        rig.tap(Action.PARRY)
        assertEquals(2, rig.sounds.size)
    }

    @Test
    fun parryCannotBeStartedWhileSliding() {
        val rig = Rig(DashType.SHORT, ParryType.NORMAL)
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(2)
        rig.tap(Action.BLINK)
        rig.tap(Action.PARRY)
        assertEquals(listOf(PlayerSound.DASH), rig.sounds)
    }

    @Test
    fun mediumParrySniperRefillsOtherGunsGainTenUncapped() {
        val sniper = HeldWeapon(currentAmmo = 0, ammoCapacity = 1, dmg = 25.0, isSniper = true)
        val rig = Rig(parry = ParryType.MEDIUM)
        rig.player.heldItem = sniper
        rig.tap(Action.PARRY)
        assertEquals(1, sniper.currentAmmo)
        assertEquals(1.5, rig.player.parryCooldownLeft, 0.05)
        assertTrue(abs(rig.player.modulate.b) < 1.0)

        val smg = HeldWeapon(currentAmmo = 25, ammoCapacity = 30, dmg = 3.0)
        val rig2 = Rig(parry = ParryType.MEDIUM)
        rig2.player.heldItem = smg
        rig2.tap(Action.PARRY)
        assertEquals(35, smg.currentAmmo)

        val punch = HeldWeapon(currentAmmo = -1, ammoCapacity = -1, dmg = 2.0)
        val rig3 = Rig(parry = ParryType.MEDIUM)
        rig3.player.heldItem = punch
        rig3.tap(Action.PARRY)
        assertEquals(-1, punch.currentAmmo)
    }

    @Test
    fun chargeParryStanceStopsMovementForOneSecondThenEnds() {
        val rig = Rig(parry = ParryType.CHARGE)
        rig.tap(Action.PARRY)
        assertTrue(rig.player.isChargingParry)
        assertEquals(0.0, rig.player.speed, 0.0)
        assertEquals(Rgba.CHARGE_RED, rig.player.modulate)
        assertEquals(10.0, rig.player.parryCooldownLeft, 0.05)
        rig.hold(Action.MOVE_RIGHTWARD)
        val x = rig.player.position.x
        rig.tick(20)
        assertEquals(x, rig.player.position.x, 0.01f)
        rig.seconds(0.8)
        assertFalse(rig.player.isChargingParry)
        assertEquals(500.0, rig.player.speed, 0.0)
        assertEquals(Rgba.WHITE, rig.player.modulate)
        assertEquals(100.0, rig.player.hp, 0.0)
    }

    @Test
    fun chargeParryCounterIgnoresDamageSpeedBoostParryCooldownReducedWeaponDamageTimesTen() {
        val gun = HeldWeapon(currentAmmo = 10, ammoCapacity = 15, dmg = 4.0)
        val rig = Rig(parry = ParryType.CHARGE)
        rig.player.heldItem = gun
        rig.tap(Action.PARRY)
        rig.tick(10)
        val cooldownBefore = rig.player.parryCooldownLeft
        rig.player.takeDmg(30.0)
        assertEquals(100.0, rig.player.hp, 0.0)
        assertFalse(rig.player.isChargingParry)
        assertEquals(625.0, rig.player.speed, 0.0)
        assertEquals(cooldownBefore - 4.0, rig.player.parryCooldownLeft, 0.05)
        assertEquals(40.0, gun.dmg, 0.0)
        rig.seconds(0.3)
        assertEquals(Rgba.CHARGE_RED, rig.player.modulate)
        rig.seconds(0.3)
        assertEquals(4.0, gun.dmg, 0.0)
        assertEquals(Rgba.WHITE, rig.player.modulate)
        rig.seconds(1.5)
        assertEquals(500.0, rig.player.speed, 0.0)
    }

    @Test
    fun chargeParryCounterWithoutAWeaponResetsTheTintAtOnceAndClampsTheCooldownToPointOne() {
        val rig = Rig(parry = ParryType.CHARGE)
        rig.tap(Action.PARRY)
        rig.seconds(0.1)
        rig.player.takeDmg(5.0)
        assertEquals(Rgba.WHITE, rig.player.modulate)
        assertEquals(6.0, rig.player.parryCooldownLeft, 0.2)
    }

    @Test
    fun takeDamageOrderOfChecks() {
        val rig = Rig()
        rig.player.takeDmg(10.0)
        assertEquals(90.0, rig.player.hp, 0.0)

        rig.player.canTakeDmg = false
        rig.player.takeDmg(10.0)
        assertEquals("ignored while !can_take_dmg", 90.0, rig.player.hp, 0.0)
        rig.player.takeDmg(10.0, ignoreInvincible = true)
        assertEquals(80.0, rig.player.hp, 0.0)

        rig.player.canTakeDmg = true
        rig.player.buffModifiers["invincible"] = 1.0
        rig.player.takeDmg(10.0)
        assertEquals("invincible stat wins", 80.0, rig.player.hp, 0.0)
    }

    @Test
    fun shieldAbsorbsFirstAndDamageReductionAppliesBefore() {
        val rig = Rig()
        rig.player.shield = 15.0
        rig.player.takeDmg(20.0)
        assertEquals(0.0, rig.player.shield, 0.0)
        assertEquals(95.0, rig.player.hp, 0.0)

        rig.player.shield = 50.0
        rig.player.consumableModifiers["dmg_reduction_raw"] = 30.0
        rig.player.takeDmg(40.0)
        assertEquals(40.0, rig.player.shield, 0.0)
        assertEquals(95.0, rig.player.hp, 0.0)
        rig.player.takeDmg(10.0)
        assertEquals("reduction never makes damage negative", 40.0, rig.player.shield, 0.0)
    }

    @Test
    fun invulnerabilityFlickerLastsTheRequestedTimeAndFlickersAlpha() {
        val rig = Rig()
        rig.player.takeDmg(5.0, invincibleTime = 0.1)
        assertFalse(rig.player.canTakeDmg)
        rig.player.takeDmg(5.0, invincibleTime = 0.1)
        assertEquals(95.0, rig.player.hp, 0.0)
        var minAlpha = 1.0
        var ticks = 0
        while (!rig.player.canTakeDmg && ticks < 30) {
            rig.tick()
            minAlpha = minOf(minAlpha, rig.player.modulate.a)
            ticks++
        }
        println("flicker took $ticks ticks, min alpha $minAlpha")
        assertTrue(ticks in 5..8)
        assertTrue("min alpha $minAlpha", minAlpha < 0.7)
        rig.tick(2)
        assertEquals(1.0, rig.player.modulate.a, 0.001)
        rig.player.takeDmg(5.0)
        assertEquals(90.0, rig.player.hp, 0.0)
    }

    @Test
    fun healthChangedCallbackFiresOnDamage() {
        val rig = Rig()
        var calls = 0
        rig.player.onStatsChanged = { calls++ }
        rig.player.takeDmg(1.0)
        assertEquals(1, calls)
    }

    @Test
    fun deathStopsPhysicsFadesAndShrinksOverOneSecondAndOnlyHappensOnce() {
        val rig = Rig()
        var finished = 0
        rig.player.onDeathAnimationFinished = { finished++ }
        rig.player.takeDmg(100.0)
        assertEquals(listOf(PlayerSound.DEATH), rig.sounds)
        assertFalse(rig.player.physicsEnabled)
        rig.hold(Action.MOVE_RIGHTWARD)
        val x = rig.player.position.x
        rig.tick(10)
        assertEquals(x, rig.player.position.x, 0.0f)
        assertTrue(rig.player.modulate.a < 1.0 && rig.player.spriteScale < 1.0)
        rig.player.takeDmg(10.0, ignoreInvincible = true)
        rig.seconds(1.1)
        assertEquals(0.0, rig.player.modulate.a, 0.001)
        assertEquals(0.0, rig.player.spriteScale, 0.001)
        assertEquals("B18: one death screen only", 1, finished)
        assertEquals(1, rig.sounds.count { it == PlayerSound.DEATH })
    }

    @Test
    fun temporaryBuffsAddThenRemoveAfterTheirDuration() {
        val rig = Rig()
        rig.player.addTemporaryBuff(mapOf("speed" to 1.0), 2.0)
        assertEquals(2.0, rig.player.stat("speed", 1.0), 0.0)
        rig.hold(Action.MOVE_RIGHTWARD)
        rig.tick(2)
        assertEquals(1000.0, rig.player.body.velocity.x, 0.001)
        rig.seconds(2.1)
        assertEquals(1.0, rig.player.stat("speed", 1.0), 0.0)
    }

    @Test
    fun permanentMaxHpBuffRaisesHpAndShieldGoesStraightToShield() {
        val rig = Rig()
        rig.player.addPermanentBuff(mapOf("max_hp" to 20.0, "shield" to 10.0))
        assertEquals(120.0, rig.player.hp, 0.0)
        assertEquals(120.0, rig.player.stat("max_hp", rig.player.maxHp), 0.0)
        assertEquals(10.0, rig.player.shield, 0.0)
    }

    private companion object {
        const val PlayerDefs_BASE = 500.0
    }
}
