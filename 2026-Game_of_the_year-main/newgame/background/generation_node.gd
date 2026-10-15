extends Node2D
class_name GenerationNode

@export_enum("disabled", "north", "south", "east", "west") var orientation: String = "disabled"
@onready var connection : GenerationNode = null
@onready var roomConnection : RoomModule = null
@onready var room = $"../.."
@onready var backgroundScene = $"../../.."
@onready var arrivalNode = $ArrivalNode
@onready var warpableArea :CollisionShape2D= $Teleporter/TeleportArea

static var barricade := preload("res://background/Barricade.tscn")


func _on_teleporter_body_entered(body: Node2D) -> void:
	if is_instance_of(body, Player):
		if backgroundScene.canWarp and connection != null:
			backgroundScene.doWarpSequence(connection.room, connection)

func barricadeNode():
	var barricadeInstance := barricade.instantiate()
	barricadeInstance.nodeOrientation = orientation
	barricadeInstance.spawnPos = global_position
	get_parent().add_child.call_deferred(barricadeInstance)
	
func enableWarpable():
	warpableArea.set_deferred("disabled", false)
func disableWarpable():
	warpableArea.set_deferred("disabled", true)
