package ro.klooseduard.streetsofsibiu.logic.data

/** `Player.DashType` (stored as ints in scenes: NONE 0, SHORT 1, MEDIUM 2, LONG 3). */
enum class DashType { NONE, SHORT, MEDIUM, LONG }

/**
 * `Player.ParryType` (NORMAL 0, MEDIUM 1, CHARGE 2). The player scene starts with the
 * out-of-range value 3, i.e. no parry until one is unlocked: modelled as `null`.
 */
enum class ParryType { NORMAL, MEDIUM, CHARGE }

/** One entry of an upgrade pool in upgrade_window.gd. [desc] and [icon] are shown on the card. */
sealed class UpgradeCard {
    abstract val desc: String
    abstract val icon: String

    data class StatCard(override val desc: String, override val icon: String, val modifiers: Modifiers) : UpgradeCard()
    data class WeaponCard(override val desc: String, override val icon: String, val item: WeaponDef) : UpgradeCard()
    data class ConsumableCard(override val desc: String, override val icon: String, val item: ConsumableDef) : UpgradeCard()
    data class DashCard(override val desc: String, override val icon: String, val dash: DashType) : UpgradeCard()
    data class ParryCard(override val desc: String, override val icon: String, val parry: ParryType) : UpgradeCard()
}

/**
 * The `reward_type` values accepted by `show_upgrades`, with how many cards are shown and whether
 * the pool is shuffled first. SMALL_CONSUMABLE exists in the original but is never requested.
 */
enum class RewardPool(val godotName: String, val numChoices: Int, val shuffle: Boolean) {
    SMALL_UPGRADES("small_upgrades", 3, true),
    BIG_UPGRADES("big_upgrades", 3, true),
    FIRST_WEAPONS("first_weapons", 2, true),
    SECOND_WEAPONS("second_weapons", 3, false),
    DASH("dash", 2, false),
    SMALL_DASH("small_dash", 1, false),
    KNIFE("knife", 1, false),
    BASIC_GUN("basic_gun", 1, false),
    BASIC_PARRY("basic_parry", 1, false),
    PARRY("parry", 2, false),
    CONSUMABLES("consumables", 2, true),
    SMALL_CONSUMABLE("small_consumable", 2, true),
}

private const val ICONS = "item/item_assets/item icons/"
private const val BULLET_ICON = "enemy/enemy_projectile/enemy_projectile_assets/bullet.png"

/** The card pools of upgrade_window.gd, in declaration order. */
object UpgradePools {
    val smallUpgrades = listOf(
        UpgradeCard.StatCard("+1 ammo capacity\nor\n0.05sec sniper cooldown reduction", BULLET_ICON,
            mapOf(Stat.BONUS_WEAPON_CAPACITY to 1.0, Stat.SNIPER_COOLDOWN_REDUCTION to 0.05)),
        UpgradeCard.StatCard("+15 max hp", ICONS + "heal bottle.png", mapOf(Stat.MAX_HP to 15.0)),
        UpgradeCard.StatCard("+1% speed", ICONS + "newbottle3.png", mapOf(Stat.SPEED to 0.01)),
        UpgradeCard.StatCard("+0.5 damage reduction", ICONS + "newbottle1.png", mapOf(Stat.DMG_REDUCTION_RAW to 0.5)),
        UpgradeCard.StatCard("+2% damage bonus", ICONS + "punch.png", mapOf(Stat.DAMAGE_BONUS to 2.0)),
        UpgradeCard.StatCard("+10 shield", ICONS + "Shield.png", mapOf(Stat.SHIELD to 10.0)),
    )

    val bigUpgrades = listOf(
        UpgradeCard.StatCard("+5 ammo capacity\nor\n0.1sec sniper cooldown reduction", BULLET_ICON,
            mapOf(Stat.BONUS_WEAPON_CAPACITY to 5.0, Stat.SNIPER_COOLDOWN_REDUCTION to 0.1)),
        UpgradeCard.StatCard("+20 max hp", ICONS + "heal bottle.png", mapOf(Stat.MAX_HP to 20.0)),
        UpgradeCard.StatCard("+5% speed", ICONS + "newbottle3.png", mapOf(Stat.SPEED to 0.05)),
        UpgradeCard.StatCard("+2 damage reduction", ICONS + "newbottle3.png", mapOf(Stat.DMG_REDUCTION_RAW to 2.0)),
        UpgradeCard.StatCard("+50 shield", ICONS + "Shield.png", mapOf(Stat.SHIELD to 50.0)),
        UpgradeCard.StatCard("+3% crit chance", ICONS + "knife.png", mapOf(Stat.CRIT_CHANCE to 3.0)),
        UpgradeCard.StatCard("+5% enemy slow", ICONS + "newbottle4.png", mapOf(Stat.SLOWN_ENEMY to -0.05)),
        UpgradeCard.StatCard("+10% damage bonus", ICONS + "punch.png", mapOf(Stat.DAMAGE_BONUS to 10.0)),
        UpgradeCard.StatCard("+30 max hp\n-5% speed", ICONS + "heal bottle.png", mapOf(Stat.MAX_HP to 30.0, Stat.SPEED to -0.05)),
        UpgradeCard.StatCard("+25% damage bonus\n-20 max hp", ICONS + "punch.png", mapOf(Stat.DAMAGE_BONUS to 25.0, Stat.MAX_HP to -20.0)),
    )

    val firstWeaponChoice = listOf(
        UpgradeCard.WeaponCard("Bat", ICONS + "bat.png", Weapons.BAT),
        UpgradeCard.WeaponCard("Chainsaw", ICONS + "chainsaw.png", Weapons.CHAINSAW),
    )

    val secondWeaponChoice = listOf(
        UpgradeCard.WeaponCard("SMG", ICONS + "smg.png", Weapons.SMG),
        UpgradeCard.WeaponCard("Negev", ICONS + "negev.png", Weapons.NEGEV),
        UpgradeCard.WeaponCard("Sniper", ICONS + "sniper.png", Weapons.SNIPER),
    )

    val dashChoices = listOf(
        UpgradeCard.DashCard(
            "Medium Dash\n2 Tiles\nReloads 2 bullets\nReduces sniper cooldown by 0.2s\nHas invincibility frames\n0.5s Cooldown",
            ICONS + "dash2.png", DashType.MEDIUM),
        UpgradeCard.DashCard(
            "Teleport Dash\nMove to cursor\n-33% Speed for 1s\nHas invincibility frames\n2s Cooldown",
            ICONS + "dash1.png", DashType.LONG),
    )

    val smallDashChoice = listOf(
        UpgradeCard.DashCard("Short Dash\n2 Tiles\nHas invincibility frames\n1s Cooldown", ICONS + "normaldash.png", DashType.SHORT),
    )

    val knifeChoice = listOf(UpgradeCard.WeaponCard("Knife", ICONS + "knife.png", Weapons.KNIFE))

    val basicGunChoice = listOf(UpgradeCard.WeaponCard("Basic rifle", ICONS + "rifle.png", Weapons.MEDIUM_RIFLE))

    val basicParryChoice = listOf(
        UpgradeCard.ParryCard("Normal Parry\nNegates damage\n0.5s Cooldown", ICONS + "parry1.png", ParryType.NORMAL),
    )

    val parryChoices = listOf(
        UpgradeCard.ParryCard(
            "Charge Parry\nCharges for 1s then parries\nCan't move during charge up\nIf successful x2 primary weapons damage for 1s, reduces parry cooldown by 4s and +25% speed\n10s Cooldown",
            ICONS + "parry2.png", ParryType.CHARGE),
        UpgradeCard.ParryCard("Reload Parry\nNegates damage\nReloads 10 bullets\n1s Cooldown", ICONS + "parry3.png", ParryType.MEDIUM),
    )

    val consumableChoices = listOf(
        UpgradeCard.ConsumableCard("Health Potion", ICONS + "heal bottle.png", Consumables.HEALTH),
        UpgradeCard.ConsumableCard("Shield", ICONS + "Shield.png", Consumables.SHIELD),
        UpgradeCard.ConsumableCard("Speed", ICONS + "newbottle3.png", Consumables.SPEED),
        UpgradeCard.ConsumableCard("Double Damage", ICONS + "newbottle2.png", Consumables.RAGE),
        UpgradeCard.ConsumableCard("Damage Reduction", ICONS + "newbottle4.png", Consumables.ARMOR),
        UpgradeCard.ConsumableCard("Stun Enemies", ICONS + "newbottle3.png", Consumables.STUN_GRENADE),
        UpgradeCard.ConsumableCard("Invincibility", ICONS + "newbottle1.png", Consumables.GHOST),
        UpgradeCard.ConsumableCard("Temporary Damage Bonus", ICONS + "newbottle2.png", Consumables.ADRENALINE),
    )

    /** The pool `show_upgrades` draws from (SMALL_CONSUMABLE is assembled at runtime in the original). */
    fun pool(reward: RewardPool): List<UpgradeCard> = when (reward) {
        RewardPool.SMALL_UPGRADES -> smallUpgrades
        RewardPool.BIG_UPGRADES -> bigUpgrades
        RewardPool.FIRST_WEAPONS -> firstWeaponChoice
        RewardPool.SECOND_WEAPONS -> secondWeaponChoice
        RewardPool.DASH -> dashChoices
        RewardPool.SMALL_DASH -> smallDashChoice
        RewardPool.KNIFE -> knifeChoice
        RewardPool.BASIC_GUN -> basicGunChoice
        RewardPool.BASIC_PARRY -> basicParryChoice
        RewardPool.PARRY -> parryChoices
        RewardPool.CONSUMABLES -> consumableChoices
        RewardPool.SMALL_CONSUMABLE -> error("assembled at runtime from one small upgrade and one consumable")
    }
}
