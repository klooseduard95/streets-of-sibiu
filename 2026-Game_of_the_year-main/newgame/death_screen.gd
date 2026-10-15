extends CanvasLayer

@onready var label: Label = %Label
@onready var panel: PanelContainer = $PanelContainer

func setup_death_screen(player: Player, level_number: int, is_win: bool = false) -> void:
	get_tree().paused = true
	
	var title_label = $PanelContainer/HBoxContainer/ButtonsContainer/YouDied
	
	if is_win:
		title_label.text = "YOU WIN"
		title_label.modulate = Color(0,1,0)
	else:
		title_label.text = "YOU DIED"
		title_label.modulate = Color(1,1,1)
	
	var txt = "Player Statistics\n"
	txt += "--------------------------\n"
	txt += "Level: %d/8\n" % level_number
	txt += "HP: %d / %d\n" % [player.hp, int(player.get_stat("max_hp", player.maxHp))]
	txt += "Shield: %d\n" % int(player.shield)
	txt += "Bonus Damage: %d%%\n" % int(player.get_stat("damage_bonus", 0.0))
	txt += "Speed mult: %.2fX\n" % player.get_stat("speed", 1.0)
	txt += "Critical Chance: %d%%\n" % int(player.get_stat("crit_chance", player.critChance))
	txt += "Enemy Slowness: %d%%\n" % int((1.0 - player.get_stat("slown_enemy", 1.0)) * 100)
	txt += "Damage Reduction: %d\n" % int(player.get_stat("dmg_reduction_raw", player.dmgReductionRaw))
  txt += "Bonus Ammo Capacity: %d\n" % int(player.get_stat("bonus_weapon_capacity", player.bonusAmmoCapacity))
	txt += "Sniper Cooldown Reduction: -%.2fs\n" % int(player.get_stat("sniper_cooldown_reduction", player.sniperCooldownReduction))

	
	label.text = txt
	
	panel.modulate.a = 0.0
	var fade_tween = create_tween().set_trans(Tween.TRANS_SINE)
	fade_tween.tween_property(panel, "modulate:a", 1.0, 1.0)

func _on_retry_pressed() -> void:
	get_tree().paused = false
	get_tree().change_scene_to_file("res://background/MainBackground.tscn")


func _on_main_menu_pressed() -> void:
	get_tree().paused = false
	get_tree().change_scene_to_file("res://main_menu/main_menu.tscn")
