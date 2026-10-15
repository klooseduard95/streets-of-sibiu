package ro.klooseduard.streetsofsibiu.logic.data

/** `EnemyStats.type` values. */
enum class EnemyType(val godotName: String) {
    RAT("rat"), PIGEON("pigeon"), THIEF("thief"), CRIMINAL("criminal"),
    GHOST("ghost"), MARSHMALLOW("marshmallow"), KING_ROTTA("king_rotta"),
}

/**
 * `EnemyStats` resource (enemy/enemy_types/EnemyStats.gd) with its defaults. Mutable copies are
 * made per enemy at runtime (`stats.duplicate(1)`). `dmgMult` is an int in the original, so the
 * ghost's 1.3 buff truncates to 1 (V10).
 */
data class EnemyStats(
    val type: EnemyType,
    val visionRange: Double = 300.0,
    val hp: Double = 100.0,
    val dmg: Double = 5.0,
    val walkSpeed: Double = 50.0,
    val reach: Double = 50.0,
    val hitCooldown: Double = 0.2,
    val hitWindup: Double = 0.1,
    val sizeModifier: Double = 1.0,
    val dmgMult: Int = 1,
)

/** The seven enemy type resources in `enemy/enemy_types/`. */
object EnemyTypes {
    val RAT = EnemyStats(EnemyType.RAT, hp = 10.0, walkSpeed = 300.0, hitCooldown = 0.7)
    val PIGEON = EnemyStats(EnemyType.PIGEON, hp = 5.0, dmg = 2.0, walkSpeed = 400.0, reach = 75.0, hitCooldown = 0.4)
    val THIEF = EnemyStats(EnemyType.THIEF, hp = 30.0, walkSpeed = 300.0, reach = 300.0, hitCooldown = 0.9, sizeModifier = 1.25)
    val CRIMINAL = EnemyStats(
        EnemyType.CRIMINAL, hp = 60.0, dmg = 20.0, walkSpeed = 250.0, reach = 100.0,
        hitCooldown = 1.5, hitWindup = 0.4, sizeModifier = 1.5,
    )
    val GHOST = EnemyStats(
        EnemyType.GHOST, visionRange = 0.0, hp = 30.0, dmg = 0.0, walkSpeed = 0.0, reach = 300.0,
        hitCooldown = 0.0, hitWindup = 0.0, sizeModifier = 1.5,
    )
    val MARSHMALLOW = EnemyStats(
        EnemyType.MARSHMALLOW, visionRange = 0.0, hp = 60.0, dmg = 0.0, walkSpeed = 0.0,
        hitCooldown = 10.0, hitWindup = 0.0, sizeModifier = 1.5,
    )
    val KING_ROTTA = EnemyStats(
        EnemyType.KING_ROTTA, visionRange = 0.0, hp = 2000.0, dmg = 40.0, walkSpeed = 0.0, reach = 0.0,
        hitCooldown = 10.0, hitWindup = 0.0, sizeModifier = 1.5,
    )

    fun of(type: EnemyType): EnemyStats = when (type) {
        EnemyType.RAT -> RAT
        EnemyType.PIGEON -> PIGEON
        EnemyType.THIEF -> THIEF
        EnemyType.CRIMINAL -> CRIMINAL
        EnemyType.GHOST -> GHOST
        EnemyType.MARSHMALLOW -> MARSHMALLOW
        EnemyType.KING_ROTTA -> KING_ROTTA
    }

    /** `IEnemy.assets` + `updateLooks()`: sprite sheet and frame grid per type. */
    data class Looks(val texture: String, val hframes: Int, val vframes: Int, val initialFrame: Int)

    fun looks(type: EnemyType): Looks? = when (type) {
        EnemyType.RAT -> Looks("enemy/enemy_assets/Rat Spritesheet.png", 2, 2, 0)
        EnemyType.PIGEON -> Looks("enemy/enemy_assets/PigeonSpritesheet.png", 2, 2, 0)
        EnemyType.THIEF -> Looks("enemy/enemy_assets/thiefAssets.png", 3, 4, 5)
        EnemyType.CRIMINAL -> Looks("enemy/enemy_assets/criminalAssets.png", 3, 4, 5)
        EnemyType.MARSHMALLOW -> Looks("enemy/enemy_assets/manhole.png", 1, 1, 0)
        EnemyType.GHOST -> Looks("enemy/enemy_assets/ghost.png", 1, 1, 0)
        EnemyType.KING_ROTTA -> null // set in KingRottaBoss.tscn (KingRotta.png, hframes 2)
    }

    /** `IEnemy.updateStats()`: added per (level - 1). */
    data class LevelScaling(val hpPerLevel: Double, val dmgPerLevel: Double)

    fun levelScaling(type: EnemyType): LevelScaling? = when (type) {
        EnemyType.RAT -> LevelScaling(3.0, 1.0)
        EnemyType.PIGEON -> LevelScaling(2.0, 1.0)
        EnemyType.THIEF -> LevelScaling(3.0, 1.5)
        EnemyType.CRIMINAL -> LevelScaling(3.0, 1.5)
        else -> null
    }
}

/** `EnemyProjectile.getSprite` attack names. */
enum class AttackKind(val godotName: String, val texture: String, val spriteRotationDeg: Double) {
    BASIC_SLASH("BASIC_SLASH", "enemy/enemy_projectile/enemy_projectile_assets/claw.png", 45.0),
    BASIC_BULLET("BASIC_BULLET", "enemy/enemy_projectile/enemy_projectile_assets/bullet.png", 0.0),
    KING_ROTTA_BULLET("KING_ROTTA_BULLET", "enemy/enemy_projectile/enemy_projectile_assets/the_orb.png", 0.0),
    GHOST_BULLET("GHOST_BULLET", "enemy/enemy_projectile/enemy_projectile_assets/the_orb_GHOST.png", 0.0),
    CRIMINAL_SLASH("CRIMINAL_SLASH", "item/item_assets/item icons/knife.png", 90.0),
}

/**
 * Arguments of each `initializeProjectile` call site. [speedPerTick] is in px per physics tick
 * (the original adds it every `_physics_process` without delta).
 */
data class AttackParams(val kind: AttackKind, val travelTime: Double, val speedPerTick: Double, val piercing: Boolean, val isSupport: Boolean)

object EnemyAttacks {
    /** enemy.gd attack(): rat and pigeon (defaults travelTime 0.1, speed 0, piercing). */
    val SLASH = AttackParams(AttackKind.BASIC_SLASH, 0.1, 0.0, piercing = true, isSupport = false)
    /** enemy.gd attack(): thief. */
    val THIEF_BULLET = AttackParams(AttackKind.BASIC_BULLET, 6.0, 7.0, piercing = false, isSupport = false)
    /** enemy.gd attack(): criminal. */
    val CRIMINAL_SLASH = AttackParams(AttackKind.CRIMINAL_SLASH, 0.5, 5.0, piercing = false, isSupport = false)
    /** KingRotta.gd shotgunAttack(). */
    val KING_ORB = AttackParams(AttackKind.KING_ROTTA_BULLET, 4.0, 30.0, piercing = false, isSupport = false)
    /** SupportEnemy.gd sendBuff(): "damage" 1.3 is the dmgMult it tries to apply. */
    val GHOST_BUFF = AttackParams(AttackKind.GHOST_BULLET, 2.0, 4.0, piercing = true, isSupport = true)
    const val GHOST_BUFF_VALUE = 1.3

    fun forType(type: EnemyType): AttackParams? = when (type) {
        EnemyType.RAT, EnemyType.PIGEON -> SLASH
        EnemyType.THIEF -> THIEF_BULLET
        EnemyType.CRIMINAL -> CRIMINAL_SLASH
        EnemyType.KING_ROTTA -> KING_ORB
        EnemyType.GHOST -> GHOST_BUFF
        EnemyType.MARSHMALLOW -> null
    }
}
