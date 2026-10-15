extends Control
@onready var overlay = $ColorRect

# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	pass


func _on_start_pressed() -> void:
	$TutorialScreen.show()

func _on_settings_pressed() -> void:
	get_tree().change_scene_to_file("res://scenes/Settings.tscn")


func _on_exit_pressed() -> void:
	get_tree().quit()


func _on_button_pressed() -> void:
	overlay.visible = !overlay.visible
	
