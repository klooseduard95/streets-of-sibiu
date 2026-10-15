## V6: does re-enabling a disabled CollisionShape2D re-emit body_entered for an overlapping body?
## V7: order of body_entered when a player bullet appears overlapping an enemy body AND its hitbox.
extends "res://probes/probe_base.gd"

var log_rows := []
var tick := 0

func _circle(r: float) -> CollisionShape2D:
	var cs := CollisionShape2D.new()
	var c := CircleShape2D.new()
	c.radius = r
	cs.shape = c
	return cs

func _v6() -> Dictionary:
	var world := Node2D.new()
	root.add_child(world)
	var body := CharacterBody2D.new()  # stands in for the Player (layer 1)
	body.add_child(_circle(30))
	world.add_child(body)
	var area := Area2D.new()          # stands in for the enemy HitRange (mask 1)
	var shape := _circle(50)
	area.add_child(shape)
	area.position = Vector2(20, 0)
	world.add_child(area)
	var events := []
	area.body_entered.connect(func(b): events.append(["entered", tick]))
	area.body_exited.connect(func(b): events.append(["exited", tick]))
	for t in 30:
		await physics_frame
		tick = t
		if t == 5:
			shape.set_deferred("disabled", true)   # as BasicEnemy.attack()
		if t == 15:
			shape.set_deferred("disabled", false)  # after the cooldown
	world.queue_free()
	await physics_frames(2)
	return {"events_[name,tick]": events, "note": "disable at tick 5 (deferred), re-enable at tick 15 (deferred)"}

func _v7_trial(enemy_first: bool, hitbox_child_first: bool, bullet_offset: Vector2) -> Array:
	var world := Node2D.new()
	root.add_child(world)
	var enemy := CharacterBody2D.new()      # Enemy body: layer 1, circle r10
	enemy.name = "EnemyBody"
	var body_shape := _circle(10)
	var hitbox := StaticBody2D.new()         # EnemyHitbox: layer/mask 4, circle r27
	hitbox.name = "EnemyHitbox"
	hitbox.collision_layer = 4
	hitbox.collision_mask = 4
	hitbox.add_child(_circle(27))
	if hitbox_child_first:
		enemy.add_child(hitbox); enemy.add_child(body_shape)
	else:
		enemy.add_child(body_shape); enemy.add_child(hitbox)
	var bullet := Area2D.new()               # player bullet: layer/mask 5, capsule r11 h32 rotated 90°
	bullet.collision_layer = 5
	bullet.collision_mask = 5
	var cs := CollisionShape2D.new()
	var cap := CapsuleShape2D.new()
	cap.radius = 11; cap.height = 32
	cs.shape = cap
	cs.rotation = PI / 2
	bullet.add_child(cs)
	bullet.position = bullet_offset
	var order := []
	bullet.body_entered.connect(func(b): order.append(b.name))
	if enemy_first:
		world.add_child(enemy); await physics_frames(2); world.add_child(bullet)
	else:
		world.add_child(bullet); await physics_frames(2); world.add_child(enemy)
	await physics_frames(4)
	world.queue_free()
	await physics_frames(2)
	return order

func _initialize() -> void:
	await frames(2)
	results["V6"] = await _v6()
	var trials := []
	for enemy_first in [true, false]:
		for hitbox_first in [true, false]:
			for offset in [Vector2.ZERO, Vector2(5, 3), Vector2(-8, 0)]:
				trials.append({"enemy_added_first": enemy_first, "hitbox_child_first": hitbox_first,
					"offset": vec(offset), "order": await _v7_trial(enemy_first, hitbox_first, offset)})
	results["V7_trials"] = trials
	save("v6_areas")
	quit()
