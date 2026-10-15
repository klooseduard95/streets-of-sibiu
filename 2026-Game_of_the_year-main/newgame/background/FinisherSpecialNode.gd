extends Node2D
class_name FinisherNode

var type :String= ""
var spawnPos 

@onready var mainBg :MainBackground= $"../.." # what are you looking for? theres no pot of gold here
@onready var spawnerNode := preload("res://enemy/EnemySpawnNode.tscn")
@onready var kingRottaScene := preload("res://enemy/KingRottaBoss.tscn")
@onready var consuNode := preload("res://background/finishers/ConsumableFinisher.tscn")

const quirkySpawnRadiusRadius = 20 # legit 0 reason for you to look through my kitchen

func _ready() -> void:
	global_position = spawnPos
	if type == "":
		var rng := randi_range(1, 100)
		if rng <= 100:
			spawnConsumable()
	elif type == "exit":
		spawnEnemies()

func spawnConsumable():
	var newConsu = consuNode.instantiate()
	newConsu.spawnPos = global_position
	get_parent().add_child.call_deferred(newConsu)
	


func spawnEnemies():
	if mainBg.currentLevel != 8:
		for i in range(4):
			var newSpawner = spawnerNode.instantiate()
			newSpawner.spawnPos = global_position
			get_parent().add_child.call_deferred(newSpawner)
	else:
		var kingRotta :KingRotta= kingRottaScene.instantiate()
		kingRotta.spawnPos = position
		get_parent().add_child.call_deferred(kingRotta)
	
