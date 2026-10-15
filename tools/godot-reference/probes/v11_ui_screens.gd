## Reference renders of the D1 screens (windowed run): main menu, main menu + tutorial, Settings,
## and the game start (MainBackground after the level-1 fade). Each scene is instanced in a
## 1920x1080 SubViewport (Nearest filter, like the root viewport) and saved to results/ui_screens/.
extends "res://probes/probe_base.gd"

func _shot(vp: SubViewport, name: String, dir: String) -> void:
	await frames(3)
	vp.get_texture().get_image().save_png(dir.path_join(name + ".png"))

func _initialize() -> void:
	var dir := out_dir().path_join("ui_screens")
	DirAccess.make_dir_recursive_absolute(dir)
	var vp := SubViewport.new()
	vp.size = Vector2i(1920, 1080)
	vp.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	vp.canvas_item_default_texture_filter = Viewport.DEFAULT_CANVAS_ITEM_TEXTURE_FILTER_NEAREST
	root.add_child(vp)

	var menu = load("res://main_menu/main_menu.tscn").instantiate()
	vp.add_child(menu)
	await _shot(vp, "menu", dir)
	menu.get_node("TutorialScreen").show()
	await _shot(vp, "tutorial", dir)
	menu.queue_free()
	await frames(2)

	var settings = load("res://scenes/Settings.tscn").instantiate()
	vp.add_child(settings)
	await _shot(vp, "options", dir)
	settings.queue_free()
	await frames(2)

	var bg = load("res://background/MainBackground.tscn").instantiate()
	vp.add_child(bg)
	# Wait for level generation and the black-screen fade-out (generation takes a few frames).
	for i in 240:
		await process_frame
	await _shot(vp, "game_start", dir)
	results["player_position"] = vec(bg.get_node("Player").global_position)
	results["camera_center"] = vec(bg.get_node("Player/CoolCamera").get_screen_center_position())
	results["black_alpha"] = bg.get_node("Overlay/BlackScreen").modulate.a
	results["level_rooms"] = bg.currentLevelRooms.size()
	save("v11_ui_screens")
	quit()
