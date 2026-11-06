package ro.klooseduard.streetsofsibiu.logic.world

import ro.klooseduard.streetsofsibiu.logic.data.ItemDef
import ro.klooseduard.streetsofsibiu.logic.math.Vec2
import kotlin.math.sqrt

class Interactable(
    var x: Float,
    var y: Float,
    var interactName: String = "",
    var isInteractable: Boolean = true,
    val radius: Double = GameItem.INTERACTABLE_RADIUS,
    var interact: () -> Unit = {},
) {
    var isFreed = false; internal set
}

class GameItem(position: Vec2, var def: ItemDef) {
    val x = position.x
    val y = position.y
    var isFreed = false; private set
    val interactable = Interactable(position.x, position.y, "Pick up " + def.name)

    init {
        interactable.interact = { pickUp() }
    }

    internal lateinit var player: Player
    internal var onFreed: (GameItem) -> Unit = {}

    private fun pickUp() {
        if (!interactable.isInteractable) return
        val inventory = player.inventory
        if (inventory.addItem(def)) {
            interactable.isInteractable = false
            free()
        } else {
            val dropped = inventory.defOf(inventory.swapItem(def))
            if (dropped != null) {
                def = dropped
                interactable.interactName = "Switch with " + dropped.name
            }
        }
    }

    private fun free() {
        isFreed = true
        interactable.isFreed = true
        onFreed(this)
    }

    companion object {
        const val INTERACTABLE_RADIUS = 95.0
    }
}

class InteractionSystem(private val player: Player) {
    val items = mutableListOf<GameItem>()
    private val current = mutableListOf<Interactable>()
    private var canInteract = true

    var labelVisible = false; private set
    var labelText = ""; private set

    val interactables: List<Interactable> get() = current

    fun spawn(item: GameItem): GameItem {
        item.player = player
        item.onFreed = { items -= it }
        items += item
        return item
    }

    fun clearItems() {
        items.toList().forEach { it.interactable.isFreed = true }
        items.clear()
    }

    fun flushPhysicsQueries() {
        current.removeAll { it.isFreed }
        val px = player.position.x.toDouble()
        val py = player.position.y.toDouble()
        val inRange = items.filter { !it.isFreed && overlaps(px, py, it.interactable) }.map { it.interactable }
        current.removeAll { it !in inRange }
        for (area in inRange) if (area !in current) current += area
    }

    private fun overlaps(px: Double, py: Double, area: Interactable): Boolean {
        val dx = px - area.x
        val dy = py - area.y
        return sqrt(dx * dx + dy * dy) < RANGE_RADIUS + area.radius
    }

    fun interactPressed() {
        if (!canInteract || current.isEmpty()) return
        canInteract = false
        labelVisible = false
        current.first().interact()
        canInteract = true
    }

    fun process() {
        if (current.isNotEmpty() && canInteract) {
            val p = player.position
            current.sortBy { (it.x - p.x) * (it.x - p.x) + (it.y - p.y) * (it.y - p.y) }
            if (current[0].isInteractable) {
                labelText = current[0].interactName
                labelVisible = true
            }
        } else {
            labelVisible = false
        }
    }

    companion object {
        const val RANGE_RADIUS = 30.0 * 1.5
    }
}
