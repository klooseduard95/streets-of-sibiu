package ro.klooseduard.streetsofsibiu.logic.data

enum class Stat(val godotName: String) {
    SPEED("speed"),
    DAMAGE_BONUS("damage_bonus"),
    CRIT_CHANCE("crit_chance"),
    DMG_REDUCTION_RAW("dmg_reduction_raw"),
    BONUS_WEAPON_CAPACITY("bonus_weapon_capacity"),
    SNIPER_COOLDOWN_REDUCTION("sniper_cooldown_reduction"),
    MAX_HP("max_hp"),
    SHIELD("shield"),
    SLOWN_ENEMY("slown_enemy"),
    INVINCIBLE("invincible"),
    ;

    companion object {
        fun fromGodot(name: String): Stat = entries.first { it.godotName == name }
    }
}

typealias Modifiers = Map<Stat, Double>
