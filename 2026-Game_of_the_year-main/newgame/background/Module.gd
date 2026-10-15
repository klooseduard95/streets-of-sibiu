extends Node2D
class_name RoomModule

@onready var genNodes := $GenerationNodes
@onready var hitBox := $ModuleBox
@onready var hitboxShape :CollisionShape2D= $"./ModuleBox/Shape"
@onready var bg := $".."
@onready var upgradeWindow := $"../UpgradeWindow"
var finisherNode := load("res://background/FinisherSpecialNode.tscn")
var exitFinisher := load("res://background/finishers/ExitFinisher.tscn")

var opposite = {
	"north": "south",
	"south": "north",
	"east": "west",
	"west": "east",
	"disabled": "like this should never be accessed but might as well have it here"
}

var genNodeList :Array[GenerationNode]

var isExitRoom:=false
var roomType:String

var roomSceneList := GlobalUtils.roomSceneList
var finishers := GlobalUtils.finishers
var fourWays := GlobalUtils.fourWays
var threeWays := GlobalUtils.threeWays
var bigTwoWays := GlobalUtils.bigTwoWays
var smallTwoWays := GlobalUtils.smallTwoWays

signal validCheck(valid:bool)

var enemyCount:int :
	set(newValue):
		enemyCount = newValue
		if newValue == 0:
			enableWarps()
		else:
			disableWarps()

func _ready() -> void:
	for child: GenerationNode in genNodes.get_children():
		if child.orientation != "disabled":
			genNodeList.append(child)
	if enemyCount !=0:
		disableWarps()
	

func snapToNode(node: GenerationNode) -> GenerationNode:
	await ready
	for availableNode in genNodeList:
		if availableNode.orientation == opposite[node.orientation]:
			global_position = node.global_position + (global_position-availableNode.global_position)
			availableNode.orientation = "disabled"
			availableNode.connection = node
			#genNodeList.erase(availableNode)
			return availableNode
	return null
	

func checkValidity():
	await get_tree().physics_frame
	if !await hitBox.checkValidity():
		validCheck.emit(false)
		queue_free()
		return
	validCheck.emit(true)

func generateFromNode(n: GenerationNode, depth:int):
	var generatableRooms:= []
	var generatableRoomTypes:= []
	#TODO: make it not fuckass
	match depth:
		1: generatableRooms = [finishers.duplicate()]; generatableRoomTypes = ["finisher"]
		2: generatableRooms = [smallTwoWays.duplicate(),bigTwoWays.duplicate()]; generatableRoomTypes = ["small", "big"]
		3: generatableRooms = [bigTwoWays.duplicate(), threeWays.duplicate()]; generatableRoomTypes = ["big", "small"]
		4: generatableRooms = [fourWays.duplicate(), threeWays.duplicate()]; generatableRoomTypes = ["small", "small"]
		_: generatableRooms = [smallTwoWays.duplicate(), threeWays.duplicate(), fourWays.duplicate()]; ; generatableRoomTypes = ["small", "small", "small"]


	while !generatableRooms.is_empty():
		var roomTypeIndex = randi_range(0, generatableRooms.size()-1)
		var randRoomType :Array= generatableRooms[roomTypeIndex]
		var randRoomTypeString :String= generatableRoomTypes[roomTypeIndex]
		generatableRooms.remove_at(roomTypeIndex)
		
		while !randRoomType.is_empty():
			var roomIndex = randi_range(0, randRoomType.size()-1)
			var randRoom :PackedScene= randRoomType[roomIndex]
			randRoomType.remove_at(roomIndex)
			
			var newRoom :RoomModule= randRoom.instantiate()
			
			bg.add_child.call_deferred(newRoom)
			var connectingNode = await newRoom.snapToNode(n)
			if connectingNode == null:
				newRoom.queue_free()
				continue
			
			newRoom.checkValidity()
			
			var isValid = await newRoom.validCheck
			if !isValid: continue
			
			n.connection = connectingNode
			n.roomConnection = newRoom
			n.orientation = "disabled"
			bg.currentLevelRooms.append(newRoom)
			newRoom.roomType = randRoomTypeString
			if depth == 1:
				newRoom.createFinisherNode()
			
			if depth >= 2:
				await newRoom.generateAll(depth-1)
			return
	
	n.barricadeNode()

func generateAll(depth:int):
	genNodeList.shuffle()
	for node in genNodeList:
		await get_tree().process_frame
		await generateFromNode(node, depth)

func getRoomSizes():
	var shapePos = hitboxShape.global_position
	var shapeWidth = hitboxShape.shape.get_rect().size.x
	var shapeLength = hitboxShape.shape.get_rect().size.y
	return [shapePos, shapeWidth, shapeLength]

func getRoomCenter():
	return hitboxShape.global_position

func createFinisherNode():
	var finisherThingo = finisherNode.instantiate()
	if !MainBackground.hasExit:
		finisherThingo.type = "exit"
		MainBackground.hasExit = true
		isExitRoom = true
	finisherThingo.spawnPos = getRoomCenter()
	add_child(finisherThingo)

func enableWarps():
	await get_tree().create_timer(1, 0).timeout
	if isExitRoom:
		match bg.currentLevel:
			1:
				upgradeWindow.show_upgrades("knife")
				await get_tree().create_timer(0.1, 0).timeout
			2:
				upgradeWindow.show_upgrades("first_weapons")
				await get_tree().create_timer(0.1, 0).timeout
				upgradeWindow.show_upgrades("small_dash")
				await get_tree().create_timer(0.1, 0).timeout
			3:
				upgradeWindow.show_upgrades("basic_gun")
				await get_tree().create_timer(0.1, 0).timeout
			4:
				upgradeWindow.show_upgrades("dash")
				await get_tree().create_timer(0.1, 0).timeout
			5:
				upgradeWindow.show_upgrades("second_weapons")
				await get_tree().create_timer(0.1, 0).timeout
				upgradeWindow.show_upgrades("basic_parry")
				await get_tree().create_timer(0.1, 0).timeout
			6:
				upgradeWindow.show_upgrades("parry")
				await get_tree().create_timer(0.1, 0).timeout
			7:
				upgradeWindow.show_upgrades("big_upgrades")
				await get_tree().create_timer(0.1, 0).timeout
				
		
		upgradeWindow.show_upgrades("big_upgrades")
		await get_tree().create_timer(0.1, 0).timeout
		
		var exit = exitFinisher.instantiate()
		exit.spawnPos = getRoomCenter()
		add_child.call_deferred(exit)
	else:
		match roomType:
			"small":
				upgradeWindow.show_upgrades("small_upgrades")
			"big":
				upgradeWindow.show_upgrades("big_upgrades")
	for node:GenerationNode in genNodeList:
		node.enableWarpable()

func disableWarps():
	for node:GenerationNode in genNodeList:
		node.disableWarpable()
