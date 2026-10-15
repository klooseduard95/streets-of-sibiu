extends Node

var roomSceneList : Array[PackedScene] = [
	load("res://background/modules/FIGHTROOMBIGENEMY_1.tscn"),
	load("res://background/modules/FIGHTROOMBIGENEMY_2.tscn"),
	load("res://background/modules/FIGHTROOMBIGENEMY_3.tscn"),
	load("res://background/modules/FIGHTROOMBIGENEMY_4.tscn"),
	load("res://background/modules/FIGHTROOMBIGENEMY_1234.tscn"),
	load("res://background/modules/FIGHTROOM_12.tscn"),
	load("res://background/modules/FIGHTROOM_12_BIG.tscn"),
	load("res://background/modules/FIGHTROOM_12_MEDIUM.tscn"),
	load("res://background/modules/FIGHTROOM_13_BIG.tscn"),
	load("res://background/modules/FIGHTROOM_13_MEDIUM.tscn"),
	load("res://background/modules/FIGHTROOM_13_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_24_MEDIUM.tscn"),
	load("res://background/modules/FIGHTROOM_34_BIG.tscn"),
	load("res://background/modules/FIGHTROOM_34_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_1234_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_234_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_134_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_124_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_123_SMALL.tscn")
]

var finishers : Array[PackedScene] = [
	load("res://background/modules/FIGHTROOMBIGENEMY_1.tscn"),
	load("res://background/modules/FIGHTROOMBIGENEMY_2.tscn"),
	load("res://background/modules/FIGHTROOMBIGENEMY_3.tscn"),
	load("res://background/modules/FIGHTROOMBIGENEMY_4.tscn")
]

var fourWays: Array[PackedScene] = [
	load("res://background/modules/FIGHTROOMBIGENEMY_1234.tscn"),
	load("res://background/modules/FIGHTROOM_1234_SMALL.tscn")
]

var threeWays : Array[PackedScene] = [
	load("res://background/modules/FIGHTROOM_234_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_134_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_124_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_123_SMALL.tscn")
]

var smallTwoWays : Array[PackedScene] =[
	load("res://background/modules/FIGHTROOM_12.tscn"),
	load("res://background/modules/FIGHTROOM_13_SMALL.tscn"),
	load("res://background/modules/FIGHTROOM_34_SMALL.tscn")
]

var bigTwoWays : Array[PackedScene] =[
	load("res://background/modules/FIGHTROOM_12_BIG.tscn"),
	load("res://background/modules/FIGHTROOM_12_MEDIUM.tscn"),
	load("res://background/modules/FIGHTROOM_13_BIG.tscn"),
	load("res://background/modules/FIGHTROOM_13_MEDIUM.tscn"),
	load("res://background/modules/FIGHTROOM_24_MEDIUM.tscn"),
	load("res://background/modules/FIGHTROOM_34_BIG.tscn"),
]
