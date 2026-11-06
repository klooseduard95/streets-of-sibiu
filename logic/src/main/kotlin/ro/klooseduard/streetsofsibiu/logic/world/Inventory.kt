package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.ConsumableDef
import ro.klooseduard.streetsofsibiu.logic.data.ItemType
import ro.klooseduard.streetsofsibiu.logic.data.PlayerDefs
import ro.klooseduard.streetsofsibiu.logic.data.WeaponDef

class Inventory(private val player: Player) {
    val items: Array<PlayerItem?> = arrayOfNulls(5)
    var onChanged: () -> Unit = {}

    var selected = 0
        set(value) {
            equipChange(items[field], null)
            field = value
            equipChange(null, items[field])
            onChanged()
        }

    init {
        items[2] = HeldWeapon.of(ro.klooseduard.streetsofsibiu.logic.data.Weapons.PUNCH)
    }

    val selectedItem: PlayerItem? get() = items[selected]

    private fun equipChange(old: PlayerItem?, new: PlayerItem?) {
        if (old is HeldWeapon) player.clearWeaponModifiers()
        if (new is HeldWeapon) player.applyWeaponModifiers(new.def?.modifiers ?: emptyMap())
    }

    fun typeOf(item: PlayerItem?): ItemType? = when (item) {
        is HeldWeapon -> item.def?.type
        is HeldConsumable -> ItemType.CONSUMABLE
        null -> null
    }

    fun addItem(def: Any): Boolean {
        val type = typeOfDef(def)
        for (i in PlayerDefs.validSlots(type)) {
            if (items[i] == null) {
                items[i] = fresh(def)
                if (i == selected) equipChange(null, items[i])
                onChanged()
                return true
            }
        }
        return false
    }

    fun swapItem(def: Any): PlayerItem? {
        val type = typeOfDef(def)
        if (selected in PlayerDefs.validSlots(type)) {
            val old = items[selected]
            items[selected] = fresh(def)
            onChanged()
            return old
        }
        return null
    }

    private fun typeOfDef(def: Any): ItemType = when (def) {
        is WeaponDef -> def.type
        is ConsumableDef -> ItemType.CONSUMABLE
        else -> error("Unknown item $def")
    }

    private fun fresh(def: Any): PlayerItem = when (def) {
        is WeaponDef -> HeldWeapon.of(def)
        is ConsumableDef -> HeldConsumable(def)
        else -> error("Unknown item $def")
    }
}
