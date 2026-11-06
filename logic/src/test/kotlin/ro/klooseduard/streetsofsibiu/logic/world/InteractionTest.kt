package ro.klooseduard.streetsofsibiu.logic.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.Consumables
import ro.klooseduard.streetsofsibiu.logic.data.ItemDef
import ro.klooseduard.streetsofsibiu.logic.data.Weapons
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.runtime.FrameHooks
import ro.klooseduard.streetsofsibiu.logic.runtime.GameLoop
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRandom
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime

class InteractionTest {
    private class Rig {
        val runtime = GameRuntime(GameRandom(1))
        val input = ActionInput<Action>(runtime)
        val player = Player(Vec2(500f, 550f), runtime)
        val sounds = mutableListOf<PlayerSound>()
        private val loop = GameLoop(runtime, object : FrameHooks {
            override fun flushPhysicsQueries() = player.interaction.flushPhysicsQueries()
            override fun physicsProcess(delta: Double) = player.physicsProcess(input, ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld(emptyList()), delta)
            override fun process(delta: Double) {
                player.process(delta)
                player.interaction.process()
            }
        })

        init {
            player.onSound = { sounds += it }
        }

        fun tick(n: Int = 1) = repeat(n) { loop.advance(1.0 / 60) }
        fun seconds(s: Double) = tick(Math.round(s * 60).toInt())
        fun ground(def: ItemDef, dx: Float = 0f, dy: Float = 0f) = player.interaction.spawn(GameItem(Vec2(500f + dx, 550f + dy), def))
        fun pressInteract() = player.interaction.interactPressed()
        fun useItem() {
            input.press(Action.SHOOT)
            tick()
            input.release(Action.SHOOT)
        }
    }

    @Test
    fun labelAppearsWithinRangeAndShowsPickUpName() {
        val rig = Rig()
        rig.ground(Weapons.KNIFE, dx = 139f)
        rig.tick(2)
        assertTrue(rig.player.interaction.labelVisible)
        assertEquals("Pick up Knife", rig.player.interaction.labelText)
    }

    @Test
    fun noLabelWhenOutOfRange() {
        val rig = Rig()
        rig.ground(Weapons.KNIFE, dx = 141f)
        rig.tick(3)
        assertFalse(rig.player.interaction.labelVisible)
        rig.pressInteract()
        assertTrue("nothing picked up", rig.player.inventory.items[2] is HeldWeapon && (rig.player.inventory.items[2] as HeldWeapon).def == Weapons.PUNCH)
    }

    @Test
    fun interactPutsTheItemInTheFirstFreeMatchingSlotAndRemovesTheGroundItem() {
        val rig = Rig()
        val item = rig.ground(Weapons.SMG)
        rig.tick(2)
        rig.pressInteract()
        assertTrue(rig.player.inventory.items[0] is HeldWeapon)
        assertTrue(item.isFreed)
        assertFalse(item.interactable.isInteractable)
        assertTrue(rig.player.interaction.items.isEmpty())
        rig.tick(2)
        assertFalse(rig.player.interaction.labelVisible)
    }

    @Test
    fun fullSlotsSwapWithTheSelectedSlotAndTheOldItemStaysOnTheGround() {
        val rig = Rig()
        rig.player.inventory.addItem(Weapons.MEDIUM_RIFLE)
        rig.player.inventory.addItem(Weapons.SMG)
        val ground = rig.ground(Weapons.NEGEV)
        rig.tick(2)
        rig.pressInteract()
        assertEquals("primary_alternative", (rig.player.inventory.items[0] as HeldWeapon).def!!.id)
        assertEquals("primary_short", (rig.player.inventory.items[1] as HeldWeapon).def!!.id)
        assertFalse(ground.isFreed)
        assertEquals(Weapons.MEDIUM_RIFLE, ground.def)
        assertEquals("Switch with Medium Rifle", ground.interactable.interactName)
        rig.tick(2)
        assertEquals("Switch with Medium Rifle", rig.player.interaction.labelText)
        rig.pressInteract()
        assertEquals("primary_medium", (rig.player.inventory.items[0] as HeldWeapon).def!!.id)
        assertEquals(Weapons.NEGEV, ground.def)
    }

    @Test
    fun swapDoesNothingWhenTheSelectedSlotIsOfAnotherType() {
        val rig = Rig()
        rig.player.inventory.addItem(Weapons.MEDIUM_RIFLE)
        rig.player.inventory.addItem(Weapons.SMG)
        rig.player.inventory.selected = 3
        val ground = rig.ground(Weapons.NEGEV)
        rig.tick(2)
        rig.pressInteract()
        assertFalse(ground.isFreed)
        assertEquals(Weapons.NEGEV, ground.def)
        assertEquals("primary_medium", (rig.player.inventory.items[0] as HeldWeapon).def!!.id)
    }

    @Test
    fun theNearestItemIsOfferedFirst() {
        val rig = Rig()
        rig.ground(Weapons.NEGEV, dx = 100f)
        rig.ground(Weapons.SMG, dx = 30f)
        rig.tick(2)
        assertEquals("Pick up SMG", rig.player.interaction.labelText)
        rig.pressInteract()
        assertEquals(1, rig.player.interaction.items.size)
        assertEquals(Weapons.NEGEV, rig.player.interaction.items[0].def)
    }

    @Test
    fun aNonInteractableFirstItemLeavesTheLabelAsItWas() {
        val rig = Rig()
        val a = rig.ground(Weapons.KNIFE, dx = 10f)
        rig.ground(Weapons.BAT, dx = 100f)
        rig.tick(2)
        assertEquals("Pick up Knife", rig.player.interaction.labelText)
        a.interactable.isInteractable = false
        rig.tick(2)
        assertEquals("Pick up Knife", rig.player.interaction.labelText)
        assertTrue(rig.player.interaction.labelVisible)
    }

    @Test
    fun walkingAwayLeavesTheRangeAndHidesTheLabel() {
        val rig = Rig()
        rig.ground(Weapons.KNIFE)
        rig.tick(2)
        assertTrue(rig.player.interaction.labelVisible)
        rig.player.body.position = Vec2(900f, 550f)
        rig.tick(2)
        assertFalse(rig.player.interaction.labelVisible)
    }

    @Test
    fun healthPotionOnlyWorksBelowFullHealthAddsFortyAndIsConsumed() {
        val rig = Rig()
        rig.player.inventory.addItem(Consumables.HEALTH)
        rig.player.inventory.selected = 3
        rig.useItem()
        assertTrue("full HP: not used", rig.player.inventory.items[3] is HeldConsumable)
        assertTrue(rig.sounds.isEmpty())
        rig.player.hp = 50.0
        rig.useItem()
        assertEquals(90.0, rig.player.hp, 0.0)
        assertNull(rig.player.inventory.items[3])
        assertEquals(listOf(PlayerSound.POTION), rig.sounds)
        rig.player.inventory.addItem(Consumables.HEALTH)
        rig.player.hp = 80.0
        rig.useItem()
        assertEquals("capped at max HP", 100.0, rig.player.hp, 0.0)
    }

    @Test
    fun consumableIsUsedOnThePressNotWhileHolding() {
        val rig = Rig()
        rig.player.inventory.addItem(Consumables.SHIELD)
        rig.player.inventory.addItem(Consumables.SHIELD)
        rig.player.inventory.selected = 3
        rig.input.press(Action.SHOOT)
        rig.seconds(0.5)
        assertEquals(150.0, rig.player.shield, 0.0)
        assertTrue(rig.player.inventory.items[4] is HeldConsumable)
    }

    @Test
    fun shieldPotionAddsShieldSlowsAndEndsAfterTenSeconds() {
        val rig = Rig()
        rig.player.inventory.addItem(Consumables.SHIELD)
        rig.player.inventory.selected = 3
        rig.useItem()
        assertEquals(150.0, rig.player.shield, 0.0)
        assertEquals(0.67, rig.player.stat("speed", 1.0), 1e-9)
        rig.seconds(9.8)
        assertEquals(0.67, rig.player.stat("speed", 1.0), 1e-9)
        rig.seconds(0.4)
        assertEquals(1.0, rig.player.stat("speed", 1.0), 1e-9)
    }

    @Test
    fun speedRageArmorAndGhostBuffsLastTheirDuration() {
        val cases = listOf(
            Triple(Consumables.SPEED, "speed" to 1.0, 20.0),
            Triple(Consumables.RAGE, "damage_bonus" to 100.0, 10.0),
            Triple(Consumables.ARMOR, "dmg_reduction_raw" to 30.0, 10.0),
            Triple(Consumables.GHOST, "invincible" to 1.0, 5.0),
        )
        for ((def, mod, duration) in cases) {
            val rig = Rig()
            rig.player.inventory.addItem(def)
            rig.player.inventory.selected = 3
            rig.useItem()
            val base = if (mod.first == "speed") 1.0 else 0.0
            assertEquals(def.name, base + mod.second, rig.player.stat(mod.first, base), 1e-9)
            rig.seconds(duration + 0.2)
            assertEquals(def.name + " ended", base, rig.player.stat(mod.first, base), 1e-9)
        }
    }

    @Test
    fun ghostPotionMakesThePlayerInvincible() {
        val rig = Rig()
        rig.player.inventory.addItem(Consumables.GHOST)
        rig.player.inventory.selected = 3
        rig.useItem()
        rig.player.takeDmg(30.0)
        assertEquals(100.0, rig.player.hp, 0.0)
        rig.seconds(5.2)
        rig.player.takeDmg(30.0)
        assertEquals(70.0, rig.player.hp, 0.0)
    }

    @Test
    fun adrenalineGivesPlusFiftyThenMinusFiftyAfterwards() {
        val rig = Rig()
        rig.player.inventory.addItem(Consumables.ADRENALINE)
        rig.player.inventory.selected = 3
        rig.useItem()
        assertEquals(50.0, rig.player.stat("damage_bonus", 0.0), 1e-9)
        rig.seconds(20.3)
        assertEquals(-50.0, rig.player.stat("damage_bonus", 0.0), 1e-9)
        rig.seconds(10.0)
        assertEquals(0.0, rig.player.stat("damage_bonus", 0.0), 1e-9)
    }

    @Test
    fun stunGrenadeCostsTwentyHpAndSlowsEnemiesForSixSeconds() {
        val rig = Rig()
        rig.player.inventory.addItem(Consumables.STUN_GRENADE)
        rig.player.inventory.selected = 3
        rig.useItem()
        assertEquals(80.0, rig.player.hp, 0.0)
        assertEquals(0.0, rig.player.slownEnemy, 1e-9)
        rig.seconds(6.2)
        assertEquals(1.0, rig.player.slownEnemy, 1e-9)
    }
}
