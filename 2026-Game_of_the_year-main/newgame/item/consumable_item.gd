extends ItemData
class_name ConsumableItem

@export var hp : int
@export var shield : int

@export var buffDuration : float = 0.0
@export var modifiers: Dictionary = {}

@export var after_effect_duration : float = 0.0
@export var after_effect_modifiers: Dictionary = {}

func activate(player: Node) -> bool:
	var used = false
	print("added buff")
	
	if hp != 0:
		if player.hp < player.maxHp and hp > 0:
			player.hp = min (player.hp + hp, player.get_stat("max_hp", player.maxHp))
			used = true
		elif hp < 0:
			player.hp += hp
			used = true
	
	if shield > 0:
		player.shield += shield 
		used = true
	
	if buffDuration > 0 and not modifiers.is_empty():
		player.add_temporary_buff(modifiers, buffDuration)
		used = true
		
		if not after_effect_modifiers.is_empty() and after_effect_duration > 0:
			var tree = player.get_tree()
			if tree:
				var timer = tree.create_timer(buffDuration, 0)
				timer.timeout.connect(func():
					if is_instance_valid(player):
						player.add_temporary_buff(after_effect_modifiers, after_effect_duration)
				)
	
	if used:
		player.update_health_ui()
	
	return used
