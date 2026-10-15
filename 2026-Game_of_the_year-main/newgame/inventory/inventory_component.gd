class_name InventoryComponent
extends Node

signal inventory_changed

var items: Array[ItemData] = [null, null, load("res://item/weapons/secondary/secondary_punch.tres"), null, null]
var selected_slot_index: int = 0:
	set(value):
		if items[selected_slot_index] != null:
			items[selected_slot_index].unequip(get_parent())
		
		selected_slot_index = value
		
		if items[selected_slot_index] != null:
			items[selected_slot_index].equip(get_parent())
		
		inventory_changed.emit()

func add_item(item: ItemData) -> bool:
	var valid_indices = get_valid_indices(item.type)
	
	for i in valid_indices:
		if items[i] == null:
			items[i] = item.duplicate()
			if i == selected_slot_index:
				items[i].equip(get_parent())
			inventory_changed.emit()
			print("added %s to slot %d" % [item.name, i])
			return true
	
	return false

func swap_item(new_item: ItemData) -> ItemData:
	var valid_indices = get_valid_indices(new_item.type)
	var target_index = -1
	
	if selected_slot_index in valid_indices:
		target_index = selected_slot_index
		var old_item = items[target_index]
		
		items[target_index] = new_item
		inventory_changed.emit()
		print("swapped %s with %s in slot %d" % [old_item.name, new_item.name, target_index])
		return old_item
	
	return null

func get_valid_indices(type: int) -> Array:
	match type:
		ItemData.Type.PRIMARY:
			return [0, 1]
		ItemData.Type.SECONDARY:
			return [2]
		ItemData.Type.CONSUMABLE:
			return [3,4]
	return []
