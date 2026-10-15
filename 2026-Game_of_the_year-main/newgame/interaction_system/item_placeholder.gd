extends Area2D
class_name GameItem

@onready var interactable: Area2D = $Interactable
@onready var sprite: Sprite2D = $Sprite2D
@export var item_data: ItemData

func _ready() -> void:
	if item_data and sprite:
		sprite.texture = item_data.icon
	interactable.interact = _on_interact
	
	if item_data:
		interactable.interact_name = "Pick up " + item_data.name
	else:
		interactable.interact_name = "Pick up item"

func _on_interact(player: Node):
	if not interactable.is_interactable:
		return
	
	var inventory = player.get_node_or_null("Inventory")
	
	if inventory and item_data:
		if inventory.add_item(item_data):
			interactable.is_interactable = false
			queue_free()
		else:
			var dropped_item = inventory.swap_item(item_data)
			if dropped_item:
				item_data = dropped_item
				sprite.texture = dropped_item.icon
				interactable.interact_name = "Switch with " + dropped_item.name
			else:
				print("inventory type slots full. hold specific type to swap")
