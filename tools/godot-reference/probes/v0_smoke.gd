extends "res://probes/probe_base.gd"

func _initialize() -> void:
	await frames(2)
	results["autoload_GlobalUtils"] = root.get_node_or_null("GlobalUtils") != null
	results["autoload_AudioPlayer"] = root.get_node_or_null("AudioPlayer") != null
	results["viewport_size"] = vec(root.get_visible_rect().size)
	results["physics_ticks"] = Engine.physics_ticks_per_second
	results["max_physics_steps"] = Engine.max_physics_steps_per_frame
	save("v0_smoke")
	quit()
