## V1: do pause_menu.gd / death_screen.gd load, and what does the running game look like?
extends "res://probes/probe_base.gd"

func _describe_screen(node: Node) -> Dictionary:
	return {
		"script_attached": node.get_script() != null,
		"visible": node.visible,
		"process_mode": node.process_mode,
		"has_setup_death_screen": node.has_method("setup_death_screen"),
		"has_pause_game": node.has_method("pause_game"),
	}

func _initialize() -> void:
	await frames(1)
	results["pause_menu_gd_loads"] = ResourceLoader.load("res://pause_menu.gd") != null
	results["death_screen_gd_loads"] = ResourceLoader.load("res://death_screen.gd") != null
	results["pause_menu_tscn_instance"] = _describe_screen(load("res://pause_menu.tscn").instantiate())
	results["death_screen_tscn_instance"] = _describe_screen(load("res://death_screen.tscn").instantiate())

	# Run the real game scene and look at the in-game pause menu after level 1 is generated.
	change_scene_to_file("res://background/MainBackground.tscn")
	await frames(240)
	var bg = current_scene
	var pause_menu = bg.get_node("PauseMenu")
	results["in_game_pause_menu"] = _describe_screen(pause_menu)
	results["tree_paused_after_start"] = paused
	results["current_level"] = bg.currentLevel
	results["is_in_transition"] = bg.isInTransition
	results["level_rooms"] = bg.currentLevelRooms.size()
	results["room_types"] = bg.currentLevelRooms.map(func(r): return [r.scene_file_path.get_file(), r.roomType, r.isExitRoom])

	# Simulate the player's death screen call path.
	var ds = load("res://death_screen.tscn").instantiate()
	bg.add_child(ds)
	results["death_screen_call_ok"] = ds.has_method("setup_death_screen")
	save("v1_runtime")
	quit()
