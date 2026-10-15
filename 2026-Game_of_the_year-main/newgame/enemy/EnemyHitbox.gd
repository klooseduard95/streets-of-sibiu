extends StaticBody2D
class_name EnemyHitbox

@onready var enemy:= $".."


func takeDamage(dmg):
	enemy.takeDamage(dmg)
