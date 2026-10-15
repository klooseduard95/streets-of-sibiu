## V9 golden traces of the real Player scene in the real SpawnRoom, plus V3 (idle frame after stop),
## V10b (runtime error mid-function) and V15 (is_action_just_pressed per physics tick).
## Inputs are applied in the SceneTree.physics_frame signal, which fires before nodes' _physics_process
## in the same tick. State is recorded at the start of each tick = result of the previous tick.
extends "res://probes/probe_base.gd"

const DashType = {"NONE": 0, "SHORT": 1, "MEDIUM": 2, "LONG": 3}
const ParryType = {"NORMAL": 0, "MEDIUM": 1, "CHARGE": 2}

# name: [start_pos, ticks, dash, parry, {tick: {"press": [...], "release": [...]}}, setup]
var scenarios := {
	"walk_right_into_east_wall": [Vector2(1600, 300), 60, "NONE", 3, {0: {"press": ["move_rightward"]}}, ""],
	"diagonal_up_right_slide_to_corner": [Vector2(1000, 300), 120, "NONE", 3, {0: {"press": ["move_forward", "move_rightward"]}}, ""],
	"diagonal_down_left_into_corner": [Vector2(400, 800), 80, "NONE", 3, {0: {"press": ["move_backward", "move_leftward"]}}, ""],
	"walk_right_into_pillar": [Vector2(450, 250), 40, "NONE", 3, {0: {"press": ["move_rightward"]}}, ""],
	"walk_down_into_pillar_top": [Vector2(1350, 650), 40, "NONE", 3, {0: {"press": ["move_backward"]}}, ""],
	"diagonal_down_right_into_pillar_corner": [Vector2(1200, 700), 40, "NONE", 3, {0: {"press": ["move_backward", "move_rightward"]}}, ""],
	"walk_left_then_stop": [Vector2(1000, 550), 50, "NONE", 3, {0: {"press": ["move_leftward"]}, 33: {"release": ["move_leftward"]}}, ""],
	"walk_up_then_stop": [Vector2(1000, 550), 50, "NONE", 3, {0: {"press": ["move_forward"]}, 10: {"release": ["move_forward"]}}, ""],
	"none_dash_mini_slide": [Vector2(500, 550), 40, "NONE", 3, {0: {"press": ["move_rightward"]}, 10: {"press": ["blink"]}, 11: {"release": ["blink"]}, 12: {"release": ["move_rightward"]}}, ""],
	"short_dash_open_floor": [Vector2(500, 550), 60, "SHORT", 3, {0: {"press": ["move_rightward"]}, 5: {"press": ["blink"]}, 7: {"release": ["blink"]}, 8: {"release": ["move_rightward"]}}, ""],
	"short_dash_diagonal_open": [Vector2(500, 750), 60, "SHORT", 3, {0: {"press": ["move_rightward", "move_forward"]}, 5: {"press": ["blink"]}, 7: {"release": ["blink"]}, 8: {"release": ["move_rightward", "move_forward"]}}, ""],
	"short_dash_into_east_wall": [Vector2(1750, 300), 60, "SHORT", 3, {0: {"press": ["move_rightward"]}, 3: {"press": ["blink"]}, 5: {"release": ["blink"]}}, ""],
	"short_dash_into_pillar": [Vector2(400, 250), 60, "SHORT", 3, {0: {"press": ["move_rightward"]}, 3: {"press": ["blink"]}, 5: {"release": ["blink"]}}, ""],
	"short_dash_into_east_wall_with_room_clamp": [Vector2(1750, 300), 60, "SHORT", 3, {0: {"press": ["move_rightward"]}, 3: {"press": ["blink"]}, 5: {"release": ["blink"]}}, "clamp"],
	"short_dash_up_through_top_door_with_room_clamp": [Vector2(950, 250), 60, "SHORT", 3, {0: {"press": ["move_forward"]}, 3: {"press": ["blink"]}, 5: {"release": ["blink"]}}, "clamp"],
	"walk_right_through_east_opening_with_room_clamp": [Vector2(1700, 550), 60, "NONE", 3, {0: {"press": ["move_rightward"]}}, "clamp"],
	"short_dash_standing_still": [Vector2(1000, 550), 30, "SHORT", 3, {5: {"press": ["blink"]}, 6: {"release": ["blink"]}}, ""],
	"medium_dash_open_floor": [Vector2(500, 550), 60, "MEDIUM", 3, {0: {"press": ["move_rightward"]}, 5: {"press": ["blink"]}, 7: {"release": ["blink"]}, 8: {"release": ["move_rightward"]}}, ""],
	"medium_dash_with_consumable_selected": [Vector2(500, 550), 60, "MEDIUM", 3, {0: {"press": ["move_rightward"]}, 5: {"press": ["blink"]}, 7: {"release": ["blink"]}, 8: {"release": ["move_rightward"]}}, "consumable"],
	"normal_parry": [Vector2(1000, 550), 30, "NONE", 0, {2: {"press": ["parry"]}, 3: {"release": ["parry"]}}, ""],
	"medium_parry_with_consumable_selected": [Vector2(1000, 550), 30, "NONE", 1, {2: {"press": ["parry"]}, 3: {"release": ["parry"]}}, "consumable"],
}

var trace: Array = []

func _snapshot(t: int, player) -> Dictionary:
	return {
		"t": t, "pos": vec(player.global_position), "vel": vec(player.velocity),
		"sliding": player.is_sliding, "can_take_dmg": player.can_take_dmg,
		"frame": player.get_node("PlayerSprite").frame,
		"anim": player.get_node("PlayerSprite/AnimationPlayer").current_animation,
		"playing": player.get_node("PlayerSprite/AnimationPlayer").is_playing(),
		"modulate": [player.get_node("PlayerSprite").modulate.r, player.get_node("PlayerSprite").modulate.g,
			player.get_node("PlayerSprite").modulate.b, player.get_node("PlayerSprite").modulate.a],
		"dash_cooldown_left": player.get_node("DashTimer").time_left,
		"on_floor": player.is_on_floor(), "on_wall": player.is_on_wall(), "on_ceiling": player.is_on_ceiling(),
	}

func _run(name: String, spec: Array) -> Dictionary:
	var world := Node2D.new()
	world.name = "World"
	root.add_child(world)
	current_scene = world
	world.add_child(load("res://background/modules/SpawnRoom.tscn").instantiate())
	var player = load("res://player/Player_Placeholder.tscn").instantiate()
	player.selected_dash = DashType[spec[2]]
	player.selected_parry = spec[3]
	world.add_child(player)
	player.global_position = spec[0]
	if spec[5] == "consumable":
		player.inventory.items[3] = load("res://item/consumables/consumable_health.tres").duplicate()
		player.inventory.selected_slot_index = 3
	var clamp_rect := Rect2()
	if spec[5] == "clamp":
		var sizes = world.get_node("SpawnRoom").getRoomSizes()
		var left := int(sizes[0].x - sizes[1] / 2.0); var right := int(sizes[0].x + sizes[1] / 2.0)
		var top := int(sizes[0].y - sizes[2] / 2.0); var bottom := int(sizes[0].y + sizes[2] / 2.0)
		clamp_rect = Rect2(Vector2(left + 40, top + 40), Vector2(right - left - 80, bottom - top - 80))
	await physics_frames(2)  # let the physics space register the room and the player
	var rows := []
	var schedule: Dictionary = spec[4]
	for t in spec[1]:
		await physics_frame
		if spec[5] == "clamp":
			player.position = player.position.clamp(clamp_rect.position, clamp_rect.end)
		rows.append(_snapshot(t, player))
		if schedule.has(t):
			for action in schedule[t].get("release", []):
				Input.action_release(action)
			for action in schedule[t].get("press", []):
				Input.action_press(action)
	for action in InputMap.get_actions():
		Input.action_release(action)
	world.queue_free()
	await frames(2)
	return {"clamp_rect": [vec(clamp_rect.position), vec(clamp_rect.end)], "start": vec(spec[0]), "dash": spec[2], "parry": spec[3], "setup": spec[5],
		"inputs": schedule, "rows": rows}

func _v15() -> Dictionary:
	# Edge detection inside physics ticks for (a) Input.action_press inside the physics tick and
	# (b) a real InputEventAction parsed during an idle frame (buffered like OS events).
	var out := {}
	var seen := []
	for t in 6:
		await physics_frame
		if t == 1:
			Input.action_press("interact")
		seen.append(Input.is_action_just_pressed("interact"))
	Input.action_release("interact")
	out["action_press_in_tick1_just_pressed_per_tick"] = seen
	await physics_frames(3)
	var ev := InputEventAction.new()
	ev.action = "interact"
	ev.pressed = true
	await process_frame
	Input.parse_input_event(ev)
	var seen2 := []
	for t in 6:
		await physics_frame
		seen2.append(Input.is_action_just_pressed("interact"))
	out["parse_input_event_in_idle_then_physics_ticks"] = seen2
	Input.action_release("interact")
	return out

func _initialize() -> void:
	await frames(2)
	var all := {}
	for name in scenarios:
		all[name] = await _run(name, scenarios[name])
	results["scenarios"] = all
	results["input_semantics"] = "Inputs applied in SceneTree.physics_frame at tick t: is_action_pressed is visible to the player in tick t; is_action_just_pressed in tick t+1. Rows record state at the start of tick t (= after tick t-1). With 'clamp', MainBackground's clamp is applied at the start of each tick before the player moves."
	results["V15"] = await _v15()
	save("v9_player_traces")
	quit()
