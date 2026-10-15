## V8: NavigationServer2D paths on the real room tile maps with the Enemy agent's settings
## (layers 2, max polygons 512, max search distance 500, corridor funnel), with and without the
## distance limit, plus an agent-in-the-loop chase trace that mimics BasicEnemy.pathFind().
extends "res://probes/probe_base.gd"

const ROOMS := ["SpawnRoom", "FIGHTROOM_12_MEDIUM", "FIGHTROOM_13_BIG", "FIGHTROOM_34_BIG", "FIGHTROOMBIGENEMY_1234"]

func _strip_spawners(room: Node) -> void:
	for child in room.get_children():
		if child.get_script() and child.get_script().resource_path.ends_with("EnemySpawnNode.gd"):
			room.remove_child(child); child.free()

func _walkable_centers(room: Node) -> Array:
	var layer: TileMapLayer = room.get_node("TileMapLayer")
	var out := []
	for cell in layer.get_used_cells():
		var data := layer.get_cell_tile_data(cell)
		if data and data.get_navigation_polygon(0) != null:
			out.append(layer.to_global(layer.map_to_local(cell)))
	out.sort_custom(func(a, b): return a.x < b.x or (a.x == b.x and a.y < b.y))
	return out

func _query(map: RID, from: Vector2, to: Vector2, limited: bool) -> Array:
	var params := NavigationPathQueryParameters2D.new()
	params.map = map
	params.start_position = from
	params.target_position = to
	params.navigation_layers = 2
	params.pathfinding_algorithm = NavigationPathQueryParameters2D.PATHFINDING_ALGORITHM_ASTAR
	params.path_postprocessing = NavigationPathQueryParameters2D.PATH_POSTPROCESSING_CORRIDORFUNNEL
	params.simplify_path = false
	params.path_search_max_polygons = 512
	params.path_search_max_distance = 500.0 if limited else 0.0
	var result := NavigationPathQueryResult2D.new()
	NavigationServer2D.query_path(params, result)
	return Array(result.path).map(func(p): return vec(p))

func _chase_trace(world: Node2D, start: Vector2, target_positions: Array) -> Array:
	# Replica of an Enemy (size 1): floating CharacterBody2D, circle r10, wall_min_slide_angle 0.
	var enemy := CharacterBody2D.new()
	enemy.motion_mode = CharacterBody2D.MOTION_MODE_FLOATING
	enemy.wall_min_slide_angle = 0.0
	var cs := CollisionShape2D.new()
	var circle := CircleShape2D.new()
	cs.shape = circle
	enemy.add_child(cs)
	var agent := NavigationAgent2D.new()
	agent.navigation_layers = 2
	agent.path_search_max_polygons = 512
	agent.path_search_max_distance = 500.0
	agent.radius = 49.3
	enemy.add_child(agent)
	world.add_child(enemy)
	enemy.global_position = start
	await physics_frames(2)
	var rows := []
	for t in 240:
		await physics_frame
		if t % 60 == 0:  # BasicEnemy re-targets on its 1 s Tick
			agent.target_position = target_positions[(t / 60) % target_positions.size()]
		var next := agent.get_next_path_position()
		var dir := enemy.to_local(next).normalized()
		enemy.velocity = dir * 300.0
		enemy.move_and_slide()
		rows.append({"t": t, "pos": vec(enemy.global_position), "next": vec(next), "finished": agent.is_navigation_finished()})
	enemy.queue_free()
	return rows

func _initialize() -> void:
	var world := Node2D.new()
	root.add_child(world)
	await frames(2)
	var rng := RandomNumberGenerator.new()
	rng.seed = 20261015
	var rooms_out := {}
	for room_name in ROOMS:
		var room = load("res://background/modules/%s.tscn" % room_name).instantiate()
		_strip_spawners(room)
		var map := world.get_world_2d().navigation_map
		var iteration_before := NavigationServer2D.map_get_iteration_id(map)
		world.add_child(room)
		# Wait until the navigation map has synchronised the new region (iteration id advances).
		for i in 60:
			await physics_frame
			if NavigationServer2D.map_get_iteration_id(map) > iteration_before + 1:
				break
		await physics_frames(2)
		var centers := _walkable_centers(room)
		var queries := []
		for i in 12:
			var a: Vector2 = centers[rng.randi_range(0, centers.size() - 1)] + Vector2(rng.randf_range(-40, 40), rng.randf_range(-40, 40))
			var b: Vector2 = centers[rng.randi_range(0, centers.size() - 1)] + Vector2(rng.randf_range(-40, 40), rng.randf_range(-40, 40))
			queries.append({"from": vec(a), "to": vec(b), "path_limited_500": _query(map, a, b, true), "path_unlimited": _query(map, a, b, false)})
		var room_data := {"walkable_tiles": centers.size(), "queries": queries,
			"map_iteration_id": NavigationServer2D.map_get_iteration_id(map)}
		if room_name in ["SpawnRoom", "FIGHTROOM_13_BIG"]:
			var start: Vector2 = centers[0] + Vector2(10, 10)
			var targets := [centers[centers.size() - 1], centers[centers.size() / 2], centers[centers.size() / 3]]
			room_data["chase_trace"] = {"start": vec(start), "targets": targets.map(func(p): return vec(p)),
				"rows": await _chase_trace(world, start, targets)}
		rooms_out[room_name] = room_data
		room.queue_free()
		await physics_frames(3)
	results["rooms"] = rooms_out
	save("v8_navigation")
	quit()
