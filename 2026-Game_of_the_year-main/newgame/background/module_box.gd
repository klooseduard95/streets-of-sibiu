extends Area2D
class_name ModuleBox

func checkValidity() -> int:
	await get_tree().physics_frame
	for area in get_overlapping_areas():
		if is_instance_of(area, ModuleBox):
			return 0
	return 1
	
	


func _on_area_entered(area: Area2D) -> void:
	if is_instance_of(area, ModuleBox):
		pass
