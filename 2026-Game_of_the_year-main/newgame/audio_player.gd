extends Node2D

@onready var musicPlayer :AudioStreamPlayer2D = $AudioStreamPlayer2D

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	pass

# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	pass


func _on_horse_timer_timeout() -> void:
	if randi_range(1, 250) == 67:
		$HorsePlayer.play()


func _on_audio_stream_player_2d_finished() -> void:
	musicPlayer.play() # inginerie
