extends IEnemy
class_name SupportEnemy

@onready var buffArea:= $BuffArea
@onready var buffHitbox := $BuffArea/BuffAreaHitbox

@onready var projectile := preload("res://enemy/enemy_projectile/EnemyProjectile.tscn")




func _ready():
	super.initialize()
	buffHitbox.shape.radius = stats.reach

func sendBuff(enemy: BasicEnemy):
	var hitAngle = global_position.angle_to_point(enemy.global_position)
	var newProjectile :EnemyProjectile= projectile.instantiate()
	get_parent().add_child.call_deferred(newProjectile)
	newProjectile.initializeProjectile("GHOST_BULLET", hitAngle, 1.3, global_position, 2, 4, true, true)


func _on_tick_timeout() -> void:
	for body in buffArea.get_overlapping_bodies():
		if body is BasicEnemy:
			sendBuff(body)
