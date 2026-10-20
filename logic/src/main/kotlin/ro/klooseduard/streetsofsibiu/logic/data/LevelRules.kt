package ro.klooseduard.streetsofsibiu.logic.data

import ro.klooseduard.streetsofsibiu.logic.data.rooms.DoorOrientation
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog

enum class RoomType(val godotName: String) { SMALL("small"), BIG("big"), FINISHER("finisher") }

data class RoomPool(val rooms: List<String>, val type: RoomType)

object LevelRules {
    const val LEVEL_COUNT = 8

    fun generationDepth(level: Int): Int = when (level) {
        in 1..5 -> 2
        in 6..8 -> 3
        else -> error("no level $level")
    }

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

    const val EXIT_SPAWN_NODES = 4

    const val BOSS_LEVEL = 8

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

    fun roomReward(type: RoomType): RewardPool? = when (type) {
        RoomType.SMALL -> RewardPool.SMALL_UPGRADES
        RoomType.BIG -> RewardPool.BIG_UPGRADES
        RoomType.FINISHER -> null
    }

    const val ROOM_CLEAR_DELAY = 1.0

    const val REWARD_WINDOW_GAP = 0.1
}

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

    const val DEFAULT_SPAWN_RADIUS = 100
}
