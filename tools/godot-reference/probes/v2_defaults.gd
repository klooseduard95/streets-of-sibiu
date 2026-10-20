extends "res://probes/probe_base.gd"

const ROOMS := [
	"FIGHTROOMBIGENEMY_1", "FIGHTROOMBIGENEMY_2", "FIGHTROOMBIGENEMY_3", "FIGHTROOMBIGENEMY_4",
	"FIGHTROOMBIGENEMY_1234", "FIGHTROOM_12", "FIGHTROOM_12_BIG", "FIGHTROOM_12_MEDIUM",
	"FIGHTROOM_13_BIG", "FIGHTROOM_13_MEDIUM", "FIGHTROOM_13_SMALL", "FIGHTROOM_24_MEDIUM",
	"FIGHTROOM_34_BIG", "FIGHTROOM_34_SMALL", "FIGHTROOM_1234_SMALL", "FIGHTROOM_234_SMALL",
	"FIGHTROOM_134_SMALL", "FIGHTROOM_124_SMALL", "FIGHTROOM_123_SMALL", "SpawnRoom",
]

func _initialize() -> void:
	await frames(1)
	var spawn_node = load("res://enemy/EnemySpawnNode.tscn").instantiate()
	results["V2_base_scene_spawnRadius"] = [spawn_node.spawnRadius, type_string(typeof(spawn_node.spawnRadius))]
	var per_room := {}
	for room_name in ROOMS:
		var room = load("res://background/modules/%s.tscn" % room_name).instantiate()
		var radii := []
		for child in room.get_children():
			if child.get_script() and child.get_script().resource_path.ends_with("EnemySpawnNode.gd"):
				radii.append(child.spawnRadius)
		per_room[room_name] = radii
		room.free()
	results["V2_spawnRadius_per_room"] = per_room

	var stats = load("res://enemy/enemy_types/RatType.tres").duplicate(true)
	stats.dmgMult = 1.3
	results["V10_dmgMult_after_assign_1_3"] = [stats.dmgMult, type_string(typeof(stats.dmgMult))]

	var p1 = load("res://enemy/enemy_projectile/EnemyProjectile.tscn").instantiate()
	var p2 = load("res://enemy/enemy_projectile/EnemyProjectile.tscn").instantiate()
	results["V12_projectile_rect_shared"] = is_same(p1.get_node("ProjectileHitbox").shape, p2.get_node("ProjectileHitbox").shape)
	var e1 = load("res://enemy/Enemy.tscn").instantiate()
	var e2 = load("res://enemy/Enemy.tscn").instantiate()
	results["V12_enemy_agro_circle_shared"] = is_same(e1.get_node("AgroRange/AgroShape").shape, e2.get_node("AgroRange/AgroShape").shape)
	results["V12_enemy_hit_circle_shared"] = is_same(e1.get_node("HitRange/HitShape").shape, e2.get_node("HitRange/HitShape").shape)

	var agent: NavigationAgent2D = e1.get_node("NavigationAgent2D")
	results["agent_enemy"] = {
		"path_desired_distance": agent.path_desired_distance, "target_desired_distance": agent.target_desired_distance,
		"path_max_distance": agent.path_max_distance, "path_postprocessing": agent.path_postprocessing,
		"pathfinding_algorithm": agent.pathfinding_algorithm, "simplify_path": agent.simplify_path,
		"avoidance_enabled": agent.avoidance_enabled, "navigation_layers": agent.navigation_layers,
		"path_search_max_polygons": agent.path_search_max_polygons, "path_search_max_distance": agent.path_search_max_distance,
		"target_position_default": vec(agent.target_position),
	}
	var cam := Camera2D.new()
	results["camera_defaults"] = {
		"position_smoothing_speed": cam.position_smoothing_speed, "process_callback": cam.process_callback,
		"anchor_mode": cam.anchor_mode, "drag_horizontal_enabled": cam.drag_horizontal_enabled,
		"limit_left": cam.limit_left, "limit_top": cam.limit_top, "ignore_rotation": cam.ignore_rotation,
	}
	var player = load("res://player/Player_Placeholder.tscn").instantiate()
	for pair in [["player", player], ["enemy", e1], ["king", load("res://enemy/KingRottaBoss.tscn").instantiate()]]:
		var b: CharacterBody2D = pair[1]
		results["body_" + pair[0]] = {
			"motion_mode": b.motion_mode, "safe_margin": b.safe_margin, "max_slides": b.max_slides,
			"floor_snap_length": b.floor_snap_length, "floor_max_angle_deg": rad_to_deg(b.floor_max_angle),
			"floor_stop_on_slope": b.floor_stop_on_slope, "floor_block_on_wall": b.floor_block_on_wall,
			"floor_constant_speed": b.floor_constant_speed, "wall_min_slide_angle_deg": rad_to_deg(b.wall_min_slide_angle),
			"up_direction": vec(b.up_direction), "collision_layer": b.collision_layer, "collision_mask": b.collision_mask,
			"platform_on_leave": b.platform_on_leave, "platform_floor_layers": b.platform_floor_layers,
		}
	var sfx := AudioStreamPlayer2D.new()
	results["audio2d_defaults"] = {"max_distance": sfx.max_distance, "attenuation": sfx.attenuation, "panning_strength": sfx.panning_strength}
	var settings := {}
	for key in ["audio/general/2d_panning_strength", "navigation/2d/default_edge_connection_margin",
			"navigation/2d/default_cell_size", "navigation/2d/default_link_connection_radius",
			"navigation/2d/merge_rasterizer_cell_scale", "physics/2d/solver/solver_iterations",
			"physics/common/physics_jitter_fix", "physics/common/physics_interpolation",
			"input_devices/buffering/agile_event_flushing", "gui/theme/custom_font", "gui/theme/default_font_antialiasing",
			"rendering/2d/snap/snap_2d_transforms_to_pixel", "display/window/stretch/mode"]:
		settings[key] = ProjectSettings.get_setting(key)
	results["project_settings"] = settings

	var fonts := {}
	for spec in [["Montserrat", 400], ["JetBrains Mono NL", 400], ["JetBrains Mono", 400], ["Hack", 750]]:
		var f := SystemFont.new()
		f.font_names = PackedStringArray([spec[0]])
		f.font_weight = spec[1]
		fonts[spec[0]] = {"resolved_name": f.get_font_name(), "resolved_style": f.get_font_style_name()}
	results["V11_system_fonts_this_machine"] = fonts
	results["V11_default_theme_font"] = ThemeDB.fallback_font.get_font_name() + " / " + ThemeDB.fallback_font.get_font_style_name()
	results["V11_default_font_size"] = ThemeDB.fallback_font_size

	var trans := {"LINEAR": Tween.TRANS_LINEAR, "SINE": Tween.TRANS_SINE}
	var eases := {"IN": Tween.EASE_IN, "OUT": Tween.EASE_OUT, "IN_OUT": Tween.EASE_IN_OUT, "OUT_IN": Tween.EASE_OUT_IN}
	var samples := {}
	for t in trans:
		for e in eases:
			var row := []
			for i in 11:
				row.append(Tween.interpolate_value(0.0, 1.0, i / 10.0, 1.0, trans[t], eases[e]))
			samples[t + "_" + e] = row
	results["V14_easing_samples_t0_to_1_step_0_1"] = samples
	save("v2_defaults")
	quit()
