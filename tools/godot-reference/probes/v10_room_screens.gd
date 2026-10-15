## Reference renders of every room module (windowed run; real renderer needed):
## camera snapped to the module-box centre and to the room origin (clamped by the limits),
## exactly like MainBackground.updateCameraAnchor. Writes results/room_screens/<ROOM>_<view>.png.
extends "res://probes/probe_base.gd"

const ROOMS := [
	"SpawnRoom", "FIGHTROOMBIGENEMY_1", "FIGHTROOMBIGENEMY_2", "FIGHTROOMBIGENEMY_3", "FIGHTROOMBIGENEMY_4",
	"FIGHTROOMBIGENEMY_1234", "FIGHTROOM_12", "FIGHTROOM_12_BIG", "FIGHTROOM_12_MEDIUM", "FIGHTROOM_13_BIG",
	"FIGHTROOM_13_MEDIUM", "FIGHTROOM_13_SMALL", "FIGHTROOM_24_MEDIUM", "FIGHTROOM_34_BIG", "FIGHTROOM_34_SMALL",
	"FIGHTROOM_1234_SMALL", "FIGHTROOM_234_SMALL", "FIGHTROOM_134_SMALL", "FIGHTROOM_124_SMALL", "FIGHTROOM_123_SMALL",
]

func _initialize() -> void:
	var dir := out_dir().path_join("room_screens")
	DirAccess.make_dir_recursive_absolute(dir)
	var vp := SubViewport.new()
	vp.size = Vector2i(1920, 1080)
	vp.render_target_update_mode = SubViewport.UPDATE_ALWAYS
	# The game renders in the root viewport, which takes the project's Nearest default filter;
	# a SubViewport defaults to Linear, so match the root viewport explicitly.
	vp.canvas_item_default_texture_filter = Viewport.DEFAULT_CANVAS_ITEM_TEXTURE_FILTER_NEAREST
	root.add_child(vp)
	var written := []
	for room_name in ROOMS:
		var room = load("res://background/modules/%s.tscn" % room_name).instantiate()
		for child in room.get_children():
			if child.get_script() and child.get_script().resource_path.ends_with("EnemySpawnNode.gd"):
				room.remove_child(child); child.free()
		vp.add_child(room)
		var target := Node2D.new()
		vp.add_child(target)
		var cam := Camera2D.new()
		target.add_child(cam)
		await frames(1)
		var sizes = room.getRoomSizes()
		cam.limit_top = sizes[0].y - sizes[2] / 2
		cam.limit_bottom = sizes[0].y + sizes[2] / 2
		cam.limit_right = sizes[0].x + sizes[1] / 2
		cam.limit_left = sizes[0].x - sizes[1] / 2
		cam.make_current()
		for view in ["centre", "corner"]:
			target.position = room.getRoomCenter() if view == "centre" else Vector2.ZERO
			cam.reset_smoothing()
			await frames(4)
			var path := dir.path_join("%s_%s.png" % [room_name, view])
			vp.get_texture().get_image().save_png(path)
			written.append(path.get_file())
		target.queue_free()
		room.queue_free()
		await frames(2)
	results["written"] = written
	results["renderer"] = RenderingServer.get_current_rendering_method() + " / " + RenderingServer.get_video_adapter_name()
	save("v10_room_screens")
	quit()
