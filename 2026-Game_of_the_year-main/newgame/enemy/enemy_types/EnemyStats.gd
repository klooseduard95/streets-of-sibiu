extends Resource
class_name EnemyStats

@export var type :String
@export var visionRange := 300.0
@export var hp := 100.0
@export var dmg := 5.0
@export var walkSpeed := 50.0
@export var reach := 50.0
@export var hitCooldown := 0.2
@export var hitWindup := 0.1
@export var sizeModifier := 1.0
var dmgMult := 1
var sizeMult := 1  # randomized later zzz
var hasShield := 0 # also randomized
