package ro.klooseduard.streetsofsibiu.logic.world

sealed interface PlayerItem

class HeldWeapon(
    var currentAmmo: Int,
    val ammoCapacity: Int,
    var dmg: Double,
    val isSniper: Boolean = false,
) : PlayerItem

object HeldConsumable : PlayerItem

enum class PlayerSound { DASH, PARRY, DEATH }
