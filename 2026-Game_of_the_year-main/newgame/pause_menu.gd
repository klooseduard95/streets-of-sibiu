extends CanvasLayer


@export var player: Player
@onready var label: Label = %Label
@onready var mainBackground :MainBackground = $".."

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	visible= false
	get_tree().paused=false

func _on_resume_pressed() -> void:
	visible=false
	get_tree().paused=false
	
func _input(event: InputEvent) -> void:
	if !mainBackground.isInTransition:
		if Input.is_action_just_pressed("escape"):
			if visible:
				visible = false
				get_tree().paused = false
			elif not get_tree().paused:
				pause_game()

func _on_main_menu_pressed() -> void:
	get_tree().paused=false
	get_tree().change_scene_to_file("res://main_menu/main_menu.tscn")

func pause_game() -> void:
	update_stats()
	visible = true
	get_tree().paused = true

func update_stats() -> void:
	if player:
		var txt = "Player Statistics\n"
		txt += "--------------------------\n"
		txt += "Level: %d/8\n" % mainBackground.currentLevel
		txt += "HP: %d / %d\n" % [player.hp, int(player.get_stat("max_hp", player.maxHp))]
		txt += "Shield: %d\n" % int(player.shield)
		txt += "Bonus Damage: %d%%\n" % int(player.get_stat("damage_bonus", 0.0))
		txt += "Speed mult: %.2fX\n" % player.get_stat("speed", 1.0)
		txt += "Critical Chance: %d%%\n" % int(player.get_stat("crit_chance", player.critChance))
		txt += "Enemy Slowness: %d%%\n" % int((1.0 - player.get_stat("slown_enemy", 1.0)) * 100)
		txt += "Damage Reduction: %d\n" % int(player.get_stat("dmg_reduction_raw", player.dmgReductionRaw))
  	txt += "Bonus Ammo Capacity: %d\n" % int(player.get_stat("bonus_weapon_capacity", player.bonusAmmoCapacity))
		txt += "Sniper Cooldown Reduction: -%.2fs\n" % int(player.get_stat("sniper_cooldown_reduction", player.sniperCooldownReduction))

		
		if label:
			label.text = txt
	else:
		print("label error")


func _on_settings_pressed() -> void:
	$Settings.show()
