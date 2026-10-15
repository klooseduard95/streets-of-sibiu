package ro.klooseduard.streetsofsibiu.logic.data

import ro.klooseduard.streetsofsibiu.logic.data.rooms.DoorOrientation
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog

/** Room type label given when a room is generated (`RoomModule.roomType`). */
enum class RoomType(val godotName: String) { SMALL("small"), BIG("big"), FINISHER("finisher") }

/** A pool of candidate rooms with the type label it gives (`generatableRooms` / `generatableRoomTypes`). */
data class RoomPool(val rooms: List<String>, val type: RoomType)

/** Level structure from `main_background.gd`, `Module.gd` and `EnemySpawnNode.gd`. */
object LevelRules {
    const val LEVEL_COUNT = 8

    /** `main_background.gd generateLevel`: depth passed to `generateAll` per level. */
    fun generationDepth(level: Int): Int = when (level) {
        in 1..5 -> 2
        in 6..8 -> 3
        else -> error("no level $level")
    }

    /** `Module.gd generateFromNode`: candidate pools per remaining depth, in declaration order. */
    fun poolsForDepth(depth: Int): List<RoomPool> = when (depth) {
        1 -> listOf(RoomPool(RoomCatalog.finishers, RoomType.FINISHER))
        2 -> listOf(RoomPool(RoomCatalog.smallTwoWays, RoomType.SMALL), RoomPool(RoomCatalog.bigTwoWays, RoomType.BIG))
        3 -> listOf(RoomPool(RoomCatalog.bigTwoWays, RoomType.BIG), RoomPool(RoomCatalog.threeWays, RoomType.SMALL))
        4 -> listOf(RoomPool(RoomCatalog.fourWays, RoomType.SMALL), RoomPool(RoomCatalog.threeWays, RoomType.SMALL))
        else -> listOf(
            RoomPool(RoomCatalog.smallTwoWays, RoomType.SMALL),
            RoomPool(RoomCatalog.threeWays, RoomType.SMALL),
            RoomPool(RoomCatalog.fourWays, RoomType.SMALL),
        )
    }

    /** Barricade cone tiles per closed door (`Barricade.gd`), as inclusive cell ranges relative to the door. */
    fun barricadeCells(orientation: DoorOrientation): List<Pair<Int, Int>> {
        val (start, end) = when (orientation) {
            DoorOrientation.NORTH -> (-2 to 0) to (1 to 0)
            DoorOrientation.SOUTH -> (-2 to -1) to (1 to -1)
            DoorOrientation.EAST -> (-1 to -2) to (-1 to 1)
            DoorOrientation.WEST -> (0 to -2) to (0 to 1)
        }
        return buildList {
            for (x in start.first..end.first) for (y in start.second..end.second) add(x to y)
        }
    }

    /** `FinisherSpecialNode.gd`: the exit room of levels 1-7 gets this many EnemySpawnNodes at its centre. */
    const val EXIT_SPAWN_NODES = 4

    /** Level whose exit room contains King Rotta instead of spawn nodes. */
    const val BOSS_LEVEL = 8

    /**
     * `Module.gd enableWarps` for the exit room: the reward windows shown in order after the room
     * is cleared, each opening 0.1 s (pausable) after the previous one closes. The trailing
     * "big_upgrades" is shown on every level, including 8.
     */
    fun exitRewards(level: Int): List<RewardPool> = when (level) {
        1 -> listOf(RewardPool.KNIFE)
        2 -> listOf(RewardPool.FIRST_WEAPONS, RewardPool.SMALL_DASH)
        3 -> listOf(RewardPool.BASIC_GUN)
        4 -> listOf(RewardPool.DASH)
        5 -> listOf(RewardPool.SECOND_WEAPONS, RewardPool.BASIC_PARRY)
        6 -> listOf(RewardPool.PARRY)
        7 -> listOf(RewardPool.BIG_UPGRADES)
        else -> emptyList()
    } + RewardPool.BIG_UPGRADES

    /** `Module.gd enableWarps` for a cleared non-exit room. */
    fun roomReward(type: RoomType): RewardPool? = when (type) {
        RoomType.SMALL -> RewardPool.SMALL_UPGRADES
        RoomType.BIG -> RewardPool.BIG_UPGRADES
        RoomType.FINISHER -> null
    }

    /** Delay before a cleared room shows its reward and opens its doors (`create_timer(1, 0)`). */
    const val ROOM_CLEAR_DELAY = 1.0

    /** Delay between consecutive reward windows (`create_timer(0.1, 0)`). */
    const val REWARD_WINDOW_GAP = 0.1
}

/**
 * Per-level enemy table of one EnemySpawnNode (`EnemySpawnNode.gd spawnEnemies`):
 * spawn `randi_range(minCount, maxCount)` enemies, each type drawn uniformly from [weightedTypes]
 * (types are repeated to express weights, exactly as the original builds its arrays).
 */
data class SpawnTable(val minCount: Int, val maxCount: Int, val weightedTypes: List<EnemyType>)

object SpawnTables {
    fun forLevel(level: Int): SpawnTable = when (level) {
        1 -> SpawnTable(1, 2, listOf(EnemyType.RAT))
        2 -> SpawnTable(2, 3, listOf(EnemyType.RAT, EnemyType.PIGEON))
        3 -> SpawnTable(2, 4, listOf(EnemyType.RAT, EnemyType.PIGEON, EnemyType.THIEF))
        4 -> SpawnTable(3, 4, listOf(EnemyType.PIGEON, EnemyType.THIEF, EnemyType.THIEF))
        5 -> SpawnTable(2, 5, listOf(EnemyType.RAT, EnemyType.THIEF, EnemyType.CRIMINAL))
        6 -> SpawnTable(3, 5, weighted(EnemyType.RAT to 3, EnemyType.PIGEON to 3, EnemyType.THIEF to 4, EnemyType.CRIMINAL to 2, EnemyType.GHOST to 1))
        7, 8 -> SpawnTable(3, 5, weighted(EnemyType.RAT to 2, EnemyType.PIGEON to 2, EnemyType.THIEF to 4, EnemyType.CRIMINAL to 2, EnemyType.GHOST to 1, EnemyType.MARSHMALLOW to 1))
        else -> error("no level $level")
    }

    private fun weighted(vararg entries: Pair<EnemyType, Int>): List<EnemyType> =
        entries.flatMap { (type, count) -> List(count) { type } }

    /** Default `EnemySpawnNode.spawnRadius` (also what the inherited `null` resolves to, V2). */
    const val DEFAULT_SPAWN_RADIUS = 100
}
