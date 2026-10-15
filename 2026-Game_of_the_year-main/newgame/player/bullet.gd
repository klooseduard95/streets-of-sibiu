extends Area2D

@export var speed: float
@export var isMelee: bool
var dmg: float
var maxRange: float
var traveled_distange: float = 0.0

var direction: Vector2 = Vector2.RIGHT

var weapon_texture: Texture2D = null

func _ready() -> void:
	body_entered.connect(_on_body_entered)
	if weapon_texture != null:
		$Sprite2D.texture = weapon_texture

func _physics_process(delta: float) -> void:
	var moveStep = speed * delta
	
	position += direction * moveStep
	
	traveled_distange += moveStep
	if traveled_distange > maxRange:
		queue_free()

func _on_body_entered(body):
	if body is EnemyHitbox:
		body.takeDamage(dmg)
	elif isMelee and body is IEnemy:
		return
	elif body is Player:
		return
	else:
		queue_free() # wall
	print("enemy took %.2f dmg" % dmg)
	
	if isMelee == false:
		queue_free()
