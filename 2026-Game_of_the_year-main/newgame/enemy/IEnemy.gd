extends CharacterBody2D
class_name IEnemy


@onready var healthBar :ProgressBar= $HealthBar
@onready var sprite :Sprite2D = $EnemySprite

@onready var parentRoom = $".."
@onready var mainBackGround = $"../.."

static var assets :Dictionary={
	"rat": load("res://enemy/enemy_assets/Rat Spritesheet.png"),
	"pigeon": load("res://enemy/enemy_assets/PigeonSpritesheet.png"),
	"thief": load("res://enemy/enemy_assets/thiefAssets.png"),
	"criminal": load("res://enemy/enemy_assets/criminalAssets.png"),
	"marshmallow": load("res://enemy/enemy_assets/manhole.png"),
	"ghost": load("res://enemy/enemy_assets/ghost.png")
}

# WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING 
# Bine ai venit, domnule inginer!
# vad ca ai incercat sa instantiezi un inamic intr-o scena care nu este o camera
# din pacate pentru tine acest lucru nu mai este posibil
# bleh :pp
#
# EDIT: m-am decis sa nu fiu malefic azi si acest warning este acum irelevant
# WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING 
# :3:3:3:3:3:3:3:3:3:3:3

@export var stats:EnemyStats

var timeToFrame := 15
var currentTimeToFrame :=0

var isDead:bool=false

var row
var col
var spawnPos

func initialize() -> void:
	position += Vector2(randi_range(1, 5), 0).rotated(deg_to_rad(randi_range(1,360)))
	if spawnPos:
		position = spawnPos
	if parentRoom is RoomModule:
		parentRoom.enemyCount += 1
	stats = stats.duplicate(1)
	updateStats()
	rollForSize()
	updateLooks()
	
	healthBar.max_value = stats.hp
	healthBar.value = stats.hp
	scale = Vector2(stats.sizeModifier, stats.sizeModifier)

func _physics_process(delta: float) -> void:

	match stats.type:
		"rat":
			if velocity != Vector2.ZERO and currentTimeToFrame > timeToFrame:
				if velocity.x > 0: col = 1
				else: col = 0
				if row == 1: row = 0
				else: row = 1
				sprite.frame = row * sprite.hframes + col
				currentTimeToFrame = 0
		"pigeon":
			if currentTimeToFrame > timeToFrame:
				if velocity.x > 0: col = 1
				else: col = 0
				if row == 1: row = 0
				else: row = 1
				sprite.frame = row * sprite.hframes + col
				currentTimeToFrame = 0
		"thief":
			if velocity != Vector2.ZERO and currentTimeToFrame > timeToFrame:
				if velocity.x >= 0:
					if velocity.y >= 0:
						if velocity.x > velocity.y: row = 3
						else: row = 1
					elif velocity.y <= 0:
						if velocity.x > abs(velocity.y): row = 3
						else: row = 0
				elif velocity.x <= 0:
					if velocity.y >= 0:
						if abs(velocity.x) > velocity.y: row = 2
						else: row = 1
					elif velocity.y <= 0:
						if abs(velocity.x) > abs(velocity.y): row = 2
						else: row = 0
				
				if col == 0: col = 1
				else: col = 0
				
				sprite.frame = row * sprite.hframes + col
				currentTimeToFrame = 0
			elif velocity == Vector2.ZERO:
				if !row: row = 1
				col = 2
				currentTimeToFrame = 0
				sprite.frame = row * sprite.hframes + col
		"criminal":
			if velocity != Vector2.ZERO and currentTimeToFrame > timeToFrame:
				if velocity.x >= 0:
					if velocity.y >= 0:
						if velocity.x > velocity.y: row = 3
						else: row = 1
					elif velocity.y <= 0:
						if velocity.x > abs(velocity.y): row = 3
						else: row = 0
				elif velocity.x <= 0:
					if velocity.y >= 0:
						if abs(velocity.x) > velocity.y: row = 2
						else: row = 1
					elif velocity.y <= 0:
						if abs(velocity.x) > abs(velocity.y): row = 2
						else: row = 0
				
				if col == 0: col = 1
				else: col = 0
				
				sprite.frame = row * sprite.hframes + col
				currentTimeToFrame = 0
			elif velocity == Vector2.ZERO:
				if !row: row = 1
				col = 2
				currentTimeToFrame = 0
				sprite.frame = row * sprite.hframes + col
			
	
	currentTimeToFrame +=1
		

func die():
	isDead = true
	if parentRoom is RoomModule:
		parentRoom.enemyCount -= 1
	stats.dmg = 0
	stats.walkSpeed = 0
	await create_tween().tween_property(self, "modulate:a", 0, 0.3).finished
	queue_free()


func takeDamage(damage: float):
	if stats.hp > 0:
		stats.hp -= damage
		if stats.hp <= 0:
			stats.hp = 0
			die()
		updateHealthBar()

func updateHealthBar():
	if healthBar.value == healthBar.max_value:
		healthBar.modulate.a = 1
	var healthTweener := create_tween()
	healthTweener.tween_property(healthBar, "value", float(stats.hp), 0.4)


func rollForSize():
	if stats.sizeModifier == 1:
		var rng = randi_range(1, 100)
		if rng <=10:
			return
		elif rng <=85:
			stats.sizeModifier = 1.25
			stats.hp = stats.hp + 20.0/100 * stats.hp
			stats.dmg = stats.dmg + 10.0/100 * stats.dmg
		else:
			stats.sizeModifier = 1.5
			stats.hp = stats.hp + 40.0/100 * stats.hp
			stats.dmg = stats.dmg + 20.0/100 * stats.dmg
	
	if stats.sizeModifier == 1.25:
		var rng = randi_range(1, 100)
		if rng <=85:
			return
		else:
			stats.sizeModifier = 1.5
			stats.hp = stats.hp + 30.0/100 * stats.hp
			stats.dmg = stats.dmg + 15.0/100 * stats.dmg

func updateLooks():
	match stats.type:
		"rat":
			sprite.texture = assets["rat"]
			sprite.hframes = 2
			sprite.vframes = 2
		"pigeon":
			sprite.texture = assets["pigeon"]
			sprite.hframes = 2
			sprite.vframes = 2
		"thief":
			sprite.texture = assets["thief"]
			sprite.hframes = 3
			sprite.vframes = 4
			sprite.frame = 5
		"criminal":
			sprite.texture = assets["criminal"]
			sprite.hframes = 3
			sprite.vframes = 4
			sprite.frame = 5
		"marshmallow":
			sprite.texture = assets["marshmallow"]
		"ghost":
			sprite.texture = assets["ghost"]


func updateStats():
	# lowk freestyling the rampup cus i dont fw the design doc
	# feel free to change the numbers
	var statIncreaseMult :int= mainBackGround.currentLevel -1
	match stats.type:
		"rat":
			stats.hp += 3*statIncreaseMult
			stats.dmg += 1 * statIncreaseMult
		"pigeon":
			stats.hp += 2*statIncreaseMult
			stats.dmg += 1 * statIncreaseMult
		"thief":
			stats.hp += 3*statIncreaseMult
			stats.dmg += 1.5 * statIncreaseMult
		"criminal":
			stats.hp += 3*statIncreaseMult
			stats.dmg += 1.5 * statIncreaseMult
			
			
	
