class_name ItemData
extends Resource

enum Type { PRIMARY, SECONDARY, CONSUMABLE }

@export var name: String = ""
@export_multiline var description: String = ""
@export var icon: Texture2D
@export var type: Type

func equip (player: Node) -> void:
	pass

func unequip(player: Node) -> void:
	pass
