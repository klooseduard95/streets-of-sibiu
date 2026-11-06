extends "res://probes/probe_base.gd"

var vp: SubViewport

func _camera(scale_value: Vector2, smoothing: bool) -> Camera2D:
	var target := Node2D.new()
	vp.add_child(target)
	var cam := Camera2D.new()
	cam.scale = scale_value
	cam.zoom = Vector2(1, 1)
	cam.process_callback = Camera2D.CAMERA2D_PROCESS_PHYSICS
	cam.position_smoothing_enabled = smoothing
	cam.limit_smoothed = true
	target.add_child(cam)
	cam.make_current()
	return cam

func _room_limits(room) -> Array:
	var sizes = room.getRoomSizes()
	var cam := Camera2D.new()
	cam.limit_top = sizes[0].y - sizes[2] / 2
	cam.limit_bottom = sizes[0].y + sizes[2] / 2
	cam.limit_right = sizes[0].x + sizes[1] / 2
	cam.limit_left = sizes[0].x - sizes[1] / 2
	var raw := [sizes[0].x - sizes[1] / 2, sizes[0].y - sizes[2] / 2, sizes[0].x + sizes[1] / 2, sizes[0].y + sizes[2] / 2]
	return [[cam.limit_left, cam.limit_top, cam.limit_right, cam.limit_bottom], raw]

func _initialize() -> void:
	vp = SubViewport.new()
	vp.size = Vector2i(1920, 1080)
	root.add_child(vp)
	await frames(2)

	var cam := _camera(Vector2(3.9215689, 3.9215689), false)
	cam.get_parent().position = Vector2(1000, 600)
	await physics_frames(3)
	results["scaled_camera_screen_center"] = vec(cam.get_screen_center_position())
	results["scaled_camera_canvas_transform"] = [vec(vp.canvas_transform.x), vec(vp.canvas_transform.y), vec(vp.canvas_transform.origin)]
	cam.get_parent().queue_free()
	await physics_frames(2)

	var limit_cases := {}
	var cam2 := _camera(Vector2.ONE, false)
	cam2.limit_left = 19; cam2.limit_top = 24; cam2.limit_right = 1973; cam2.limit_bottom = 1065
	for target_pos in [Vector2(996, 545), Vector2(100, 100), Vector2(1900, 1000), Vector2(996, 0), Vector2(996, 2000)]:
		cam2.get_parent().position = target_pos
		await physics_frames(3)
		limit_cases[str(target_pos)] = vec(cam2.get_screen_center_position())
	cam2.limit_left = 0; cam2.limit_top = 0; cam2.limit_right = 1500; cam2.limit_bottom = 900
	for target_pos in [Vector2(750, 450), Vector2(0, 0)]:
		cam2.get_parent().position = target_pos
		await physics_frames(3)
		limit_cases["both_axes_small " + str(target_pos)] = vec(cam2.get_screen_center_position())
	results["limit_cases_screen_center"] = limit_cases
	cam2.get_parent().queue_free()
	await physics_frames(2)

	var cam3 := _camera(Vector2.ONE, true)
	cam3.limit_left = -10000; cam3.limit_top = -10000; cam3.limit_right = 10000; cam3.limit_bottom = 10000
	cam3.get_parent().position = Vector2(0, 0)
	await physics_frames(3)
	cam3.reset_smoothing()
	await physics_frames(2)
	var track := []
	cam3.get_parent().position = Vector2(600, 300)
	for i in 40:
		await physics_frame
		track.append(vec(cam3.get_screen_center_position()))
	results["smoothing_track_after_jump"] = track
	cam3.limit_right = 1500
	var track2 := []
	for i in 30:
		await physics_frame
		track2.append(vec(cam3.get_screen_center_position()))
	results["smoothing_track_after_limit_change"] = track2
	cam3.reset_smoothing()
	await physics_frame
	results["after_reset_smoothing"] = vec(cam3.get_screen_center_position())
	cam3.get_parent().queue_free()

	var per_room := {}
	for room_name in ["SpawnRoom", "FIGHTROOM_12", "FIGHTROOM_124_SMALL", "FIGHTROOM_12_MEDIUM", "FIGHTROOM_12_BIG",
			"FIGHTROOM_13_BIG", "FIGHTROOM_34_BIG", "FIGHTROOMBIGENEMY_1"]:
		var room = load("res://background/modules/%s.tscn" % room_name).instantiate()
		for child in room.get_children():
			if child.get_script() and child.get_script().resource_path.ends_with("EnemySpawnNode.gd"):
				room.remove_child(child); child.free()
		vp.add_child(room)
		await physics_frame
		var limits := _room_limits(room)
		per_room[room_name] = {"int_limits_LTRB": limits[0], "raw_LTRB": limits[1]}
		room.queue_free()
	results["V13_room_camera_limits"] = per_room
	save("v4_camera")
	quit()
