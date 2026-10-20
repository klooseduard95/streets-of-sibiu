package ro.klooseduard.streetsofsibiu.logic.data

object PlayerDefs {
    const val BASE_SPEED = 500.0
    const val BLINK_DISTANCE = 100.0
    const val SLIDE_MOMENTUM = 1200.0
    const val FRICTION = 3000.0
    const val SLIDE_END_SPEED = 10.0
    const val MAX_HP = 100.0

    val INITIAL_DASH = DashType.NONE
    val INITIAL_PARRY: ParryType? = null

    const val CAPSULE_RADIUS = 30.0
    const val CAPSULE_HEIGHT = 85.0

    const val GHOST_INTERVAL = 0.15
    const val GHOST_FADE_TIME = 0.75

    const val INTERACT_RADIUS = 30.0 * 1.5

    fun dashCooldown(dash: DashType): Double? = when (dash) {
        DashType.SHORT -> 1.0
        DashType.MEDIUM -> 0.5
        DashType.LONG -> 2.0
        DashType.NONE -> null
    }

    fun parryCooldown(parry: ParryType): Double = when (parry) {
        ParryType.NORMAL -> 1.0
        ParryType.MEDIUM -> 1.5
        ParryType.CHARGE -> 10.0
    }

    const val SPRITE_SHEET = "player/player_assets/playerassets.png"
    const val SPRITE_HFRAMES = 3
    const val SPRITE_VFRAMES = 4

    val WALK_ANIMATIONS: Map<String, List<Int>> = mapOf(
        "walk_down" to listOf(5, 4, 5, 3),
        "walk_left" to listOf(8, 6, 8, 7),
        "walk_right" to listOf(11, 10, 11, 9),
        "walk_up" to listOf(2, 0, 2, 1),
    )
    const val WALK_ANIMATION_LENGTH = 0.4
    const val WALK_ANIMATION_STEP = 0.1

    val INITIAL_INVENTORY: List<WeaponDef?> = listOf(null, null, Weapons.PUNCH, null, null)

    fun validSlots(type: ItemType): List<Int> = when (type) {
        ItemType.PRIMARY -> listOf(0, 1)
        ItemType.SECONDARY -> listOf(2)
        ItemType.CONSUMABLE -> listOf(3, 4)
    }
}
