package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.ConsumableDef
import ro.klooseduard.streetsofsibiu.logic.data.Stat
import ro.klooseduard.streetsofsibiu.logic.data.WeaponDef
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRandom
import ro.klooseduard.streetsofsibiu.logic.runtime.GameRuntime

sealed interface PlayerItem

class HeldWeapon(
    var currentAmmo: Int,
    val ammoCapacity: Int,
    var dmg: Double,
    val isSniper: Boolean = false,
    val def: WeaponDef? = null,
) : PlayerItem {
    var isReloading = false; private set
    var canShoot = true; private set
    var rampStack = 0.0; internal set
    private var lastCalcMs = 0L

    fun totalCapacity(player: Player): Int =
        if (ammoCapacity == -1) -1 else ammoCapacity + player.stat(Stat.BONUS_WEAPON_CAPACITY.godotName, 0.0).toInt()

    fun shoot(player: Player, runtime: GameRuntime, random: GameRandom, aim: Vec2, bullets: BulletSystem) {
        if (isReloading || !canShoot) return
        if (ammoCapacity != -1 && currentAmmo <= 0) {
            startReload(player, runtime)
            return
        }
        val def = def ?: return
        player.onWeaponSound(def.sound)
        val ramping = def.isRamping
        if (ramping) {
            val now = runtime.ticksMsec()
            if (lastCalcMs == 0L) lastCalcMs = now
            val decay = (now - lastCalcMs) / 1000.0 * def.rampDecayPerSec
            rampStack = maxOf(0.0, rampStack - decay)
            lastCalcMs = now
        }
        val dx = (aim.x - player.position.x).toDouble()
        val dy = (aim.y - player.position.y).toDouble()
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        val dirX = if (len > 0) dx / len else 0.0
        val dirY = if (len > 0) dy / len else 0.0
        var multiplier = 1.0
        multiplier += player.stat(Stat.DAMAGE_BONUS.godotName, 0.0) / 100.0
        var almostFinal = dmg
        if (ramping) almostFinal += rampStack
        var finalDamage = almostFinal * multiplier
        val totalCrit = def.critChance + player.stat(Stat.CRIT_CHANCE.godotName, 0.0)
        if (random.randf() * 100 < totalCrit) finalDamage *= 2.0
        val bullet = Bullet(
            position = player.position, dirX = dirX, dirY = dirY, speed = def.bulletSpeed, dmg = finalDamage,
            maxRange = def.maxRange, isMelee = def.projectile == ro.klooseduard.streetsofsibiu.logic.data.ProjectileKind.MELEE,
            weaponIcon = if (ammoCapacity == -1) def.icon else null,
        )
        if (ramping) bullet.onTargetEntered = { rampStack += def.rampGainPerHit }
        bullets.spawn(bullet)
        if (ammoCapacity != -1) {
            currentAmmo -= 1
            if (isSniper) currentAmmo = 0
        }
        canShoot = false
        val delay = if (def.fireRate > 0) 1.0 / def.fireRate else 0.1
        runtime.createTimer(delay, processAlways = false).onTimeout { canShoot = true }
    }

    fun reload(player: Player, runtime: GameRuntime) {
        if (ammoCapacity == -1 || isReloading) return
        if (currentAmmo < totalCapacity(player)) startReload(player, runtime)
    }

    private fun startReload(player: Player, runtime: GameRuntime) {
        isReloading = true
        val time = (def?.reloadTime ?: 0.0) - player.stat(Stat.SNIPER_COOLDOWN_REDUCTION.godotName, 0.0)
        runtime.createTimer(time, processAlways = false).onTimeout {
            currentAmmo = totalCapacity(player)
            isReloading = false
        }
    }

    companion object {
        fun of(def: WeaponDef) = HeldWeapon(def.currentAmmo, def.ammoCapacity, def.dmg, def.isSniper, def)
    }
}

class HeldConsumable(val def: ConsumableDef? = null) : PlayerItem

enum class PlayerSound { DASH, PARRY, DEATH }
