extends Node2D
class_name EnemySpawnNode

@onready var enemy := preload("res://enemy/Enemy.tscn")
@onready var supportEnemy := preload("res://enemy/SupportEnemy.tscn")
@onready var spawnerEnemy := preload("res://enemy/SpawnerEnemy.tscn")

@onready var enemyTypeList :Dictionary={
	"rat": preload("res://enemy/enemy_types/RatType.tres"),
	"pigeon": preload("res://enemy/enemy_types/PigeonType.tres"),
	"thief": preload("res://enemy/enemy_types/ThiefType.tres"),
	"criminal": preload("res://enemy/enemy_types/CriminalType.tres"),
	"ghost": preload("res://enemy/enemy_types/GhostType.tres"),
	"marshmallow": preload("res://enemy/enemy_types/MarshMallowType.tres")
}
@onready var mainBg :MainBackground= $"../.." # xd

@export var spawnRadius := 100

var spawnPos

func _ready() -> void:
	if spawnPos:
		global_position = spawnPos
	await spawnEnemies()
	queue_free()

func spawnEnemies():
	match mainBg.currentLevel:
		1: # level 1: 2-3 rats
			for i in range(randi_range(1, 2)):
				spawnEnemy("rat")
		2: # level 2: 3-4 rats/pigeons
			for i in range(randi_range(2, 3)):
				spawnEnemy(["rat", "pigeon"][randi_range(0, 1)])
		3: # level 3: 4-5 enemies, rat/pigeon/thief
			for i in range(randi_range(2, 4)):
				spawnEnemy(["rat", "pigeon", "thief"][randi_range(0, 2)])
		4: # level 4: 5-6 enemies, pigeon/thief x2
			for i in range(randi_range(3, 4)):
				spawnEnemy(["pigeon", "thief", "thief"][randi_range(0, 2)])
		5: # level 5: 6-7 enemies, rat/thief/criminal
			for i in range(randi_range(2, 5)):
				spawnEnemy(["rat", "thief", "criminal"][randi_range(0, 2)])
		6: # level 6: 6-7 enemies, rat/pigeon/thief/criminal/ghost
			for i in range(randi_range(3, 5)):
				var enemyArr := []
				for j in range(3): enemyArr.append("rat")
				for j in range(3): enemyArr.append("pigeon")
				for j in range(4): enemyArr.append("thief")
				for j in range(2): enemyArr.append("criminal")
				for j in range(1): enemyArr.append("ghost")
				spawnEnemy(enemyArr[randi_range(0, enemyArr.size()-1)])
		7: # level 7: 6-7 enemies, rat/pigeon/thief/criminal/ghost/marshmallow
			for i in range(randi_range(3, 5)):
				var enemyArr := []
				for j in range(2): enemyArr.append("rat")
				for j in range(2): enemyArr.append("pigeon")
				for j in range(4): enemyArr.append("thief")
				for j in range(2): enemyArr.append("criminal")
				for j in range(1): enemyArr.append("ghost")
				for j in range(1): enemyArr.append("marshmallow")
				spawnEnemy(enemyArr[randi_range(0, enemyArr.size()-1)])
		8: # level 8: 6-7 enemies, rat/pigeon/thief/criminal/ghost/marshmallow
			for i in range(randi_range(3, 5)):
				var enemyArr := []
				for j in range(2): enemyArr.append("rat")
				for j in range(2): enemyArr.append("pigeon")
				for j in range(4): enemyArr.append("thief")
				for j in range(2): enemyArr.append("criminal")
				for j in range(1): enemyArr.append("ghost")
				for j in range(1): enemyArr.append("marshmallow")
				spawnEnemy(enemyArr[randi_range(0, enemyArr.size()-1)])
			
			

func spawnEnemy(type):
	var enemyInstance
	if type in ["rat", "pigeon", "thief", "criminal"]:
		enemyInstance = enemy.instantiate()
	elif type in ["ghost"]:
		enemyInstance = supportEnemy.instantiate()
	elif type in ["marshmallow"]:
		enemyInstance = spawnerEnemy.instantiate()
		
	enemyInstance.stats = enemyTypeList[type]
	enemyInstance.spawnPos = position + Vector2(spawnRadius,0).rotated(deg_to_rad(randi_range(0, 359)))
	get_parent().add_child.call_deferred(enemyInstance)
	await enemyInstance.ready
