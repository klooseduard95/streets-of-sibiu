package ro.klooseduard.streetsofsibiu.logic.data.rooms

/** The 20 room modules and the room pools of the original (`GlobalUtils.gd`). */
object RoomCatalog {
    val all: List<RoomDef> get() = GeneratedRooms.all

    private val byId: Map<String, RoomDef> by lazy { all.associateBy { it.id } }

    fun room(id: String): RoomDef = byId[id] ?: error("unknown room $id")

    /** Loaded by `main_background.gd` at the start of every level. */
    const val SPAWN_ROOM = "SpawnRoom"

    /** `GlobalUtils.roomSceneList` (declared but never read by the original code). */
    val roomSceneList = listOf(
        "FIGHTROOMBIGENEMY_1", "FIGHTROOMBIGENEMY_2", "FIGHTROOMBIGENEMY_3", "FIGHTROOMBIGENEMY_4",
        "FIGHTROOMBIGENEMY_1234", "FIGHTROOM_12", "FIGHTROOM_12_BIG", "FIGHTROOM_12_MEDIUM",
        "FIGHTROOM_13_BIG", "FIGHTROOM_13_MEDIUM", "FIGHTROOM_13_SMALL", "FIGHTROOM_24_MEDIUM",
        "FIGHTROOM_34_BIG", "FIGHTROOM_34_SMALL", "FIGHTROOM_1234_SMALL", "FIGHTROOM_234_SMALL",
        "FIGHTROOM_134_SMALL", "FIGHTROOM_124_SMALL", "FIGHTROOM_123_SMALL",
    )

    /** `GlobalUtils.finishers`: dead ends, generated at depth 1. */
    val finishers = listOf("FIGHTROOMBIGENEMY_1", "FIGHTROOMBIGENEMY_2", "FIGHTROOMBIGENEMY_3", "FIGHTROOMBIGENEMY_4")

    /** `GlobalUtils.fourWays`. */
    val fourWays = listOf("FIGHTROOMBIGENEMY_1234", "FIGHTROOM_1234_SMALL")

    /** `GlobalUtils.threeWays`. */
    val threeWays = listOf("FIGHTROOM_234_SMALL", "FIGHTROOM_134_SMALL", "FIGHTROOM_124_SMALL", "FIGHTROOM_123_SMALL")

    /** `GlobalUtils.smallTwoWays`. */
    val smallTwoWays = listOf("FIGHTROOM_12", "FIGHTROOM_13_SMALL", "FIGHTROOM_34_SMALL")

    /** `GlobalUtils.bigTwoWays`. */
    val bigTwoWays = listOf(
        "FIGHTROOM_12_BIG", "FIGHTROOM_12_MEDIUM", "FIGHTROOM_13_BIG",
        "FIGHTROOM_13_MEDIUM", "FIGHTROOM_24_MEDIUM", "FIGHTROOM_34_BIG",
    )
}
