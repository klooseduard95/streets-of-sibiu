extends Control

func _on_goback_pressed() -> void:
	if get_parent().name == "PauseMenu":
		hide()
	else:
		get_tree().change_scene_to_file("res://main_menu/main_menu.tscn")
