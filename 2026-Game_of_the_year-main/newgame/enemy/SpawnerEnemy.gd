extends IEnemy
class_name SpawnerEnemy

#@onready var buffArea:= $BuffArea
#@onready var buffHitbox := $BuffArea/BuffAreaHitbox
@onready var tick := $Tick


@onready var enemy := preload("res://enemy/Enemy.tscn")
@onready var enemyTypeList :Array=[
	preload("res://enemy/enemy_types/RatType.tres"),
	preload("res://enemy/enemy_types/PigeonType.tres")
]


func _ready() -> void:
	super.initialize()
	spawnEnemies()
	tick.start(stats.hitCooldown)
	


func spawnEnemies():
	$SpawnSound.play()
	var rng := randi_range(1, 10)
	var amountToSpawn
	var radius := stats.reach
	if rng <= 5:
		amountToSpawn = 2
	elif rng <= 8:
		amountToSpawn = 1
	else: amountToSpawn = 3
	for i in range(amountToSpawn):
		spawnRandomEnemy(radius)

func spawnRandomEnemy(radius):
	var rng := randi_range(0, enemyTypeList.size()-1)
	var spawnPos = position + Vector2(radius, 0).rotated(deg_to_rad(randi_range(1, 360)))
	var enemyInstance :BasicEnemy= enemy.instantiate()
	enemyInstance.stats = enemyTypeList[rng]
	get_parent().add_child.call_deferred(enemyInstance)
	enemyInstance.global_position = spawnPos

func _on_tick_timeout() -> void:
	if !isDead:
		spawnEnemies()
