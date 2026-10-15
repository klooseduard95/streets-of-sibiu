extends Node2D


@onready var interactable :Area2D= $Interactable
@onready var interactingArea :CollisionShape2D= $Interactable/CollisionShape2D
@onready var sprite :Sprite2D= $Sprite
@onready var mainBg :MainBackground= $"../.."
@onready var upgradeWindow := $"../../UpgradeWindow"

var spawnPos

func _ready() -> void:
	if spawnPos: global_position = spawnPos
	interactable.interact = _on_interact


func _on_interact(player: Node):
	interactingArea.set_deferred("disabled", true)
	upgradeWindow.show_upgrades("consumables")
