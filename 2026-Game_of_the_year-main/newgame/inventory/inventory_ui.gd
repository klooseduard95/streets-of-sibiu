extends CanvasLayer

@export var player: CharacterBody2D
@export var empty_slot_texture: Texture2D
@onready var weapon_container: HBoxContainer = $Control/HBoxContainer/WeaponContainer
@onready var consumable_container: VBoxContainer = $Control/HBoxContainer/ConsumableContainer

var selected_color = Color(1, 1, 0, 1)
var normal_color = Color(1, 1, 1, 1)
var empty_color = Color(1, 1, 1, 0.2)

func _ready() -> void:
	if player:
		if not player.is_node_ready():
			await player.ready
		var inventory = player.get_node("Inventory")
		inventory.inventory_changed.connect(update_ui)
		update_ui()

func update_ui():
	var inventory = player.get_node("Inventory")
	var items = inventory.items
	var selected_index = inventory.selected_slot_index
	
	var weapon_slots = weapon_container.get_children()
	for i in range(weapon_slots.size()):
		update_single_slot(weapon_slots[i], items[i], i, selected_index)
	
	var cons_slots = consumable_container.get_children()
	for i in range(cons_slots.size()):
		var real_index = 3 + i
		update_single_slot(cons_slots[i], items[real_index], real_index, selected_index)
	

func update_single_slot(slot_node, item_data, index, selected_index):
	if item_data != null:
		slot_node.texture = item_data.icon
		slot_node.modulate = normal_color
		
		slot_node.tooltip_text = "[color=coral][u][b]%s[/b][/u][/color]\n[color=MintCream]%s[/color]" % [item_data.name, item_data.description]
	else:
		slot_node.texture = empty_slot_texture
		slot_node.modulate = empty_color
	
	if index == selected_index:
		slot_node.modulate = selected_color
