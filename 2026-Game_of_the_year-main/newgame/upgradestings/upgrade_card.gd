extends Panel

@onready var texture_rect: TextureRect = $TextureRect
@onready var label: RichTextLabel = $Label

var upgrade_data: Dictionary = {}
var player_ref: Player
var window_ref: CanvasLayer

func setup_card(data: Dictionary, player: Player, window: CanvasLayer) -> void:
	upgrade_data = data
	player_ref = player
	window_ref = window
	
	if data.has("is_item") and data["is_item"] == true and data.has("item_data"):
		var item = data["item_data"]
		texture_rect.texture = item.icon
		label.text = "[color=coral][u][b]%s[/b][/u][/color]\n[color=MintCream]%s[/color]" % [item.name, item.description]
	else:
		texture_rect.texture = data["icon"]
		label.text = data["desc"]

func _on_button_pressed() -> void:
	if upgrade_data.has("is_item") and upgrade_data["is_item"] == true:
		if window_ref and window_ref.item_drop_scene:
			var dropped_item = window_ref.item_drop_scene.instantiate()
			dropped_item.item_data = upgrade_data["item_data"]
			dropped_item.global_position = player_ref.global_position + Vector2(0,0)
			get_tree().current_scene.add_child(dropped_item)
			print("spawned item: ", upgrade_data["desc"])
	elif upgrade_data.has("dash_style"):
		player_ref.choose_dash_style(upgrade_data["dash_style"])
		print("upgraded dash: ", upgrade_data["desc"])
	elif upgrade_data.has("parry_style"):
		player_ref.selected_parry = upgrade_data["parry_style"]
		print("upgraded parry: ", upgrade_data["desc"])
	elif upgrade_data.has("modifiers"):
		if player_ref.has_method("add_permanent_buff"):
			player_ref.add_permanent_buff(upgrade_data["modifiers"])
			print("stat upgraded: ", upgrade_data["desc"])
	
	if window_ref and window_ref.has_method("close_upgrade_window"):
		window_ref.close_upgrade_window()
