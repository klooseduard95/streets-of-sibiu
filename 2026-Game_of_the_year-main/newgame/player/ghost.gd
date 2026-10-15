extends Sprite2D

func _ready():
	ghosting()

func set_property(tx_pos, tx_scale, tx_texture, tx_hframes, tx_vframes, tx_frame):
	global_position = tx_pos
	scale = tx_scale
	texture = tx_texture
	hframes = tx_hframes
	vframes = tx_vframes
	frame = tx_frame

func ghosting():
	var tween_fade = get_tree().create_tween()
	tween_fade.tween_property(self, "self_modulate", Color(1.0, 1.0, 1.0, 0.0), 0.75)
	await tween_fade.finished
	
	queue_free()
	
