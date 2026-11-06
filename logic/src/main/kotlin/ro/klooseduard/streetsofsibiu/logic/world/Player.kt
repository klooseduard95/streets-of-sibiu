package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.DashType
import ro.klooseduard.streetsofsibiu.logic.data.ParryType
import ro.klooseduard.streetsofsibiu.logic.data.PlayerDefs
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.physics.BodyShape
import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import ro.klooseduard.streetsofsibiu.logic.physics.KinematicBody
import ro.klooseduard.streetsofsibiu.logic.physics.StaticWorld
import ro.klooseduard.streetsofsibiu.logic.runtime.ActionInput
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime
import ro.klooseduard.streetsofsibiu.logic.runtime.SceneNode
import ro.klooseduard.streetsofsibiu.logic.runtime.TransitionType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Player(start: Vec2, private val runtime: GameRuntime, parent: SceneNode? = null) : SceneNode(parent = parent) {
    val body = KinematicBody(BodyShape(PlayerDefs.CAPSULE_RADIUS, PlayerDefs.CAPSULE_HEIGHT), start)
    val animator = FrameAnimator(PlayerDefs.WALK_ANIMATIONS, PlayerDefs.WALK_ANIMATION_STEP, PlayerDefs.WALK_ANIMATION_LENGTH)

    var selectedDash = PlayerDefs.INITIAL_DASH
    var selectedParry: ParryType? = PlayerDefs.INITIAL_PARRY
    var mousePosition: () -> Vec2 = { body.position }
    var onSound: (PlayerSound) -> Unit = {}
    var onStatsChanged: () -> Unit = {}
    var onDeathAnimationFinished: () -> Unit = {}
    var onWeaponSound: (String) -> Unit = {}
    var bullets = BulletSystem(runtime)
    val inventory = Inventory(this)
    var heldItem: PlayerItem?
        get() = inventory.items[inventory.selected]
        set(value) { inventory.items[inventory.selected] = value }

    var speed = PlayerDefs.BASE_SPEED
    var maxHp = PlayerDefs.MAX_HP
    var hp = PlayerDefs.MAX_HP
        set(value) {
            field = value
            if (value < 1) startDeath()
        }
    var shield = 0.0
    var dmgReductionRaw = 0.0

    var canTakeDmg = true
    var isSliding = false; private set
    var isChargingParry = false; private set
    var physicsEnabled = true; private set

    var modulate = Rgba.WHITE
    var spriteScale = 1.0; private set

    val weaponModifiers = mutableMapOf<String, Double>()
    val consumableModifiers = mutableMapOf<String, Double>()
    val buffModifiers = mutableMapOf<String, Double>()

    val ghosts = mutableListOf<Ghost>()

    private val ghostTimer = runtime.createNodeTimer(this, PlayerDefs.GHOST_INTERVAL).onTimeout { addGhost() }
    private val dashTimer = runtime.createNodeTimer(this, oneShot = true)
    private val slownessTimer = runtime.createNodeTimer(this, oneShot = true).onTimeout { speed = PlayerDefs.BASE_SPEED }
    private val parryTimer = runtime.createNodeTimer(this, oneShot = true)

    private var deathStarted = false

    val position: Vec2 get() = body.position
    val dashCooldownLeft: Double get() = dashTimer.timeLeft
    val parryCooldownLeft: Double get() = parryTimer.timeLeft

    fun stat(name: String, base: Double): Double =
        base + (weaponModifiers[name] ?: 0.0) + (consumableModifiers[name] ?: 0.0) + (buffModifiers[name] ?: 0.0)

    fun physicsProcess(input: ActionInput<Action>, world: StaticWorld, delta: Double) {
        if (!physicsEnabled) return
        if (!isSliding) {
            if (!handleParry(input)) return
            var dx = 0.0
            var dy = 0.0
            if (input.isPressed(Action.MOVE_FORWARD)) dy -= 1
            if (input.isPressed(Action.MOVE_BACKWARD)) dy += 1
            if (input.isPressed(Action.MOVE_LEFTWARD)) dx -= 1
            if (input.isPressed(Action.MOVE_RIGHTWARD)) dx += 1
            val len = sqrt(dx * dx + dy * dy)
            handleItem(input)
            if (len > 0) {
                dx /= len
                dy /= len
                body.velocity = DVec(dx * stat("speed", 1.0) * speed, dy * stat("speed", 1.0) * speed)
                updateAnimation(dx, dy)
            } else {
                body.velocity = DVec.ZERO
                animator.stop()
            }
            if (input.isJustPressed(Action.BLINK) && dashTimer.isStopped) {
                if (!startDash(dx, dy)) return
            }
        } else {
            body.velocity = body.velocity.moveToward(DVec.ZERO, PlayerDefs.FRICTION * delta)
            if (body.velocity.length() < PlayerDefs.SLIDE_END_SPEED) endSlide()
        }
        body.moveAndSlide(world, delta)
    }

    fun process(delta: Double) = animator.process(delta)

    private fun handleItem(input: ActionInput<Action>) {
        val item = heldItem as? HeldWeapon ?: return
        if (input.isPressed(Action.SHOOT)) item.shoot(this, runtime, runtime.random, mousePosition(), bullets)
        if (input.isJustPressed(Action.RELOAD)) item.reload(this, runtime)
    }

    fun applyWeaponModifiers(modifiers: Map<ro.klooseduard.streetsofsibiu.logic.data.Stat, Double>) {
        weaponModifiers.clear()
        modifiers.forEach { (stat, value) -> weaponModifiers[stat.godotName] = value }
    }

    fun clearWeaponModifiers() = weaponModifiers.clear()

    private fun startDash(dx: Double, dy: Double): Boolean {
        onSound(PlayerSound.DASH)
        if (dx != 0.0 || dy != 0.0 || selectedDash == DashType.LONG) {
            when (selectedDash) {
                DashType.SHORT -> {
                    teleportAlong(dx, dy)
                    dashTimer.start(PlayerDefs.dashCooldown(DashType.SHORT)!!)
                    canTakeDmg = false
                    tweenModulate(Rgba.DASH_BLUE, 0.1)
                }
                DashType.MEDIUM -> {
                    teleportAlong(dx, dy)
                    dashTimer.start(PlayerDefs.dashCooldown(DashType.MEDIUM)!!)
                    canTakeDmg = false
                    when (val item = heldItem) {
                        is HeldWeapon -> item.currentAmmo = min(item.currentAmmo + 2, item.ammoCapacity)
                        is HeldConsumable -> return false
                        null -> {}
                    }
                }
                DashType.LONG -> {
                    body.position = mousePosition()
                    body.velocity = DVec.ZERO
                    dashTimer.start(PlayerDefs.dashCooldown(DashType.LONG)!!)
                    speed = PlayerDefs.BASE_SPEED * 0.66
                    slownessTimer.start(1.0)
                }
                DashType.NONE -> {}
            }
        }
        isSliding = true
        ghostTimer.start()
        addGhost()
        return true
    }

    private fun teleportAlong(dx: Double, dy: Double) {
        val reach = PlayerDefs.BLINK_DISTANCE * 2
        body.position = Vec2(position.x + (dx * reach).toFloat(), position.y + (dy * reach).toFloat())
        body.velocity = DVec(dx * PlayerDefs.SLIDE_MOMENTUM, dy * PlayerDefs.SLIDE_MOMENTUM)
    }

    private fun endSlide() {
        isSliding = false
        ghostTimer.stop()
        if (selectedDash == DashType.SHORT || selectedDash == DashType.MEDIUM) {
            canTakeDmg = true
            if (selectedDash == DashType.SHORT) {
                tweenModulate(Rgba.WHITE, 0.1).onFinished { canTakeDmg = true }
            }
        }
    }

    private fun addGhost() {
        val ghost = Ghost(position, animator.frame, spriteScale)
        runtime.callDeferred {
            ghosts += ghost
            ghost.start(runtime) { ghosts -= it }
        }
    }

    private fun handleParry(input: ActionInput<Action>): Boolean {
        if (input.isJustPressed(Action.PARRY) && parryTimer.isStopped) {
            when (selectedParry) {
                ParryType.NORMAL -> normalParry()
                ParryType.MEDIUM -> return mediumParry()
                ParryType.CHARGE -> chargeParry()
                null -> {}
            }
        }
        return true
    }

    private fun normalParry() {
        onSound(PlayerSound.PARRY)
        canTakeDmg = false
        parryTimer.start(PlayerDefs.parryCooldown(ParryType.NORMAL))
        flash(Rgba.PARRY_GREEN)
        runtime.createTimer(0.2, processAlways = false).onTimeout { canTakeDmg = true }
    }

    private fun mediumParry(): Boolean {
        onSound(PlayerSound.PARRY)
        canTakeDmg = false
        parryTimer.start(PlayerDefs.parryCooldown(ParryType.MEDIUM))
        when (val item = heldItem) {
            is HeldWeapon -> if (item.isSniper) item.currentAmmo = item.ammoCapacity else if (item.ammoCapacity > 0) item.currentAmmo += 10
            is HeldConsumable -> return false
            null -> {}
        }
        flash(Rgba.PARRY_YELLOW)
        runtime.createTimer(0.2, processAlways = false).onTimeout { canTakeDmg = true }
        return true
    }

    private fun chargeParry() {
        onSound(PlayerSound.PARRY)
        isChargingParry = true
        speed = 0.0
        parryTimer.start(PlayerDefs.parryCooldown(ParryType.CHARGE))
        modulate = Rgba.CHARGE_RED
        runtime.createTimer(1.0, processAlways = false).onTimeout { if (isChargingParry) endChargeParry() }
    }

    private fun endChargeParry() {
        isChargingParry = false
        speed = PlayerDefs.BASE_SPEED
        modulate = Rgba.WHITE
    }

    private fun flash(color: Rgba) {
        val tween = runtime.createTween(this)
        tween.tweenProperty({ modulate }, { modulate = it }, color, 0.1, Rgba::lerp)
        tween.tweenProperty({ modulate }, { modulate = it }, Rgba.WHITE, 0.1, Rgba::lerp)
    }

    private fun tweenModulate(target: Rgba, duration: Double) =
        runtime.createTween(this).also { tw -> tw.tweenProperty({ modulate }, { modulate = it }, target, duration, Rgba::lerp) }

    fun takeDmg(damage: Double, invincibleTime: Double = 0.0, ignoreInvincible: Boolean = false) {
        if (stat("invincible", 0.0) > 0.0) return
        if (isChargingParry) {
            counterWithChargeParry()
            return
        }
        if (!canTakeDmg && !ignoreInvincible) return
        if (invincibleTime > 0.0) {
            canTakeDmg = false
            val quarter = invincibleTime / 4.0
            val tween = runtime.createTween(this).setTrans(TransitionType.SINE)
            for (alpha in listOf(0.5, 1.0, 0.5, 1.0)) {
                tween.tweenProperty({ modulate.a }, { modulate = modulate.withAlpha(it) }, alpha, quarter)
            }
            tween.onFinished { canTakeDmg = true }
        }
        var finalDamage = max(damage - stat("dmg_reduction_raw", dmgReductionRaw), 0.0)
        if (shield > 0) {
            if (shield >= finalDamage) {
                shield -= finalDamage
                finalDamage = 0.0
            } else {
                finalDamage -= shield
                shield = 0.0
            }
        }
        hp -= finalDamage
        onStatsChanged()
    }

    private fun counterWithChargeParry() {
        isChargingParry = false
        speed = PlayerDefs.BASE_SPEED * 1.25
        runtime.createTimer(2.0, processAlways = false).onTimeout { speed = PlayerDefs.BASE_SPEED }
        parryTimer.start(max(0.1, parryTimer.timeLeft - 4.0))
        val item = heldItem
        if (item is HeldWeapon) {
            val original = item.dmg
            item.dmg = original * 10
            runtime.createTimer(0.5, processAlways = false).onTimeout {
                item.dmg = original
                modulate = Rgba.WHITE
            }
        } else {
            modulate = Rgba.WHITE
        }
    }

    private fun startDeath() {
        if (deathStarted) return
        deathStarted = true
        onSound(PlayerSound.DEATH)
        physicsEnabled = false
        val tween = runtime.createTween(this).setTrans(TransitionType.SINE)
        tween.tweenProperty({ modulate.a }, { modulate = modulate.withAlpha(it) }, 0.0, 1.0)
        tween.parallel().tweenProperty({ spriteScale }, { spriteScale = it }, 0.0, 1.0)
        tween.onFinished { onDeathAnimationFinished() }
    }

    fun addTemporaryBuff(modifiers: Map<String, Double>, duration: Double) {
        modifiers.forEach { (stat, value) -> consumableModifiers[stat] = (consumableModifiers[stat] ?: 0.0) + value }
        onStatsChanged()
        runtime.createTimer(duration, processAlways = false).onTimeout {
            if (isFreed) return@onTimeout
            modifiers.forEach { (stat, value) -> consumableModifiers[stat]?.let { consumableModifiers[stat] = it - value } }
            onStatsChanged()
        }
    }

    fun addPermanentBuff(modifiers: Map<String, Double>) {
        for ((stat, value) in modifiers) {
            if (stat == "shield") {
                shield += value
                continue
            }
            buffModifiers[stat] = (buffModifiers[stat] ?: 0.0) + value
            if (stat == "max_hp") hp += value
        }
        onStatsChanged()
    }

    private fun updateAnimation(dx: Double, dy: Double) {
        if (abs(dx) > abs(dy)) {
            animator.play(if (dx > 0) "walk_right" else "walk_left")
        } else {
            animator.play(if (dy > 0) "walk_down" else "walk_up")
        }
    }
}
