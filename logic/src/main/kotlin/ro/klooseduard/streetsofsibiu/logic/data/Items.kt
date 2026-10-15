package ro.klooseduard.streetsofsibiu.logic.data

/** `ItemData.Type` (enum order matters: the .tres files store 0/1/2). */
enum class ItemType { PRIMARY, SECONDARY, CONSUMABLE }

/** Which projectile scene a weapon fires (`projectile_scene`). */
enum class ProjectileKind(val scene: String) { RANGED("player/bullet.tscn"), MELEE("player/bullet_melee.tscn") }

/**
 * A `WeaponItem` resource. Defaults follow weapon_item.gd (`bullet_speed` 1200, `is_ramping`
 * false, `ramp_gain_per_hit` 1.0, `ramp_decay_per_sec` 2.0). `ammoCapacity == -1` means infinite.
 */
data class WeaponDef(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val type: ItemType,
    val projectile: ProjectileKind,
    val modifiers: Modifiers = emptyMap(),
    val dmg: Double,
    val currentAmmo: Int,
    val ammoCapacity: Int,
    val fireRate: Double,
    val critChance: Double = 0.0,
    val reloadTime: Double = 0.0,
    val maxRange: Double,
    val isSniper: Boolean = false,
    val bulletSpeed: Double = 1200.0,
    val sound: String,
    val isRamping: Boolean = false,
    val rampGainPerHit: Double = 1.0,
    val rampDecayPerSec: Double = 2.0,
)

/** A `ConsumableItem` resource (consumable_item.gd defaults: 0 / empty). */
data class ConsumableDef(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val hp: Int = 0,
    val shield: Int = 0,
    val buffDuration: Double = 0.0,
    val modifiers: Modifiers = emptyMap(),
    val afterEffectDuration: Double = 0.0,
    val afterEffectModifiers: Modifiers = emptyMap(),
) {
    val type: ItemType get() = ItemType.CONSUMABLE
}

private const val ICONS = "item/item_assets/item icons/"

/** The eight weapon resources under `item/weapons/`; [id] is the file name without extension. */
object Weapons {
    val PUNCH = WeaponDef(
        id = "secondary_punch", name = "Punch",
        description = "2 Damage\n0.2s Cooldown\n1 Tile Range",
        icon = ICONS + "punch.png", type = ItemType.SECONDARY, projectile = ProjectileKind.MELEE,
        dmg = 2.0, currentAmmo = -1, ammoCapacity = -1, fireRate = 5.0, maxRange = 150.0,
        bulletSpeed = 800.0, sound = "Music/Slash.mp3",
    )
    val KNIFE = WeaponDef(
        id = "secondary_knife", name = "Knife",
        description = "5 Damage\n0.5s Cooldown\n1 Tile Range",
        icon = ICONS + "knife.png", type = ItemType.SECONDARY, projectile = ProjectileKind.MELEE,
        dmg = 5.0, currentAmmo = -1, ammoCapacity = -1, fireRate = 2.5, maxRange = 150.0,
        bulletSpeed = 800.0, sound = "Music/Slash.mp3",
    )
    val BAT = WeaponDef(
        id = "secondary_bat", name = "Bat",
        description = "8 Damage\n0.5s Cooldown\n2 Tile Range",
        icon = ICONS + "bat.png", type = ItemType.SECONDARY, projectile = ProjectileKind.MELEE,
        dmg = 8.0, currentAmmo = -1, ammoCapacity = -1, fireRate = 2.0, maxRange = 250.0,
        bulletSpeed = 800.0, sound = "Music/Slash.mp3",
    )
    val CHAINSAW = WeaponDef(
        id = "secondary_chainsaw", name = "Chainsaw",
        description = "1 Base Damage\n+0.40 dmg per successful hit\n-1.5 dmg per 0.5s you are not hitting\n0.125/s Cooldown\n1 Tile Range",
        icon = ICONS + "chainsaw.png", type = ItemType.SECONDARY, projectile = ProjectileKind.MELEE,
        dmg = 1.0, currentAmmo = -1, ammoCapacity = -1, fireRate = 8.0, maxRange = 150.0,
        bulletSpeed = 800.0, sound = "Music/Chainsaw.mp3", isRamping = true, rampGainPerHit = 0.4,
    )
    val MEDIUM_RIFLE = WeaponDef(
        id = "primary_medium", name = "Medium Rifle",
        description = "4 Damage / Bullet\n15 Ammo Capacity\nNormal Fire Speed\n0.75s Reload Time\n5 Tile Range",
        icon = ICONS + "rifle.png", type = ItemType.PRIMARY, projectile = ProjectileKind.RANGED,
        dmg = 4.0, currentAmmo = 15, ammoCapacity = 15, fireRate = 3.0, reloadTime = 0.75, maxRange = 550.0,
        sound = "Music/Gunshot.mp3",
    )
    val SMG = WeaponDef(
        id = "primary_short", name = "SMG",
        description = "3 Damage / Bullet\n30 Ammo Capacity\nFast Fire Speed\n1.5s Reload Time\n5 Tile Range\n+10% Movement Speed while holding",
        icon = ICONS + "smg.png", type = ItemType.PRIMARY, projectile = ProjectileKind.RANGED,
        modifiers = mapOf(Stat.SPEED to 0.1),
        dmg = 3.0, currentAmmo = 30, ammoCapacity = 30, fireRate = 9.0, reloadTime = 1.5, maxRange = 550.0,
        bulletSpeed = 1300.0, sound = "Music/Gunshot.mp3",
    )
    val NEGEV = WeaponDef(
        id = "primary_alternative", name = "Negev",
        description = "2.5 Damage / Bullet\n60 Ammo Capacity\nFast Fire Speed\n2.5s Reload Time\n6 Tile Range\n-30% Movement Speed while holding",
        icon = ICONS + "negev.png", type = ItemType.PRIMARY, projectile = ProjectileKind.RANGED,
        modifiers = mapOf(Stat.SPEED to -0.3),
        dmg = 2.5, currentAmmo = 60, ammoCapacity = 60, fireRate = 10.0, reloadTime = 2.5, maxRange = 650.0,
        bulletSpeed = 1350.0, sound = "Music/Gunshot.mp3",
    )
    val SNIPER = WeaponDef(
        id = "primary_sniper", name = "Sniper Rifle",
        description = "25 Damage / Bullet\n1 Ammo Capacity\n1s Reload time\nInfinite Range\n+10% Critical Chance while holding",
        icon = ICONS + "sniper.png", type = ItemType.PRIMARY, projectile = ProjectileKind.RANGED,
        modifiers = mapOf(Stat.CRIT_CHANCE to 10.0),
        dmg = 25.0, currentAmmo = 1, ammoCapacity = 1, fireRate = 10.0, critChance = 10.0, reloadTime = 1.0,
        maxRange = 3000.0, isSniper = true, bulletSpeed = 1700.0, sound = "Music/Gunshot.mp3",
    )

    val all = listOf(PUNCH, KNIFE, BAT, CHAINSAW, MEDIUM_RIFLE, SMG, NEGEV, SNIPER)
}

/** The eight consumable resources in `item/consumables/`; [id] is the file name without extension. */
object Consumables {
    val HEALTH = ConsumableDef(
        id = "consumable_health", name = "Health Potion", description = "+40 HP",
        icon = ICONS + "heal bottle.png", hp = 40,
    )
    val SHIELD = ConsumableDef(
        id = "consumable_shield", name = "Shield", description = "+150 Shield\n-33% Movement Speed\nDuration: 10s",
        icon = ICONS + "Shield.png", shield = 150, buffDuration = 10.0, modifiers = mapOf(Stat.SPEED to -0.33),
    )
    val SPEED = ConsumableDef(
        id = "consumable_speed", name = "Speed Potion", description = "x2 Movement Speed\nDuration: 20s",
        icon = ICONS + "newbottle3.png", buffDuration = 20.0, modifiers = mapOf(Stat.SPEED to 1.0),
    )
    val RAGE = ConsumableDef(
        id = "consumable_x2dmg", name = "Rage Potion", description = "x2 Damage\nDuration: 10s",
        icon = ICONS + "newbottle2.png", buffDuration = 10.0, modifiers = mapOf(Stat.DAMAGE_BONUS to 100.0),
    )
    val ARMOR = ConsumableDef(
        id = "consumable_dmg_reduction", name = "Armor", description = "+30 Raw Damage Reduction\nDuration: 10s",
        icon = ICONS + "newbottle4.png", buffDuration = 10.0, modifiers = mapOf(Stat.DMG_REDUCTION_RAW to 30.0),
    )
    val STUN_GRENADE = ConsumableDef(
        id = "consumable_enemy_stun", name = "Stun Grenade", description = "Stun All Enemies\n-20 HP\nDuration: 6s",
        icon = ICONS + "newbottle3.png", hp = -20, buffDuration = 6.0, modifiers = mapOf(Stat.SLOWN_ENEMY to -1.0),
    )
    val GHOST = ConsumableDef(
        id = "consumable_ghost", name = "Ghost", description = "Become Invincible\nDuration: 5s",
        icon = ICONS + "newbottle1.png", buffDuration = 5.0, modifiers = mapOf(Stat.INVINCIBLE to 1.0),
    )
    val ADRENALINE = ConsumableDef(
        id = "consumable_ups_and_downs", name = "Adrenaline",
        description = "x1.5 Damage\nDuration: 20s\n\nAfter Effect:\nx0.5 Damage\nDuration: 10s",
        icon = ICONS + "newbottle2.png", buffDuration = 20.0, modifiers = mapOf(Stat.DAMAGE_BONUS to 50.0),
        afterEffectDuration = 10.0, afterEffectModifiers = mapOf(Stat.DAMAGE_BONUS to -50.0),
    )

    val all = listOf(HEALTH, SHIELD, SPEED, RAGE, ARMOR, STUN_GRENADE, GHOST, ADRENALINE)
}
