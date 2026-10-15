extends Node2D
class_name MainBackground

@onready var starterRoom := load("res://background/modules/SpawnRoom.tscn")

@onready var camera :Camera2D= $"./Player/CoolCamera"
@onready var blackScreen :ColorRect=$"./Overlay/BlackScreen"
@onready var player :Player= $Player

@onready var activeRoom: RoomModule

var currentLevelRooms : Array[RoomModule]

static var hasExit :bool = false

var currentLevel := 0
var isInTransition:bool = false

var canWarp := true

func _ready():
	hasExit = false
	currentLevel = 0 # should be 0
	camera.process_mode = Node.PROCESS_MODE_ALWAYS
	camera.zoom = Vector2(1, 1)
	
	await finishLevel()
	for child in get_children():
		if is_instance_of(child, RoomModule) and child != activeRoom:
			remove_child(child)
	updateCameraAnchor(activeRoom)
	
func _physics_process(delta: float) -> void:
	player.position = player.position.clamp(Vector2(camera.limit_left+40, camera.limit_top+40), Vector2(camera.limit_right-40, camera.limit_bottom-40))

func doWarpSequence(newRoom: RoomModule, connectionNode: GenerationNode):
	canWarp = false
	player.can_take_dmg=true
	
	newRoom.process_mode = Node.PROCESS_MODE_DISABLED
	player.process_mode = Node.PROCESS_MODE_DISABLED
	
	var blackScreenTweener :Tween= create_tween()
	await blackScreenTweener.tween_property(blackScreen,"modulate:a", 1, 0.25).finished
	
	player.global_position = connectionNode.arrivalNode.global_position
	remove_child.call_deferred(activeRoom)
	activeRoom = newRoom
	add_child.call_deferred(activeRoom)
	updateCameraAnchor(activeRoom)
	
	blackScreenTweener = create_tween()
	await blackScreenTweener.tween_property(blackScreen,"modulate:a", 0, 0.25).finished
	
	activeRoom.process_mode = Node.PROCESS_MODE_INHERIT
	player.process_mode = Node.PROCESS_MODE_INHERIT
	
	canWarp = true


func updateCameraAnchor(room: RoomModule):
	var roomSizes = room.getRoomSizes()
	var anchorPos = roomSizes[0]
	var anchorWidth = roomSizes[1]
	var anchorLength = roomSizes[2]
	camera.limit_top = anchorPos.y - anchorLength/2
	camera.limit_bottom = anchorPos.y + anchorLength/2
	camera.limit_right = anchorPos.x + anchorWidth/2
	camera.limit_left = anchorPos.x - anchorWidth/2
	camera.reset_smoothing()

func finishLevel():
	currentLevel += 1
	if currentLevel > 8:
		if player.death_screen_scene:
			var win_screen = player.death_screen_scene.instantiate()
			get_tree().current_scene.add_child(win_screen)
			win_screen.setup_death_screen(player, 8, true)
		return
	await create_tween().tween_property(blackScreen,"modulate:a", 1, 0.25).finished
	freeLevel()
	await generateLevel()
	await create_tween().tween_property(blackScreen,"modulate:a", 0, 0.25).finished

func freeLevel():
	hasExit = false
	for room in currentLevelRooms:
		room.queue_free()
	currentLevelRooms = []
func generateLevel():
	for child in get_children():
		if child is GameItem:
			child.queue_free()
	isInTransition = true
	activeRoom = starterRoom.instantiate()
	add_child.call_deferred(activeRoom)
	await activeRoom.ready
	updateCameraAnchor(activeRoom)
	player.global_position = activeRoom.getRoomCenter()
	match currentLevel:
		1: await activeRoom.generateAll(2)
		2: await activeRoom.generateAll(2)
		3: await activeRoom.generateAll(2)
		4: await activeRoom.generateAll(2)
		5: await activeRoom.generateAll(2)
		6: await activeRoom.generateAll(3)
		7: await activeRoom.generateAll(3)
		8: await activeRoom.generateAll(3)
	for child in get_children():
		if is_instance_of(child, RoomModule) and child != activeRoom:
			remove_child(child)
	isInTransition = false
		
