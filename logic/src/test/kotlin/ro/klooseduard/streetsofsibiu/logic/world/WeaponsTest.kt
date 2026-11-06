package ro.klooseduard.streetsofsibiu.logic.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.Consumables
import ro.klooseduard.streetsofsibiu.logic.data.Stat
import ro.klooseduard.streetsofsibiu.logic.data.WeaponDef
import ro.klooseduard.streetsofsibiu.logic.data.Weapons
import ro.klooseduard.streetsofsibiu.logic.math.Rect
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.runtime.FrameHooks
import ro.klooseduard.streetsofsibiu.logic.runtime.GameLoop
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRandom
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime

class WeaponsTest {
    private class Dummy(override val kind: TargetKind, val x: Double, val y: Double, val r: Double) : BulletTarget {
        var damage = 0.0
        var hits = 0
        override fun overlapsCapsule(ax: Double, ay: Double, bx: Double, by: Double, radius: Double): Boolean {
            val steps = 8
            return (0..steps).any { i ->
                val t = i.toDouble() / steps
                val px = ax + (bx - ax) * t
                val py = ay + (by - ay) * t
                Math.hypot(px - x, py - y) <= radius + r
            }
        }
        override fun takeDamage(amount: Double) {
            damage += amount
            hits++
        }
    }

    private class Rig(seed: Long = 1) {
        val runtime = GameRuntime(GameRandom(seed))
        var clock = 1000L
        val input = ActionInput<Action>(runtime)
        val player = Player(Vec2(500f, 550f), runtime)
        val sounds = mutableListOf<String>()
        val dummies = mutableListOf<Dummy>()
        private val loop = GameLoop(runtime, object : FrameHooks {
            override fun flushPhysicsQueries() = player.bullets.flushPhysicsQueries()
            override fun physicsProcess(delta: Double) {
                player.physicsProcess(input, player.bullets.world, delta)
                player.bullets.physicsProcess(delta)
            }
            override fun process(delta: Double) = player.process(delta)
        })

        init {
            runtime.clockMs = { clock }
            player.onWeaponSound = { sounds += it }
            player.bullets.targets = { dummies }
            player.mousePosition = { Vec2(1500f, 550f) }
        }

        fun give(def: WeaponDef): HeldWeapon {
            player.inventory.items[player.inventory.selected] = HeldWeapon.of(def)
            return player.heldItem as HeldWeapon
        }

        fun tick(n: Int = 1) = repeat(n) {
            loop.advance(1.0 / 60)
            clock += 1000 / 60
        }
        fun seconds(s: Double) = tick(Math.round(s * 60).toInt())
        fun shootOnce(): Int {
            input.press(Action.SHOOT)
            tick()
            input.release(Action.SHOOT)
            return player.bullets.bullets.size
        }
    }

    @Test
    fun startingInventoryHasPunchInSlotThreeAndSlotOneSelected() {
        val rig = Rig()
        assertEquals(0, rig.player.inventory.selected)
        assertNull(rig.player.inventory.items[0])
        assertEquals("secondary_punch", (rig.player.inventory.items[2] as HeldWeapon).def!!.id)
    }

    @Test
    fun shotSpawnsABulletAtThePlayerAimedAtTheMouseAndPlaysTheWeaponSound() {
        val rig = Rig()
        rig.give(Weapons.MEDIUM_RIFLE)
        rig.input.press(Action.SHOOT)
        rig.tick(2)
        val b = rig.player.bullets.bullets.single()
        assertEquals(1.0, b.dirX, 1e-9)
        assertEquals(0.0, b.dirY, 1e-9)
        assertEquals(1200.0, b.speed, 0.0)
        assertEquals(listOf("Music/Gunshot.mp3"), rig.sounds)
        assertEquals(14, (rig.player.heldItem as HeldWeapon).currentAmmo)
    }

    @Test
    fun fireRateGivesOneShotPerCooldownWhileHeld() {
        val rig = Rig()
        rig.give(Weapons.MEDIUM_RIFLE)
        rig.input.press(Action.SHOOT)
        rig.seconds(1.0)
        val shots = rig.sounds.size
        println("rifle shots in 1 s: $shots")
        assertTrue(shots in 3..4)
    }

    @Test
    fun damageFormulaUsesDamageBonusRampAndDoublesOnCrit() {
        val rig = Rig()
        rig.give(Weapons.MEDIUM_RIFLE)
        rig.player.consumableModifiers["damage_bonus"] = 100.0
        rig.shootOnce()
        assertEquals(8.0, rig.player.bullets.bullets.single().dmg, 1e-9)

        val crit = Rig()
        crit.give(Weapons.MEDIUM_RIFLE)
        crit.player.buffModifiers["crit_chance"] = 100.0
        crit.shootOnce()
        assertEquals(8.0, crit.player.bullets.bullets.single().dmg, 1e-9)

        val none = Rig()
        none.give(Weapons.MEDIUM_RIFLE)
        none.shootOnce()
        assertEquals(4.0, none.player.bullets.bullets.single().dmg, 1e-9)
    }

    @Test
    fun critRateMatchesTheTotalChance() {
        val rig = Rig(seed = 42)
        val gun = rig.give(Weapons.SNIPER)
        rig.player.applyWeaponModifiers(Weapons.SNIPER.modifiers)
        var crits = 0
        val n = 4000
        repeat(n) {
            rig.player.bullets.clear()
            gun.currentAmmo = 1
            rig.shootOnce()
            rig.player.bullets.bullets.firstOrNull()?.let { if (it.dmg > 30) crits++ }
            rig.seconds(0.15)
            rig.player.bullets.clear()
        }
        val rate = crits / n.toDouble()
        println("sniper crit rate %.3f".format(rate))
        assertEquals(0.20, rate, 0.03)
    }

    @Test
    fun magazineEmptyPressStartsAReloadThatRefillsAfterTheReloadTime() {
        val rig = Rig()
        val gun = rig.give(Weapons.MEDIUM_RIFLE)
        gun.currentAmmo = 0
        rig.input.press(Action.SHOOT)
        rig.tick(2)
        rig.input.release(Action.SHOOT)
        assertTrue(gun.isReloading)
        assertEquals(0, rig.sounds.size)
        rig.seconds(0.6)
        assertTrue(gun.isReloading)
        rig.seconds(0.25)
        assertFalse(gun.isReloading)
        assertEquals(15, gun.currentAmmo)
    }

    @Test
    fun reloadKeyOnlyWorksBelowCapacityAndCapacityAndReloadUpgradesApplyToEveryWeapon() {
        val rig = Rig()
        val gun = rig.give(Weapons.SMG)
        rig.input.press(Action.RELOAD)
        rig.tick(2)
        rig.input.release(Action.RELOAD)
        assertFalse("full magazine: no reload", gun.isReloading)

        rig.player.buffModifiers["bonus_weapon_capacity"] = 5.0
        assertEquals(35, gun.totalCapacity(rig.player))
        rig.input.press(Action.RELOAD)
        rig.tick(2)
        rig.input.release(Action.RELOAD)
        assertTrue(gun.isReloading)
        rig.seconds(1.6)
        assertEquals(35, gun.currentAmmo)

        val rig2 = Rig()
        val rifle = rig2.give(Weapons.MEDIUM_RIFLE)
        rig2.player.buffModifiers["sniper_cooldown_reduction"] = 0.5
        rifle.currentAmmo = 0
        rig2.input.press(Action.SHOOT)
        rig2.tick(2)
        rig2.input.release(Action.SHOOT)
        rig2.seconds(0.35)
        assertEquals(15, rifle.currentAmmo)
    }

    @Test
    fun sniperEmptiesTheMagazineAfterOneShot() {
        val rig = Rig()
        val sniper = rig.give(Weapons.SNIPER)
        rig.shootOnce()
        assertEquals(0, sniper.currentAmmo)
        rig.seconds(0.3)
        rig.input.press(Action.SHOOT)
        rig.tick(2)
        assertTrue(sniper.isReloading)
    }

    @Test
    fun meleeWeaponsHaveInfiniteAmmoAndShowTheirIconOnTheBullet() {
        val rig = Rig()
        rig.give(Weapons.KNIFE)
        rig.shootOnce()
        val b = rig.player.bullets.bullets.single()
        assertTrue(b.isMelee)
        assertEquals("item/item_assets/item icons/knife.png", b.weaponIcon)
        assertEquals(-1, (rig.player.heldItem as HeldWeapon).currentAmmo)
    }

    @Test
    fun chainsawRampGainsPerEntryDecaysWithTimeAndAddsToDamage() {
        val rig = Rig()
        rig.give(Weapons.CHAINSAW)
        val enemy = Dummy(TargetKind.ENEMY_HITBOX, 560.0, 550.0, 30.0)
        val enemyBody = Dummy(TargetKind.ENEMY_BODY, 560.0, 550.0, 30.0)
        rig.dummies += listOf(enemy, enemyBody)
        rig.shootOnce()
        assertEquals(1.0, rig.player.bullets.bullets.single().dmg, 1e-9)
        rig.tick(4)
        val gun = rig.player.heldItem as HeldWeapon
        assertEquals("double entry: hitbox and body each add 0.4", 0.8, gun.rampStack, 1e-9)
        assertEquals(1.0, enemy.damage, 1e-9)
        rig.dummies.clear()
        rig.seconds(0.2)
        rig.shootOnce()
        val second = rig.player.bullets.bullets.last()
        assertEquals(1.0 + 0.8 - 0.28 * 2.0, second.dmg, 0.08)
    }

    @Test
    fun rangedBulletDespawnsAtMaxRangeAndMeleeToo() {
        val rig = Rig()
        rig.give(Weapons.MEDIUM_RIFLE)
        rig.shootOnce()
        rig.seconds(0.6)
        assertTrue(rig.player.bullets.bullets.isEmpty())
        val knife = Rig()
        knife.give(Weapons.KNIFE)
        knife.shootOnce()
        knife.seconds(0.3)
        assertTrue(knife.player.bullets.bullets.isEmpty())
    }

    @Test
    fun rangedBulletDamagesTheHitboxOnceAndIsFreedMeleeKeepsGoing() {
        val rig = Rig()
        rig.give(Weapons.MEDIUM_RIFLE)
        val hitbox = Dummy(TargetKind.ENEMY_HITBOX, 800.0, 550.0, 20.0)
        rig.dummies += hitbox
        rig.shootOnce()
        rig.seconds(0.5)
        assertEquals(1, hitbox.hits)
        assertEquals(4.0, hitbox.damage, 1e-9)
        assertTrue(rig.player.bullets.bullets.isEmpty())

        val bat = Rig()
        bat.give(Weapons.BAT)
        val h2 = Dummy(TargetKind.ENEMY_HITBOX, 600.0, 550.0, 20.0)
        bat.dummies += h2
        bat.shootOnce()
        bat.seconds(0.1)
        assertEquals(1, h2.hits)
        assertFalse(bat.player.bullets.bullets.isEmpty() && bat.player.bullets.bullets.size > 1)
    }

    @Test
    fun enemyBodyBlocksRangedBulletsButNotMeleeAndWallsStopBoth() {
        val rig = Rig()
        rig.give(Weapons.MEDIUM_RIFLE)
        val body = Dummy(TargetKind.ENEMY_BODY, 700.0, 550.0, 25.0)
        val hitbox = Dummy(TargetKind.ENEMY_HITBOX, 900.0, 550.0, 20.0)
        rig.dummies += listOf(body, hitbox)
        rig.shootOnce()
        rig.seconds(0.4)
        assertEquals(0, body.hits)
        assertEquals(0, hitbox.hits)
        assertTrue(rig.player.bullets.bullets.isEmpty())

        val wall = Rig()
        wall.give(Weapons.SNIPER)
        wall.player.bullets.world = StaticWorld(listOf(Rect(800f, 400f, 100f, 300f)))
        val behind = Dummy(TargetKind.ENEMY_HITBOX, 1100.0, 550.0, 20.0)
        wall.dummies += behind
        wall.shootOnce()
        wall.seconds(0.6)
        assertEquals(0, behind.hits)
        assertTrue(wall.player.bullets.bullets.isEmpty())

        val bat = Rig()
        bat.give(Weapons.BAT)
        bat.player.bullets.world = StaticWorld(listOf(Rect(600f, 400f, 50f, 300f)))
        bat.shootOnce()
        bat.seconds(0.2)
        assertTrue(bat.player.bullets.bullets.isEmpty())
    }

    @Test
    fun weaponsEquipAndUnequipTheirModifiersWhenTheSlotChanges() {
        val rig = Rig()
        rig.player.inventory.addItem(Weapons.SMG)
        assertEquals(1, rig.player.inventory.items.count { it is HeldWeapon && it.def?.id == "primary_short" })
        assertEquals("slot 1 is selected: equipped at once", 1.1, rig.player.stat("speed", 1.0), 1e-9)
        rig.player.inventory.selected = 2
        assertEquals(1.0, rig.player.stat("speed", 1.0), 1e-9)
        rig.player.inventory.selected = 0
        assertEquals(1.1, rig.player.stat("speed", 1.0), 1e-9)
    }

    @Test
    fun addItemFillsTheFirstEmptyMatchingSlotWithAFreshCopy() {
        val rig = Rig()
        val inv = rig.player.inventory
        assertTrue(inv.addItem(Weapons.MEDIUM_RIFLE))
        assertTrue(inv.addItem(Weapons.SMG))
        assertFalse("both primary slots full", inv.addItem(Weapons.NEGEV))
        assertTrue(inv.items[0] is HeldWeapon && inv.items[1] is HeldWeapon)
        assertFalse("secondary slot already holds the punch", inv.addItem(Weapons.KNIFE))
        assertTrue(inv.addItem(Consumables.HEALTH))
        assertTrue(inv.addItem(Consumables.SHIELD))
        assertFalse(inv.addItem(Consumables.SPEED))
        val a = (inv.items[0] as HeldWeapon)
        a.currentAmmo = 3
        val inv2 = Rig().player.inventory
        inv2.addItem(Weapons.MEDIUM_RIFLE)
        assertEquals(15, (inv2.items[0] as HeldWeapon).currentAmmo)
    }

    @Test
    fun swapOnlyWorksWhenTheSelectedSlotMatchesTheItemTypeAndGivesBackTheOldItem() {
        val rig = Rig()
        val inv = rig.player.inventory
        inv.addItem(Weapons.MEDIUM_RIFLE)
        inv.addItem(Weapons.SMG)
        assertNull("selected slot 0 is primary, knife is secondary", inv.swapItem(Weapons.KNIFE))
        val old = inv.swapItem(Weapons.NEGEV)
        assertEquals("primary_medium", (old as HeldWeapon).def!!.id)
        assertEquals("primary_alternative", (inv.items[0] as HeldWeapon).def!!.id)
        assertNotNull(inv.items[0])
        inv.selected = 2
        val punch = inv.swapItem(Weapons.BAT)
        assertEquals("secondary_punch", (punch as HeldWeapon).def!!.id)
        assertEquals("fresh instance, B11", 8.0, (inv.items[2] as HeldWeapon).dmg, 0.0)
    }
}
