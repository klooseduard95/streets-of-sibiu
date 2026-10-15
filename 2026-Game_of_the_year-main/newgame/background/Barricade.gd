extends Node2D
class_name Barricade

var nodeOrientation: String = "north"
var spawnPos
@onready var tileMap := $BarricadeTileMap

func _ready() -> void:
	global_position = spawnPos
	match nodeOrientation:
		"north": fillTiles(Vector2(-2, 0),Vector2(1, 0)) 
		"south": fillTiles(Vector2(-2, -1),Vector2(1, -1))
		"east": fillTiles(Vector2(-1, -2),Vector2(-1, 1))
		"west": fillTiles(Vector2(0, -2),Vector2(0, 1))



func fillTiles(start: Vector2i, end: Vector2i) -> void:
	for x in range(start.x, end.x + 1):
		for y in range(start.y, end.y + 1):
			tileMap.set_cell(Vector2i(x, y), 0, Vector2.ZERO)
			#tileMap.set_cell()
