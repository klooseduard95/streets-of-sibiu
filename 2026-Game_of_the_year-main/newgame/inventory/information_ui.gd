extends CanvasLayer

@export var player: Player

@onready var health_bar: ProgressBar = $Control/VBoxContainer/HealthBar
@onready var health_label: Label = $Control/VBoxContainer/HealthBar/Label
@onready var shield_label: Label = $Control/VBoxContainer/HBoxContainer/Label
@onready var ammo_label: Label = $Control/VBoxContainer/HBoxContainer/AmmoLabel

func _ready() -> void:
	if player:
		if not player.is_node_ready():
			await player.ready
		player.health_changed.connect(_on_health_changed)
		player.shield_changed.connect(_on_shield_changed)
		
		player.update_health_ui()

func _process(delta: float) -> void:
	if player and player.inventory:
		var current_item = player.inventory.items[player.inventory.selected_slot_index]
		
		if current_item is WeaponItem:
			ammo_label.show()
			
			if current_item.ammo_capacity == -1:
				ammo_label.text = "Ammo: ∞"
			elif current_item.isReloading:
				ammo_label.text = "Reloading..."
			else:
				var total_cap = current_item.get_total_capacity(player)
				ammo_label.text = "Ammo: %d / %d" % [current_item.current_ammo, total_cap]
		else:
			ammo_label.hide()

func _on_health_changed(current_hp: float, max_hp: float) -> void:
	health_bar.max_value = max_hp
	health_label.text = "HP: %d / %d" % [int(current_hp), int(max_hp)]
	
	var tween = create_tween().set_trans(Tween.TRANS_SINE).set_ease(Tween.EASE_OUT)
	tween.tween_property(health_bar, "value", current_hp, 0.2)

func _on_shield_changed(current_shield: float) -> void:
	shield_label.text = "Shield: %d" % int(current_shield)
	
