extends Area2D
class_name EnemyProjectile

var getSprite:Dictionary={
	"BASIC_SLASH": preload("res://enemy/enemy_projectile/enemy_projectile_assets/claw.png"),
	"BASIC_BULLET": preload("res://enemy/enemy_projectile/enemy_projectile_assets/bullet.png"),
	"KING_ROTTA_BULLET": preload("res://enemy/enemy_projectile/enemy_projectile_assets/the_orb.png"),
	"GHOST_BULLET": preload("res://enemy/enemy_projectile/enemy_projectile_assets/the_orb_GHOST.png"),
	"CRIMINAL_SLASH": preload("res://item/item_assets/item icons/knife.png")
}

@onready var projectileSprite :Sprite2D= $ProjectileSprite
@onready var projectileHitbox :CollisionShape2D = $ProjectileHitbox
@onready var deathTimer :Timer = $DeathTimer

var travelTime:float
var speed:float
var angle:float
var damage: float
var piercing:bool
var attackName:String
var isSupport:bool

func initializeProjectile(attackNameI:String, angleI: float, damageI:float, spawnPosI: Vector2,travelTimeI: float = 0.1, speedI:float = 0, piercingI:bool = true, isSupportI:bool = false):
	await ready
	self.attackName = attackNameI
	self.angle = angleI
	self.damage = damageI
	self.travelTime = travelTimeI
	self.speed = speedI
	self.piercing = piercingI
	self.isSupport = isSupportI
	
	rotate(angle)
	projectileSprite.texture = getSprite[attackName]
	projectileHitbox.shape.size = projectileSprite.texture.get_size() * projectileSprite.scale
	deathTimer.start(travelTime)
	
	if attackName == "BASIC_SLASH":
		projectileSprite.rotate(deg_to_rad(45))
	elif attackName == "CRIMINAL_SLASH":
		projectileSprite.rotate(deg_to_rad(90))
	
	global_position = spawnPosI + Vector2(projectileHitbox.shape.size.x/2, 0).rotated(angle)

func _physics_process(delta: float) -> void:
	position = position + Vector2(speed, 0).rotated(angle)

func _on_death_timer_timeout() -> void:
	queue_free()

func _on_body_entered(body: Node2D) -> void:
	if body is Player and !isSupport:
		body.take_dmg(damage, 0.1)
		if !piercing:
			queue_free()
	elif !body is IEnemy and !piercing:
		queue_free()
	elif isSupport and body is BasicEnemy:
		body.stats.dmgMult = damage
		queue_free()
