extends SceneTree

var results: Dictionary = {}

func out_dir() -> String:
	for arg in OS.get_cmdline_user_args():
		if arg.begins_with("--out="):
			return arg.substr(6)
	return "res://probes_out"

func save(probe_name: String) -> void:
	var dir := out_dir()
	DirAccess.make_dir_recursive_absolute(dir)
	results["godot_version"] = Engine.get_version_info()["string"]
	var file := FileAccess.open(dir.path_join(probe_name + ".json"), FileAccess.WRITE)
	file.store_string(JSON.stringify(results, "  ", true))
	file.close()
	print("PROBE %s saved to %s" % [probe_name, dir])

func frames(count: int) -> void:
	for i in count:
		await process_frame

func physics_frames(count: int) -> void:
	for i in count:
		await physics_frame

static func vec(v: Vector2) -> Array:
	return [v.x, v.y]
