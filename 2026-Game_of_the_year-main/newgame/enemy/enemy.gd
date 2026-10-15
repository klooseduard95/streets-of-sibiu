extends IEnemy
class_name BasicEnemy

@onready var navAgent :NavigationAgent2D= $NavigationAgent2D
@onready var lineOfSight := $LineOfSight
@onready var agroRange := $AgroRange
@onready var agroShape := $AgroRange/AgroShape
@onready var hitRange :Area2D= $HitRange
@onready var hitShape := $HitRange/HitShape
@onready var hitbox :CollisionShape2D= $CollisionShape2D


@onready var projectile :PackedScene= preload("res://enemy/enemy_projectile/EnemyProjectile.tscn")

var agro: int
var isIdle:bool

func _ready() -> void:
	super.initialize()
	
	stats.visionRange *= 10
	agro = 0
	isIdle = 0
	agroShape.shape.radius = stats.visionRange
	
	# doesnt work otherwise
	var newCircle :CircleShape2D = CircleShape2D.new()
	newCircle.radius = stats.reach
	hitShape.shape = newCircle
	

func _physics_process(delta: float) -> void:
	super(delta)
	if !isIdle:
		pathFind(delta)

func pathFind(delta):
	var dir = to_local(navAgent.get_next_path_position()).normalized()
	velocity = dir * stats.walkSpeed * Player.slownEnemy
	move_and_slide()


func hasLineOfSight(player: Node2D) -> bool:
	if player == null: return false
	if global_position.distance_to(player.global_position) > stats.visionRange:
		return false
	
	lineOfSight.target_position = to_local(player.global_position)
	lineOfSight.force_raycast_update()
	
	return lineOfSight.get_collider() == player
	


func agroCheck():
	var target = null
	for body in agroRange.get_overlapping_bodies():
		if is_instance_of(body, Player):
			target = body
			break
	var hasSight =  hasLineOfSight(target)
	if hasSight:
		agro += (11-agro)/3
		isIdle = 0
	elif agro > 0:
		agro-=1
	else: isIdle = 1
	
	if agro > 0:
		makePath(target)


func makePath(target: Node2D):
	if target != null:
		navAgent.target_position = target.global_position

func _on_tick_timeout() -> void:
	agroCheck()


func attack(player):
	if !isDead and player is Player:
		hitShape.set_deferred("disabled", true)
		var auxSpeed = stats.walkSpeed
		stats.walkSpeed = 0
		await get_tree().create_timer(stats.hitWindup, 0).timeout
		var hitAngle = global_position.angle_to_point(player.global_position)
		var newProjectile :EnemyProjectile= projectile.instantiate()
		get_parent().add_child.call_deferred(newProjectile)
		
		match stats.type:
			"rat":
				$RatSound.play()
				newProjectile.initializeProjectile("BASIC_SLASH", hitAngle, stats.dmg*stats.dmgMult, global_position)
			"pigeon":
				$PigeonSound.play()
				newProjectile.initializeProjectile("BASIC_SLASH", hitAngle, stats.dmg*stats.dmgMult, global_position)
			"thief":
				var random_number = randi() % 10
				if -1 < random_number and random_number < 3:
					$CeTelAiSound.play()
				else:
					$GruntSound.play()
				newProjectile.initializeProjectile("BASIC_BULLET", hitAngle, stats.dmg*stats.dmgMult, global_position, 6, 7, false)
			"criminal":
				var random_number = randi() % 10
				if -1 < random_number and random_number < 3:
					$CeTelAiSound.play()
				else:
					$GruntSound.play()
				newProjectile.initializeProjectile("CRIMINAL_SLASH", hitAngle, stats.dmg*stats.dmgMult, global_position, 0.5, 5, false)
		
		if get_tree():
			await get_tree().create_timer(stats.hitCooldown, 0).timeout
		hitShape.set_deferred("disabled", false)
		stats.walkSpeed = auxSpeed
	

func _on_hit_range_body_entered(body: Node2D) -> void:
	if is_instance_of(body, Player):
		attack(body)
