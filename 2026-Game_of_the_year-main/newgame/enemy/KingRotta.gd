extends IEnemy
class_name KingRotta

@onready var attackTimer :Timer= $AttackTimer

@onready var projectile := preload("res://enemy/enemy_projectile/EnemyProjectile.tscn")
@onready var enemy := preload("res://enemy/Enemy.tscn")
@onready var ratStats := preload("res://enemy/enemy_types/RatType.tres")
@onready var ratSprite:= $EnemySprite
@onready var crownSprite:= $FloatingCrown

@onready var player :Player= $"../../Player"
@onready var room :RoomModule= $".."

var timeBetweenAttacks := 3

var speed = 0
var dir = Vector2.ZERO
var phase = 1

var ratAnimationLimit:=5
var ratAnimationStep = 0.1
var crownAnimationLimit:=10
var crownAnimationStep = 0.2

var currentRatAnimationStep = 0
var currentCrownAnimationStep = 0

func _ready() -> void:
	super.initialize()
	var ratTween = create_tween()
	ratTween.set_loops()
	ratTween.set_trans(Tween.TRANS_SINE)
	ratTween.set_ease(Tween.EASE_IN_OUT)
	ratTween.tween_property(ratSprite, "position:y", ratSprite.position.y + ratAnimationLimit, 1.0)
	ratTween.tween_property(ratSprite, "position:y", ratSprite.position.y, 1.0)
	var crownTween = create_tween()
	crownTween.set_loops()
	crownTween.set_trans(Tween.TRANS_SINE)
	crownTween.set_ease(Tween.EASE_IN_OUT)
	crownTween.tween_property(crownSprite, "position:y", crownSprite.position.y + crownAnimationLimit, 1.0)
	crownTween.tween_property(crownSprite, "position:y", crownSprite.position.y, 1.0)

func _physics_process(delta: float) -> void:
	if global_position.x < player.global_position.x:
		ratSprite.frame = 1
	else:
		ratSprite.frame = 0
	
	velocity = dir * speed
	move_and_slide()

func _on_attack_timer_timeout() -> void:
	if stats.hp <= 1000 and phase < 2:
		enterSecondPhase()
	var rng = randi_range(1, 10)
	if rng <= 3:
		chargeAttack()
		attackTimer.start(1)
	else:
		shotgunAttack()
		attackTimer.start(timeBetweenAttacks)


func shotgunAttack():
	if phase == 1:
		var mainAngle = global_position.angle_to_point(player.global_position)
		var offset = deg_to_rad(30)
		for angle in [-1, 0, 1]:
			var newProjectile :EnemyProjectile= projectile.instantiate()
			room.add_child.call_deferred(newProjectile)
			newProjectile.initializeProjectile("KING_ROTTA_BULLET",mainAngle + offset * angle, stats.dmg*stats.dmgMult, global_position, 4, 30, false)
	elif phase == 2:
		for hii in range(2):
			var mainAngle = global_position.angle_to_point(player.global_position)
			var offset = deg_to_rad(20)
			for angle in [-2, -1, 0, 1, 2]:
				var newProjectile :EnemyProjectile= projectile.instantiate()
				room.add_child.call_deferred(newProjectile)
				newProjectile.initializeProjectile("KING_ROTTA_BULLET",mainAngle + offset * angle, stats.dmg*stats.dmgMult, global_position, 4, 30, false)
			await get_tree().create_timer(0.1, 0).timeout

func chargeAttack():
	dir = (player.global_position - global_position).normalized()
	speed = 1500
	create_tween().tween_property(self, "speed", 0, 0.7)

func summonAttack():
	for i in range(6):
		var newRat = enemy.instantiate()
		newRat.stats = ratStats
		newRat.spawnPos = position + Vector2(50, 0).rotated(deg_to_rad(60*i))
		room.add_child.call_deferred(newRat)


func enterSecondPhase():
	timeBetweenAttacks = 2
	phase = 2
	summonAttack()
