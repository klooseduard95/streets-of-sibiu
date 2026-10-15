package ro.klooseduard.streetsofsibiu.logic.parity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.klooseduard.streetsofsibiu.logic.data.AttackKind
import ro.klooseduard.streetsofsibiu.logic.data.AttackParams
import ro.klooseduard.streetsofsibiu.logic.data.Consumables
import ro.klooseduard.streetsofsibiu.logic.data.DashType
import ro.klooseduard.streetsofsibiu.logic.data.EnemyAttacks
import ro.klooseduard.streetsofsibiu.logic.data.EnemyType
import ro.klooseduard.streetsofsibiu.logic.data.EnemyTypes
import ro.klooseduard.streetsofsibiu.logic.data.ItemType
import ro.klooseduard.streetsofsibiu.logic.data.LevelRules
import ro.klooseduard.streetsofsibiu.logic.data.Modifiers
import ro.klooseduard.streetsofsibiu.logic.data.ParryType
import ro.klooseduard.streetsofsibiu.logic.data.PlayerDefs
import ro.klooseduard.streetsofsibiu.logic.data.RewardPool
import ro.klooseduard.streetsofsibiu.logic.data.RoomType
import ro.klooseduard.streetsofsibiu.logic.data.SpawnTables
import ro.klooseduard.streetsofsibiu.logic.data.Stat
import ro.klooseduard.streetsofsibiu.logic.data.UpgradeCard
import ro.klooseduard.streetsofsibiu.logic.data.UpgradePools
import ro.klooseduard.streetsofsibiu.logic.data.Weapons
import ro.klooseduard.streetsofsibiu.logic.data.rooms.DoorOrientation
import ro.klooseduard.streetsofsibiu.logic.data.rooms.RoomCatalog

/** Every transcribed constant must equal the original resources and scripts (read-only). */
class GameDataParityTest {
    private fun modifiers(value: String?): Modifiers =
        GodotText.numberDict(value ?: "{}").map { (k, v) -> Stat.fromGodot(k) to v }.toMap()

    private fun num(props: Map<String, String>, key: String, defaults: Map<String, String>): Double =
        (props[key] ?: defaults[key] ?: "0").toDouble()

    // ---------------------------------------------------------------- items

    @Test
    fun weaponsMatchTheirResources() {
        val defaults = gdscriptDefaults("item/weapon_item.gd")
        val files = listOf("primary", "secondary").flatMap { dir ->
            Original.file("item/weapons/$dir").listFiles()!!.filter { it.extension == "tres" }.map { "$dir/${it.nameWithoutExtension}" }
        }
        assertEquals(files.map { it.substringAfter('/') }.toSet(), Weapons.all.map { it.id }.toSet())
        for (w in Weapons.all) {
            val res = GodotText.load("item/weapons/${files.first { it.endsWith("/" + w.id) }}.tres")
            val p = res.resource
            val msg = w.id
            assertEquals(msg, GodotText.string(p.getValue("name")), w.name)
            assertEquals(msg, GodotText.string(p.getValue("description")), w.description)
            assertEquals(msg, res.extPath(p.getValue("icon")), w.icon)
            assertEquals(msg, ItemType.entries[(p["type"] ?: "0").toInt()], w.type)
            assertEquals(msg, res.extPath(p.getValue("projectile_scene")), w.projectile.scene)
            assertEquals(msg, modifiers(p["modifiers"]), w.modifiers)
            assertEquals(msg, num(p, "dmg", defaults), w.dmg, 0.0)
            assertEquals(msg, num(p, "current_ammo", defaults).toInt(), w.currentAmmo)
            assertEquals(msg, num(p, "ammo_capacity", defaults).toInt(), w.ammoCapacity)
            assertEquals(msg, num(p, "fire_rate", defaults), w.fireRate, 0.0)
            assertEquals(msg, num(p, "critChance", defaults), w.critChance, 0.0)
            assertEquals(msg, num(p, "reloadTime", defaults), w.reloadTime, 0.0)
            assertEquals(msg, num(p, "maxRange", defaults), w.maxRange, 0.0)
            assertEquals(msg, (p["is_sniper"] ?: defaults.getValue("is_sniper")).toBoolean(), w.isSniper)
            assertEquals(msg, num(p, "bullet_speed", defaults), w.bulletSpeed, 0.0)
            assertEquals(msg, res.extPath(p.getValue("weapon_sound")), w.sound)
            assertEquals(msg, (p["is_ramping"] ?: defaults.getValue("is_ramping")).toBoolean(), w.isRamping)
            assertEquals(msg, num(p, "ramp_gain_per_hit", defaults), w.rampGainPerHit, 0.0)
            assertEquals(msg, num(p, "ramp_decay_per_sec", defaults), w.rampDecayPerSec, 0.0)
        }
    }

    @Test
    fun consumablesMatchTheirResources() {
        val files = Original.file("item/consumables").listFiles()!!.filter { it.extension == "tres" }.map { it.nameWithoutExtension }
        assertEquals(files.toSet(), Consumables.all.map { it.id }.toSet())
        for (c in Consumables.all) {
            val res = GodotText.load("item/consumables/${c.id}.tres")
            val p = res.resource
            val msg = c.id
            assertEquals(msg, GodotText.string(p.getValue("name")), c.name)
            assertEquals(msg, GodotText.string(p.getValue("description")), c.description)
            assertEquals(msg, res.extPath(p.getValue("icon")), c.icon)
            assertEquals(msg, "2", p["type"])
            assertEquals(msg, (p["hp"] ?: "0").toInt(), c.hp)
            assertEquals(msg, (p["shield"] ?: "0").toInt(), c.shield)
            assertEquals(msg, (p["buffDuration"] ?: "0").toDouble(), c.buffDuration, 0.0)
            assertEquals(msg, modifiers(p["modifiers"]), c.modifiers)
            assertEquals(msg, (p["after_effect_duration"] ?: "0").toDouble(), c.afterEffectDuration, 0.0)
            assertEquals(msg, modifiers(p["after_effect_modifiers"]), c.afterEffectModifiers)
        }
    }

    // ---------------------------------------------------------------- enemies

    @Test
    fun enemyStatsMatchTheirResources() {
        val defaults = gdscriptDefaults("enemy/enemy_types/EnemyStats.gd")
        val files = mapOf(
            EnemyType.RAT to "RatType", EnemyType.PIGEON to "PigeonType", EnemyType.THIEF to "ThiefType",
            EnemyType.CRIMINAL to "CriminalType", EnemyType.GHOST to "GhostType",
            EnemyType.MARSHMALLOW to "MarshMallowType", EnemyType.KING_ROTTA to "KingRottaType",
        )
        assertEquals(files.values.toSet(), Original.file("enemy/enemy_types").listFiles()!!.filter { it.extension == "tres" }.map { it.nameWithoutExtension }.toSet())
        for ((type, file) in files) {
            val p = GodotText.load("enemy/enemy_types/$file.tres").resource
            val s = EnemyTypes.of(type)
            assertEquals(file, GodotText.string(p.getValue("type")), s.type.godotName)
            assertEquals(file, num(p, "visionRange", defaults), s.visionRange, 0.0)
            assertEquals(file, num(p, "hp", defaults), s.hp, 0.0)
            assertEquals(file, num(p, "dmg", defaults), s.dmg, 0.0)
            assertEquals(file, num(p, "walkSpeed", defaults), s.walkSpeed, 0.0)
            assertEquals(file, num(p, "reach", defaults), s.reach, 0.0)
            assertEquals(file, num(p, "hitCooldown", defaults), s.hitCooldown, 0.0)
            assertEquals(file, num(p, "hitWindup", defaults), s.hitWindup, 0.0)
            assertEquals(file, num(p, "sizeModifier", defaults), s.sizeModifier, 0.0)
            assertEquals(file, defaults.getValue("dmgMult").toInt(), s.dmgMult)
        }
        assertTrue(Original.text("enemy/KingRottaBoss.tscn").contains("path=\"res://enemy/enemy_types/KingRottaType.tres\""))
    }

    @Test
    fun enemyLooksAndLevelScalingMatchIEnemy() {
        val script = Original.text("enemy/IEnemy.gd")
        val assets = Regex(""""(\w+)": load\("res://([^"]+)"\)""").findAll(script).associate { it.groupValues[1] to it.groupValues[2] }
        val looksBlock = script.substringAfter("func updateLooks():").substringBefore("func updateStats")
        val statsBlock = script.substringAfter("func updateStats():")
        for (type in EnemyType.entries.filter { it != EnemyType.KING_ROTTA }) {
            val looks = EnemyTypes.looks(type)!!
            assertEquals(type.name, assets.getValue(type.godotName), looks.texture)
            val branch = looksBlock.substringAfter("\"${type.godotName}\":").substringBefore("\n\t\t\"").substringBefore("\n\n")
            assertEquals(type.name, Regex("hframes = (\\d+)").find(branch)?.groupValues?.get(1)?.toInt() ?: 1, looks.hframes)
            assertEquals(type.name, Regex("vframes = (\\d+)").find(branch)?.groupValues?.get(1)?.toInt() ?: 1, looks.vframes)
            assertEquals(type.name, Regex("frame = (\\d+)").find(branch.replace(Regex("[hv]frames = \\d+"), ""))?.groupValues?.get(1)?.toInt() ?: 0, looks.initialFrame)
            val scaling = EnemyTypes.levelScaling(type)
            val sBranch = Regex("\"${type.godotName}\":\\s*\\n\\s*stats.hp \\+= ([0-9.]+)\\*statIncreaseMult\\s*\\n\\s*stats.dmg \\+= ([0-9.]+) \\* statIncreaseMult").find(statsBlock)
            if (scaling == null) assertEquals(type.name, null, sBranch) else {
                assertEquals(type.name, sBranch!!.groupValues[1].toDouble(), scaling.hpPerLevel, 0.0)
                assertEquals(type.name, sBranch.groupValues[2].toDouble(), scaling.dmgPerLevel, 0.0)
            }
        }
        val king = GodotText.load("enemy/KingRottaBoss.tscn")
        assertEquals("enemy/enemy_assets/KingRotta.png", king.extPath(king.node("EnemySprite").props.getValue("texture")))
    }

    @Test
    fun enemyAttackParametersMatchTheCallSites() {
        val projectile = Original.text("enemy/enemy_projectile/enemy_projectile.gd")
        val signature = Regex("""func initializeProjectile\((.*)\):""").find(projectile)!!.groupValues[1]
        val defaultTravel = Regex("""travelTimeI: float = ([0-9.]+)""").find(signature)!!.groupValues[1].toDouble()
        val defaultSpeed = Regex("""speedI:float = ([0-9.]+)""").find(signature)!!.groupValues[1].toDouble()
        val defaultPiercing = Regex("""piercingI:bool = (\w+)""").find(signature)!!.groupValues[1].toBoolean()
        val defaultSupport = Regex("""isSupportI:bool = (\w+)""").find(signature)!!.groupValues[1].toBoolean()
        val sprites = Regex(""""(\w+)": preload\("res://([^"]+)"\)""").findAll(projectile).associate { it.groupValues[1] to it.groupValues[2] }
        for (kind in AttackKind.entries) {
            assertEquals(kind.name, sprites.getValue(kind.godotName), kind.texture)
            val rot = Regex("""attackName == "${kind.godotName}":\s*\n\s*projectileSprite.rotate\(deg_to_rad\(([0-9.]+)\)\)""").find(projectile)
            assertEquals(kind.name, rot?.groupValues?.get(1)?.toDouble() ?: 0.0, kind.spriteRotationDeg, 0.0)
        }

        fun parse(call: String): AttackParams {
            val args = call.split(',').map { it.trim() }
            val extra = args.drop(4)
            return AttackParams(
                AttackKind.valueOf(GodotText.string(args[0])),
                extra.getOrNull(0)?.toDouble() ?: defaultTravel,
                extra.getOrNull(1)?.toDouble() ?: defaultSpeed,
                extra.getOrNull(2)?.toBoolean() ?: defaultPiercing,
                extra.getOrNull(3)?.toBoolean() ?: defaultSupport,
            )
        }
        val call = Regex("""initializeProjectile\(([^)]*)\)""")
        val enemyScript = Original.text("enemy/enemy.gd").substringAfter("match stats.type:")
        for (type in listOf(EnemyType.RAT, EnemyType.PIGEON, EnemyType.THIEF, EnemyType.CRIMINAL)) {
            val branch = enemyScript.substringAfter("\"${type.godotName}\":").substringBefore("\n\t\t\"")
            assertEquals(type.name, parse(call.find(branch)!!.groupValues[1]), EnemyAttacks.forType(type))
        }
        assertEquals(parse(call.find(Original.text("enemy/KingRotta.gd"))!!.groupValues[1]), EnemyAttacks.KING_ORB)
        val support = call.find(Original.text("enemy/SupportEnemy.gd"))!!.groupValues[1]
        assertEquals(parse(support), EnemyAttacks.GHOST_BUFF)
        assertEquals(support.split(',')[2].trim().toDouble(), EnemyAttacks.GHOST_BUFF_VALUE, 0.0)
    }

    // ---------------------------------------------------------------- upgrades

    private fun cardsOf(script: String, varName: String): List<String> {
        val block = script.substringAfter("var $varName = [").substringBefore("\n]")
        return block.lines().map { it.trim() }.filter { it.startsWith("{") }
    }

    private fun assertCard(msg: String, line: String, card: UpgradeCard) {
        val desc = GodotText.string(Regex(""""desc": ("(?:[^"\\]|\\.)*")""").find(line)!!.groupValues[1])
        assertEquals(msg, desc, card.desc)
        assertEquals(msg, Regex(""""icon": preload\("res://([^"]+)"\)""").find(line)!!.groupValues[1], card.icon)
        val itemPath = Regex(""""item_data": preload\("res://([^"]+)"\)""").find(line)?.groupValues?.get(1)
        when (card) {
            is UpgradeCard.StatCard -> assertEquals(msg, modifiers(Regex(""""modifiers": (\{[^}]*\})""").find(line)!!.groupValues[1]), card.modifiers)
            is UpgradeCard.WeaponCard -> assertTrue(msg, itemPath!!.endsWith("/${card.item.id}.tres") && line.contains("\"is_item\": true"))
            is UpgradeCard.ConsumableCard -> assertTrue(msg, itemPath!!.endsWith("/${card.item.id}.tres") && line.contains("\"is_item\": true"))
            is UpgradeCard.DashCard -> assertEquals(msg, Regex("""Player\.DashType\.(\w+)""").find(line)!!.groupValues[1], card.dash.name)
            is UpgradeCard.ParryCard -> assertEquals(msg, Regex("""Player\.ParryType\.(\w+)""").find(line)!!.groupValues[1], card.parry.name)
        }
    }

    @Test
    fun upgradePoolsMatchUpgradeWindow() {
        val script = Original.text("upgradestings/upgrade_window.gd")
        val pools = mapOf(
            "small_upgrades" to UpgradePools.smallUpgrades, "big_upgrades" to UpgradePools.bigUpgrades,
            "first_weapon_choice" to UpgradePools.firstWeaponChoice, "second_weapon_choice" to UpgradePools.secondWeaponChoice,
            "dash_choices" to UpgradePools.dashChoices, "small_dash_choice" to UpgradePools.smallDashChoice,
            "knife_choice" to UpgradePools.knifeChoice, "basic_gun_choice" to UpgradePools.basicGunChoice,
            "basic_parry_choice" to UpgradePools.basicParryChoice, "parry_choices" to UpgradePools.parryChoices,
            "consumable_choices" to UpgradePools.consumableChoices,
        )
        var cards = 0
        for ((name, pool) in pools) {
            val lines = cardsOf(script, name)
            assertEquals(name, lines.size, pool.size)
            lines.zip(pool).forEachIndexed { i, (line, card) -> assertCard("$name[$i]", line, card); cards++ }
        }
        assertEquals(37, cards)
    }

    @Test
    fun showUpgradesRulesMatch() {
        val body = Original.text("upgradestings/upgrade_window.gd").substringAfter("match reward_type:").substringBefore("if shuffle:")
        val branches = Regex("""\t\t"(\w+)":\n((?:\t\t\t.*\n)+)""").findAll(body).associate { it.groupValues[1] to it.groupValues[2] }
        assertEquals(RewardPool.entries.map { it.godotName }.toSet(), branches.keys)
        for (pool in RewardPool.entries) {
            val branch = branches.getValue(pool.godotName)
            assertEquals(pool.name, Regex("""num_choices = (\d+)""").find(branch)!!.groupValues[1].toInt(), pool.numChoices)
            assertEquals(pool.name, !branch.contains("shuffle = false"), pool.shuffle)
        }
        val varFor = mapOf(
            RewardPool.SMALL_UPGRADES to "small_upgrades", RewardPool.BIG_UPGRADES to "big_upgrades",
            RewardPool.FIRST_WEAPONS to "first_weapon_choice", RewardPool.SECOND_WEAPONS to "second_weapon_choice",
            RewardPool.DASH to "dash_choices", RewardPool.SMALL_DASH to "small_dash_choice", RewardPool.KNIFE to "knife_choice",
            RewardPool.BASIC_GUN to "basic_gun_choice", RewardPool.BASIC_PARRY to "basic_parry_choice",
            RewardPool.PARRY to "parry_choices", RewardPool.CONSUMABLES to "consumable_choices",
        )
        for ((pool, varName) in varFor) assertTrue(pool.name, branches.getValue(pool.godotName).contains("pool = $varName.duplicate()"))
    }

    // ---------------------------------------------------------------- level rules

    @Test
    fun roomPoolsMatchGlobalUtils() {
        val script = Original.text("GlobalUtils.gd")
        fun list(name: String) = Regex("""res://background/modules/(\w+)\.tscn""")
            .findAll(Regex("""var $name\s*:\s*Array\[PackedScene\]\s*=\s*\[([^\]]*)\]""").find(script)!!.groupValues[1]).map { it.groupValues[1] }.toList()
        assertEquals(list("roomSceneList"), RoomCatalog.roomSceneList)
        assertEquals(list("finishers"), RoomCatalog.finishers)
        assertEquals(list("fourWays"), RoomCatalog.fourWays)
        assertEquals(list("threeWays"), RoomCatalog.threeWays)
        assertEquals(list("smallTwoWays"), RoomCatalog.smallTwoWays)
        assertEquals(list("bigTwoWays"), RoomCatalog.bigTwoWays)
    }

    @Test
    fun generationDepthAndPoolsMatch() {
        val depths = Regex("""(\d): await activeRoom.generateAll\((\d)\)""").findAll(Original.text("background/main_background.gd"))
            .associate { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
        assertEquals((1..LevelRules.LEVEL_COUNT).toList(), depths.keys.sorted())
        depths.forEach { (level, depth) -> assertEquals("level $level", depth, LevelRules.generationDepth(level)) }
        assertTrue(Original.text("background/main_background.gd").contains("if currentLevel > 8:"))

        val module = Original.text("background/Module.gd")
        val lists = mapOf(
            "finishers" to RoomCatalog.finishers, "smallTwoWays" to RoomCatalog.smallTwoWays, "bigTwoWays" to RoomCatalog.bigTwoWays,
            "threeWays" to RoomCatalog.threeWays, "fourWays" to RoomCatalog.fourWays,
        )
        val rows = Regex("""(\d|_): generatableRooms = \[([^\]]*)\];\s*;?\s*generatableRoomTypes = \[([^\]]*)\]""").findAll(module).toList()
        assertEquals(5, rows.size)
        for (row in rows) {
            val depth = row.groupValues[1].toIntOrNull() ?: 5
            val names = Regex("""(\w+)\.duplicate\(\)""").findAll(row.groupValues[2]).map { it.groupValues[1] }.toList()
            val types = Regex(""""(\w+)"""").findAll(row.groupValues[3]).map { it.groupValues[1] }.toList()
            val expected = LevelRules.poolsForDepth(depth)
            assertEquals("depth $depth", names.map { lists.getValue(it) }, expected.map { it.rooms })
            assertEquals("depth $depth", types, expected.map { it.type.godotName })
        }
    }

    @Test
    fun exitRewardsAndRoomRewardsMatchModule() {
        val enable = Original.text("background/Module.gd").substringAfter("func enableWarps():").substringBefore("func disableWarps")
        assertTrue(enable.contains("await get_tree().create_timer(${LevelRules.ROOM_CLEAR_DELAY.toInt()}, 0).timeout"))
        assertTrue(enable.contains("create_timer(${LevelRules.REWARD_WINDOW_GAP}, 0)"))
        val exit = enable.substringAfter("if isExitRoom:").substringBefore("\telse:")
        val match = exit.substringAfter("match bg.currentLevel:").substringBefore("\n\t\tupgradeWindow.show_upgrades")
        val perLevel = Regex("""\t\t\t(\d):\n((?:\t\t\t\t.*\n)+)""").findAll(match + "\n").associate { m ->
            m.groupValues[1].toInt() to Regex("""show_upgrades\("(\w+)"\)""").findAll(m.groupValues[2]).map { it.groupValues[1] }.toList()
        }
        val trailing = Regex("""show_upgrades\("(\w+)"\)""").findAll(exit.substringAfter(match)).map { it.groupValues[1] }.toList()
        assertEquals(listOf("big_upgrades"), trailing)
        for (level in 1..LevelRules.LEVEL_COUNT) {
            val expected = (perLevel[level] ?: emptyList()) + trailing
            assertEquals("level $level", expected, LevelRules.exitRewards(level).map { it.godotName })
        }
        val other = enable.substringAfter("\telse:")
        assertTrue(other.contains("\"small\":\n\t\t\t\tupgradeWindow.show_upgrades(\"small_upgrades\")"))
        assertTrue(other.contains("\"big\":\n\t\t\t\tupgradeWindow.show_upgrades(\"big_upgrades\")"))
        assertEquals(RewardPool.SMALL_UPGRADES, LevelRules.roomReward(RoomType.SMALL))
        assertEquals(RewardPool.BIG_UPGRADES, LevelRules.roomReward(RoomType.BIG))
        val finisher = Original.text("background/FinisherSpecialNode.gd")
        assertTrue(finisher.contains("for i in range(${LevelRules.EXIT_SPAWN_NODES}):"))
        assertTrue(finisher.contains("if mainBg.currentLevel != ${LevelRules.BOSS_LEVEL}:"))
    }

    @Test
    fun spawnTablesMatchEnemySpawnNode() {
        val body = Original.text("enemy/EnemySpawnNode.gd").substringAfter("match mainBg.currentLevel:").substringBefore("func spawnEnemy")
        val levels = Regex("""\t\t(\d):[^\n]*\n((?:\t\t\t.*\n|\s*\n)+)""").findAll(body).toList()
        assertEquals((1..8).toList(), levels.map { it.groupValues[1].toInt() })
        for (m in levels) {
            val level = m.groupValues[1].toInt()
            val block = m.groupValues[2]
            val range = Regex("""range\(randi_range\((\d+), (\d+)\)\)""").find(block)!!
            val types = when {
                block.contains("enemyArr.append") -> Regex("""for j in range\((\d+)\): enemyArr.append\("(\w+)"\)""").findAll(block)
                    .flatMap { e -> List(e.groupValues[1].toInt()) { e.groupValues[2] } }.toList()
                block.contains("spawnEnemy([") -> Regex(""""(\w+)"""").findAll(Regex("""spawnEnemy\(\[([^\]]*)\]""").find(block)!!.groupValues[1]).map { it.groupValues[1] }.toList()
                else -> listOf(Regex("""spawnEnemy\("(\w+)"\)""").find(block)!!.groupValues[1])
            }
            val table = SpawnTables.forLevel(level)
            assertEquals("level $level min", range.groupValues[1].toInt(), table.minCount)
            assertEquals("level $level max", range.groupValues[2].toInt(), table.maxCount)
            assertEquals("level $level types", types, table.weightedTypes.map { it.godotName })
        }
        assertTrue(Original.text("enemy/EnemySpawnNode.gd").contains("@export var spawnRadius := ${SpawnTables.DEFAULT_SPAWN_RADIUS}"))
    }

    @Test
    fun barricadeCellsMatchBarricadeScript() {
        val script = Original.text("background/Barricade.gd")
        for (o in DoorOrientation.entries) {
            val m = Regex(""""${o.name.lowercase()}": fillTiles\(Vector2\((-?\d+), (-?\d+)\),Vector2\((-?\d+), (-?\d+)\)\)""").find(script)!!
            val (sx, sy, ex, ey) = m.groupValues.drop(1).map { it.toInt() }
            val expected = (sx..ex).flatMap { x -> (sy..ey).map { y -> x to y } }
            assertEquals(o.name, expected, LevelRules.barricadeCells(o))
        }
    }

    // ---------------------------------------------------------------- player

    @Test
    fun playerConstantsMatchScriptAndScene() {
        val script = Original.text("player/player.gd")
        fun const(name: String) = Regex("""const $name = ([0-9.]+)""").find(script)!!.groupValues[1].toDouble()
        assertEquals(const("BASE_SPEED"), PlayerDefs.BASE_SPEED, 0.0)
        assertEquals(const("BLINK_DISTANCE"), PlayerDefs.BLINK_DISTANCE, 0.0)
        assertEquals(const("SLIDE_MOMENTUM"), PlayerDefs.SLIDE_MOMENTUM, 0.0)
        assertEquals(const("FRICTION"), PlayerDefs.FRICTION, 0.0)
        assertTrue(script.contains("if velocity.length() < ${PlayerDefs.SLIDE_END_SPEED.toInt()}:"))
        assertTrue(script.contains("var maxHp := ${PlayerDefs.MAX_HP}"))
        val dashBlock = script.substringAfter("match selected_dash:").substringBefore("is_sliding = true")
        for (dash in listOf(DashType.SHORT, DashType.MEDIUM, DashType.LONG)) {
            val branch = dashBlock.substringAfter("DashType.${dash.name}:").substringBefore("DashType.")
            assertEquals(dash.name, Regex("""dash_cooldown.start\(([0-9.]+)\)""").find(branch)!!.groupValues[1].toDouble(), PlayerDefs.dashCooldown(dash)!!, 0.0)
        }
        for ((parry, fn) in mapOf(ParryType.NORMAL to "trigger_normal_parry", ParryType.MEDIUM to "trigger_medium_parry", ParryType.CHARGE to "trigger_charge_parry")) {
            val body = script.substringAfter("func $fn():").substringBefore("\nfunc ")
            assertEquals(parry.name, Regex("""parry_timer.start\(([0-9.]+)\)""").find(body)!!.groupValues[1].toDouble(), PlayerDefs.parryCooldown(parry), 0.0)
        }

        val scene = GodotText.load("player/Player_Placeholder.tscn")
        val root = scene.node("Player").props
        assertEquals(PlayerDefs.INITIAL_DASH.ordinal.toString(), root["selected_dash"])
        assertEquals("3", root["selected_parry"])  // out of range: no parry (INITIAL_PARRY = null)
        assertEquals(null, PlayerDefs.INITIAL_PARRY)
        val capsule = scene.sub(scene.node("CollisionShape2D").props.getValue("shape")).props
        assertEquals(capsule.getValue("radius").toDouble(), PlayerDefs.CAPSULE_RADIUS, 0.0)
        assertEquals(capsule.getValue("height").toDouble(), PlayerDefs.CAPSULE_HEIGHT, 0.0)
        assertEquals(scene.node("GhostTimer").props.getValue("wait_time").toDouble(), PlayerDefs.GHOST_INTERVAL, 0.0)
        val sprite = scene.node("PlayerSprite").props
        assertEquals(PlayerDefs.SPRITE_SHEET, scene.extPath(sprite.getValue("texture")))
        assertEquals(PlayerDefs.SPRITE_HFRAMES.toString(), sprite["hframes"])
        assertEquals(PlayerDefs.SPRITE_VFRAMES.toString(), sprite["vframes"])
        for ((name, frames) in PlayerDefs.WALK_ANIMATIONS) {
            val anim = scene.subResources.values.single { it.props["resource_name"] == "\"$name\"" }.props
            assertEquals(name, PlayerDefs.WALK_ANIMATION_LENGTH, anim.getValue("length").toDouble(), 0.0)
            assertEquals(name, PlayerDefs.WALK_ANIMATION_STEP, anim.getValue("step").toDouble(), 0.0)
            assertEquals(name, "1", anim["loop_mode"])
            val keys = anim.getValue("tracks/0/keys")
            assertTrue(name, keys.contains("PackedFloat32Array(0, 0.1, 0.2, 0.3)"))
            assertEquals(name, frames, Regex(""""values": \[([^\]]*)\]""").find(keys)!!.groupValues[1].split(',').map { it.trim().toInt() })
        }
        assertTrue(Original.text("player/ghost.gd").contains("Color(1.0, 1.0, 1.0, 0.0), ${PlayerDefs.GHOST_FADE_TIME})"))
        val interact = GodotText.load("interaction_system/interacting_component.tscn")
        assertEquals("Vector2(1.5, 1.5)", interact.node("InteractRange").props["scale"])
        assertEquals(30.0 * 1.5, PlayerDefs.INTERACT_RADIUS, 0.0)
        assertTrue(interact.sub(interact.node("CollisionShape2D").props.getValue("shape")).props["radius"] == "30.0")
    }

    @Test
    fun initialInventoryAndSlotsMatchInventoryComponent() {
        val script = Original.text("inventory/inventory_component.gd")
        assertTrue(script.contains("""var items: Array[ItemData] = [null, null, load("res://item/weapons/secondary/secondary_punch.tres"), null, null]"""))
        assertEquals(listOf(null, null, Weapons.PUNCH, null, null), PlayerDefs.INITIAL_INVENTORY)
        assertTrue(script.contains("ItemData.Type.PRIMARY:\n\t\t\treturn [0, 1]"))
        assertTrue(script.contains("ItemData.Type.SECONDARY:\n\t\t\treturn [2]"))
        assertTrue(script.contains("ItemData.Type.CONSUMABLE:\n\t\t\treturn [3,4]"))
        assertEquals(listOf(0, 1), PlayerDefs.validSlots(ItemType.PRIMARY))
        assertEquals(listOf(2), PlayerDefs.validSlots(ItemType.SECONDARY))
        assertEquals(listOf(3, 4), PlayerDefs.validSlots(ItemType.CONSUMABLE))
    }

    // ---------------------------------------------------------------- assets

    @Test
    fun everyAssetReferencedByTheDataIsBundled() {
        val paths = Weapons.all.flatMap { listOf(it.icon, it.sound) } +
            Consumables.all.map { it.icon } +
            EnemyType.entries.mapNotNull { EnemyTypes.looks(it)?.texture } +
            AttackKind.entries.map { it.texture } +
            listOf(UpgradePools.smallUpgrades, UpgradePools.bigUpgrades, UpgradePools.firstWeaponChoice, UpgradePools.secondWeaponChoice,
                UpgradePools.dashChoices, UpgradePools.smallDashChoice, UpgradePools.knifeChoice, UpgradePools.basicGunChoice,
                UpgradePools.basicParryChoice, UpgradePools.parryChoices, UpgradePools.consumableChoices).flatten().map { it.icon } +
            listOf(PlayerDefs.SPRITE_SHEET, "tilesetbig.png", "cone.png")
        val missing = paths.toSet().filterNot { java.io.File(Original.assets, it).isFile }
        assertEquals("missing in assets/: $missing", emptyList<String>(), missing)
    }
}
