package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.AttackParams
import ro.klooseduard.streetsofsibiu.logic.data.EnemyAttacks
import ro.klooseduard.streetsofsibiu.logic.data.EnemyStats
import ro.klooseduard.streetsofsibiu.logic.data.EnemyType
import ro.klooseduard.streetsofsibiu.logic.data.EnemyTypes
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.nav.NavAgent
import ro.klooseduard.streetsofsibiu.logic.physics.BodyShape
import ro.klooseduard.streetsofsibiu.logic.physics.DVec
import ro.klooseduard.streetsofsibiu.logic.physics.DynamicCollider
import ro.klooseduard.streetsofsibiu.logic.physics.KinematicBody
import ro.klooseduard.streetsofsibiu.logic.physics.MotionMode
import ro.klooseduard.streetsofsibiu.logic.runtime.SceneNode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Enemy(
    val type: EnemyType,
    private val system: EnemySystem,
    base: EnemyStats,
    level: Int,
    spawnPos: Vec2?,
) : SceneNode(parent = system.root) {
    var hp = base.hp; private set
    var maxHp = base.hp; private set
    var dmg = base.dmg; private set
    var walkSpeed = base.walkSpeed; private set
    var dmgMult = base.dmgMult
    val reach = base.reach
    val hitCooldown = base.hitCooldown
    val hitWindup = base.hitWindup
    var sizeModifier = base.sizeModifier; private set
    var visionRange = base.visionRange; private set

    val body: KinematicBody
    var isDead = false; private set
    var alpha = 1.0; private set
    var healthBarAlpha = 0.0; private set
    var healthBarValue = 0.0; private set
    var frame = 0; private set
    var agro = 0; private set
    var isIdle = false; private set
    var hitRangeDisabled = false; private set
    private var hitRangeInside = false

    private var row: Int? = null
    private var col: Int? = null
    private var currentTimeToFrame = 0

    private val agent: NavAgent
    val collider: DynamicCollider

    val position: Vec2 get() = body.position

    init {
        val random = system.runtime.random
        var start = Vec2(0f, 0f)
        val offsetRadius = random.randiRange(1, 5)
        val offsetAngle = Math.toRadians(random.randiRange(1, 360).toDouble())
        start = Vec2((offsetRadius * cos(offsetAngle)).toFloat(), (offsetRadius * sin(offsetAngle)).toFloat())
        if (spawnPos != null) start = spawnPos
        system.enemyCount++
        applyLevelScaling(level)
        rollForSize()
        maxHp = hp
        healthBarValue = hp
        visionRange *= 10
        frame = when (type) {
            EnemyType.THIEF, EnemyType.CRIMINAL -> 5
            else -> 0
        }
        body = KinematicBody(BodyShape(10.0 * sizeModifier, 20.0 * sizeModifier), start, MotionMode.FLOATING).also {
            it.wallMinSlideAngle = 0.0
            it.floorBlockOnWall = false
            it.owner = this
        }
        collider = DynamicCollider(this, { body.position }, 10.0 * sizeModifier)
        agent = NavAgent(system.query, { body.position })
        system.runtime.createNodeTimer(this, 1.0, oneShot = false, autostart = true).onTimeout { agroCheck() }
    }

    private fun applyLevelScaling(level: Int) {
        val n = level - 1
        when (type) {
            EnemyType.RAT -> { hp += 3 * n; dmg += 1 * n }
            EnemyType.PIGEON -> { hp += 2 * n; dmg += 1 * n }
            EnemyType.THIEF -> { hp += 3 * n; dmg += 1.5 * n }
            EnemyType.CRIMINAL -> { hp += 3 * n; dmg += 1.5 * n }
            else -> {}
        }
    }

    private fun rollForSize() {
        val random = system.runtime.random
        if (sizeModifier == 1.0) {
            val rng = random.randiRange(1, 100)
            if (rng <= 10) {
                return
            } else if (rng <= 85) {
                sizeModifier = 1.25
                hp += 20.0 / 100 * hp
                dmg += 10.0 / 100 * dmg
            } else {
                sizeModifier = 1.5
                hp += 40.0 / 100 * hp
                dmg += 20.0 / 100 * dmg
            }
        }
        if (sizeModifier == 1.25) {
            val rng = random.randiRange(1, 100)
            if (rng <= 85) {
                return
            } else {
                sizeModifier = 1.5
                hp += 30.0 / 100 * hp
                dmg += 15.0 / 100 * dmg
            }
        }
    }

    fun physicsProcess(delta: Double) {
        if (isFreed) return
        animate()
        currentTimeToFrame += 1
        if (!isIdle) pathFind(delta)
    }

    private fun pathFind(delta: Double) {
        val next = agent.nextPathPosition()
        val dx = (next.x - body.position.x).toDouble()
        val dy = (next.y - body.position.y).toDouble()
        val len = sqrt(dx * dx + dy * dy)
        val speed = walkSpeed * system.player.slownEnemy
        body.velocity = if (len > 0) DVec(dx / len * speed, dy / len * speed) else DVec.ZERO
        body.moveAndSlide(system.world, delta)
    }

    private fun animate() {
        val v = body.velocity
        val moving = v.x != 0.0 || v.y != 0.0
        when (type) {
            EnemyType.RAT -> if (moving && currentTimeToFrame > TIME_TO_FRAME) {
                col = if (v.x > 0) 1 else 0
                row = if (row == 1) 0 else 1
                frame = row!! * 2 + col!!
                currentTimeToFrame = 0
            }
            EnemyType.PIGEON -> if (currentTimeToFrame > TIME_TO_FRAME) {
                col = if (v.x > 0) 1 else 0
                row = if (row == 1) 0 else 1
                frame = row!! * 2 + col!!
                currentTimeToFrame = 0
            }
            EnemyType.THIEF, EnemyType.CRIMINAL -> if (moving && currentTimeToFrame > TIME_TO_FRAME) {
                row = if (v.x >= 0) {
                    if (v.y >= 0) { if (v.x > v.y) 3 else 1 } else { if (v.x > abs(v.y)) 3 else 0 }
                } else {
                    if (v.y >= 0) { if (abs(v.x) > v.y) 2 else 1 } else { if (abs(v.x) > abs(v.y)) 2 else 0 }
                }
                col = if (col == 0) 1 else 0
                frame = row!! * 3 + col!!
                currentTimeToFrame = 0
            } else if (!moving) {
                if (row == null || row == 0) row = 1
                col = 2
                currentTimeToFrame = 0
                frame = row!! * 3 + col!!
            }
            else -> {}
        }
    }

    private fun agroCheck() {
        if (isFreed) return
        val player = system.player
        val inRange = system.playerOverlapsCircle(position.x.toDouble(), position.y.toDouble(), visionRange * sizeModifier)
        val hasSight = inRange && hasLineOfSight()
        if (hasSight) {
            agro += (11 - agro) / 3
            isIdle = false
        } else if (agro > 0) {
            agro -= 1
        } else {
            isIdle = true
        }
        if (agro > 0 && inRange) agent.setTarget(player.position)
    }

    private fun hasLineOfSight(): Boolean {
        val p = system.player.position
        val dx = (p.x - position.x).toDouble()
        val dy = (p.y - position.y).toDouble()
        if (sqrt(dx * dx + dy * dy) > visionRange) return false
        return system.firstHitIsPlayer(position, p, this)
    }

    fun takeDamage(amount: Double) {
        if (hp > 0) {
            hp -= amount
            if (hp <= 0) {
                hp = 0.0
                die()
            }
            updateHealthBar()
        }
    }

    private fun updateHealthBar() {
        if (healthBarValue == maxHp) healthBarAlpha = 1.0
        val tween = system.runtime.createTween(this)
        tween.tweenProperty({ healthBarValue }, { healthBarValue = it }, hp, 0.4)
    }

    private fun die() {
        isDead = true
        system.enemyCount--
        dmg = 0.0
        walkSpeed = 0.0
        val tween = system.runtime.createTween(this)
        tween.tweenProperty({ alpha }, { alpha = it }, 0.0, 0.3)
        tween.onFinished { system.remove(this) }
    }

    internal fun flushHitRange() {
        if (isFreed) return
        val overlapping = !hitRangeDisabled && system.playerOverlapsCircle(position.x.toDouble(), position.y.toDouble(), reach * sizeModifier)
        if (overlapping && !hitRangeInside) {
            hitRangeInside = true
            attack()
        } else if (!overlapping) {
            hitRangeInside = false
        }
    }

    private fun attack() {
        if (isDead) return
        val runtime = system.runtime
        runtime.callDeferred { hitRangeDisabled = true }
        val auxSpeed = walkSpeed
        walkSpeed = 0.0
        runtime.createTimer(hitWindup, processAlways = false).onTimeout {
            if (isFreed) return@onTimeout
            val target = system.player.position
            val angle = atan2((target.y - position.y).toDouble(), (target.x - position.x).toDouble())
            val params: AttackParams = EnemyAttacks.forType(type) ?: return@onTimeout
            system.spawnProjectile(params, angle, dmg * dmgMult, position)
            val roll = if (type == EnemyType.THIEF || type == EnemyType.CRIMINAL) runtime.random.randi() % 10 else 0L
            system.onEnemySound(type, roll)
            runtime.createTimer(hitCooldown, processAlways = false).onTimeout {
                if (isFreed) return@onTimeout
                runtime.callDeferred { hitRangeDisabled = false }
                walkSpeed = auxSpeed
            }
        }
    }

    companion object {
        const val TIME_TO_FRAME = 15

        fun statsFor(type: EnemyType): EnemyStats = EnemyTypes.of(type)
    }
}
