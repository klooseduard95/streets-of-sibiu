extends CharacterBody2D
class_name Player

@export var death_screen_scene: PackedScene

enum DashType{NONE, SHORT, MEDIUM, LONG}
@export var selected_dash: DashType = DashType.SHORT #this is for testing purposes and this is the one we'll use "var selected_dash = DashType.SHORT"

enum ParryType {NORMAL, MEDIUM, CHARGE }
@export var selected_parry: ParryType = ParryType.NORMAL #this is for testing purposes, and this is the one we'll use "var selected_parry = ParryType.NORMAL"

const BASE_SPEED = 500.0
const BLINK_DISTANCE = 100.0 # The instant teleport distance, chosen as such because the tile size is 100x100
const SLIDE_MOMENTUM = 1200.0 # The speed of the slide after teleporting
const FRICTION = 3000.0      # How fast the slide slows down

signal health_changed(current_hp: float, max_hp: float)
signal shield_changed(current_shield: float)

var weapon_modifiers: Dictionary = {}
var consumable_modifiers: Dictionary = {}
var buff_modifiers: Dictionary = {}

var maxHp := 100.0
var hp := maxHp :
	set(newValue):
		hp = newValue
		if hp < 1:
			$PlayerDeathSound.play()
			set_physics_process(false)
			var dead_tween = create_tween().set_trans(Tween.TRANS_SINE)
			dead_tween.tween_property($PlayerSprite, "modulate:a", 0.0, 1)
			dead_tween.parallel().tween_property($PlayerSprite, "scale", Vector2.ZERO, 1).finished.connect(_on_death_animation_finished) #de implementat functia "reload_curren_scene 

var can_take_dmg: bool = true
# var speed <- soooo why is it a constant? we should change it i think
var speed : float = BASE_SPEED # <- there, fixed it // YIPEE YIPEE YIPEE
var critChance := 0
static var slownEnemy : float = 1.0
var lifeSteal := 0
var dmgReductionRaw : float = 0.0
var bonusAmmoCapacity := 0
var sniperCooldownReduction := 0.0
var shield := 0.0
var is_charging_parry: bool = false
var is_deflecting: bool = false

var canWarp := true # used for warping between rooms

var is_sliding = false
@export var ghost_node : PackedScene
@onready var ghost_timer = $GhostTimer
@onready var dash_cooldown = $DashTimer
@onready var slow_timer = $SlownessTimer
@onready var parry_timer = $ParryTimer
@onready var inventory: InventoryComponent = get_node_or_null("Inventory")
@onready var sprite = $PlayerSprite
@onready var anim_player = $PlayerSprite/AnimationPlayer

func _ready() -> void:
	slownEnemy = 1.0
	update_health_ui()

func _physics_process(delta):
#The part where player has control
	if not is_sliding:
		handle_parry()
		var direction_vector = Vector2.ZERO
		#Movement
		if Input.is_action_pressed("move_forward"):
			direction_vector.y -= 1
	
		if Input.is_action_pressed("move_backward"):
			direction_vector.y += 1
	
		if Input.is_action_pressed("move_leftward"):
			direction_vector.x -= 1
	
		if Input.is_action_pressed("move_rightward"):
			direction_vector.x += 1
		
		if inventory:
			var currentItem = inventory.items[inventory.selected_slot_index]
			if currentItem:
				if currentItem.has_method("shoot") and Input.is_action_pressed("shoot"):
					currentItem.shoot(self)
				elif currentItem.has_method("activate") and Input.is_action_just_pressed("shoot"):
					if currentItem.activate(self):
						$PotionGulp.play()
						inventory.items[inventory.selected_slot_index] = null
						inventory.inventory_changed.emit()
				if Input.is_action_just_pressed("reload") and currentItem.has_method("reload"):
					currentItem.reload(self)
	
		#Normalizing the vector, ca sa nu poata alecs sa se miste mai repede pe diagonala
		if direction_vector.length() > 0:
			direction_vector = direction_vector.normalized()
			velocity = direction_vector * (get_stat("speed", 1.0) * speed)
			update_animation(direction_vector)
		else:
			velocity = Vector2.ZERO
			anim_player.stop()
		#the blink and slide trigger
		if Input.is_action_just_pressed("blink") and dash_cooldown.is_stopped():
			$DashSound.play()
			if direction_vector != Vector2.ZERO or selected_dash == DashType.LONG:
				match selected_dash:
					DashType.SHORT:
						global_position += direction_vector * (BLINK_DISTANCE * 2)
						velocity = direction_vector * SLIDE_MOMENTUM
						dash_cooldown.start(1.0)
						can_take_dmg = false
						var dash_tween = create_tween()
						dash_tween.tween_property($PlayerSprite, "modulate", Color(0.3, 0.5, 1.0, 0.6), 0.1)
					DashType.MEDIUM:
						global_position += direction_vector * (BLINK_DISTANCE * 2)
						velocity = direction_vector * SLIDE_MOMENTUM
						dash_cooldown.start(0.5)
						can_take_dmg = false
						if inventory:
							var current_item = inventory.items[inventory.selected_slot_index]
							if current_item:
								current_item.current_ammo = min(current_item.current_ammo + 2, current_item.ammo_capacity)
								if current_item.get("is_sniper") and "firerate" in current_item:
									current_item.firerate += 0.2
									get_tree().create_timer(1.0, 0).timeout.connect(func(): current_item.fire_rate -= 0.2)
					DashType.LONG:
						global_position = get_global_mouse_position()	
						velocity = Vector2.ZERO
						dash_cooldown.start(2.0)
						speed = BASE_SPEED * 0.66
						slow_timer.start(1.0)
			is_sliding = true
			#Start the ghost effect
			ghost_timer.start() 
			add_ghost()
	else:
		velocity = velocity.move_toward(Vector2.ZERO,FRICTION * delta)
		if velocity.length() < 10:
			is_sliding = false
			# Stop the ghost effect here
			ghost_timer.stop()
			if selected_dash == DashType.SHORT:
				can_take_dmg = true
				var end_dash_tween = create_tween()
				end_dash_tween.tween_property($PlayerSprite, "modulate", Color(1, 1, 1, 1), 0.1)
				end_dash_tween.finished.connect(func(): can_take_dmg = true)
	
	move_and_slide()

func add_ghost():
	var ghost = ghost_node.instantiate()
	get_tree().current_scene.add_child.call_deferred(ghost)
	ghost.set_property(global_position, $PlayerSprite.scale, $PlayerSprite.texture, $PlayerSprite.hframes, $PlayerSprite.vframes, $PlayerSprite.frame)

func _on_ghost_timer_timeout() -> void:
	add_ghost()

func _unhandled_input(event: InputEvent) -> void:
	if not inventory: return
	
	if event.is_action_pressed("slot_1"):
		inventory.selected_slot_index = 0
		inventory.inventory_changed.emit()
	elif event.is_action_pressed("slot_2"):
		inventory.selected_slot_index = 1
		inventory.inventory_changed.emit()
	elif event.is_action_pressed("slot_3"):
		inventory.selected_slot_index = 2
		inventory.inventory_changed.emit()
	elif event.is_action_pressed("slot_4"):
		inventory.selected_slot_index = 3
		inventory.inventory_changed.emit()
	elif event.is_action_pressed("slot_5"):
		inventory.selected_slot_index = 4
		inventory.inventory_changed.emit()

func take_dmg(damage_amount: float, invincible_time: float = 0.0, ignore_invincible: bool = false) -> void:
	if get_stat("invincible", 0.0) > 0.0:
		return
	# Charge parry check
	if is_charging_parry:
		is_charging_parry = false # End the stance
		speed = BASE_SPEED * 1.25 
		get_tree().create_timer(2.0, 0).timeout.connect(func(): speed = BASE_SPEED)
		var current_time = parry_timer.time_left
		parry_timer.start(max(0.1, current_time - 4.0))
		if inventory:
			var current_item = inventory.items[inventory.selected_slot_index]
			if current_item and "dmg" in current_item:
				var original_dmg = current_item.dmg
				current_item.dmg = current_item.dmg * 10
				print("Damage Boosted! Current DMG: ", current_item.dmg)
				await get_tree().create_timer(0.5, 0).timeout
				if is_instance_valid(current_item):
					current_item.dmg = original_dmg
					print("Damage Reverted to: ", current_item.dmg)
		$PlayerSprite.modulate = Color(1, 1, 1)
		return
	if not can_take_dmg and not ignore_invincible:
		return
	if invincible_time > 0.0:
		can_take_dmg = false
		var invincible_tween = create_tween().set_trans(Tween.TRANS_SINE)
		invincible_tween.tween_property($PlayerSprite, "modulate:a", 0.5, invincible_time/4.0)
		invincible_tween.chain().tween_property($PlayerSprite, 'modulate:a', 1.0, invincible_time/4.0)
		invincible_tween.chain().chain().tween_property($PlayerSprite, "modulate:a", 0.5, invincible_time/4.0)
		invincible_tween.chain().chain().chain().tween_property($PlayerSprite, "modulate:a", 1.0, invincible_time/4.0).finished.connect(func(): can_take_dmg = true)
		
	var total_reduction = get_stat("dmg_reduction_raw", dmgReductionRaw)
	var final_damage = max(damage_amount - total_reduction, 0)
	
	if shield > 0:
		if shield >= final_damage:
			shield -= final_damage
			final_damage = 0
		else:
			final_damage -= shield
			shield = 0
	
	hp -= final_damage
	update_health_ui()
	
func _on_death_animation_finished() -> void:
	if death_screen_scene:
		var death_screen = death_screen_scene.instantiate()
		get_tree().current_scene.add_child(death_screen)
		
		var current_level = 1
		if get_tree().current_scene is MainBackground:
			current_level = get_tree().current_scene.currentLevel
		
		death_screen.setup_death_screen(self, current_level)

func choose_dash_style(type: DashType):
	# This sets the player's dash style permanently
	selected_dash = type
	print("New dash style selected!")
	

func _on_slowness_timer_timeout() -> void:
	speed = BASE_SPEED

func apply_weapon_modifiers(modifiers: Dictionary) -> void:
	weapon_modifiers = modifiers

func clear_weapon_modifiers() -> void:
	weapon_modifiers = {}

func get_stat(stat_name: String, base_value: float) -> float:
	var total = base_value
	
	if stat_name in weapon_modifiers:
		total += weapon_modifiers[stat_name]
	
	if stat_name in consumable_modifiers:
		total += consumable_modifiers[stat_name]
	
	if stat_name in buff_modifiers:
		total += buff_modifiers[stat_name]
	
	return total

func add_temporary_buff(modifiers: Dictionary, duration: float) -> void:
	for stat in modifiers:
		if stat not in consumable_modifiers:
			consumable_modifiers[stat] = 0.0
		consumable_modifiers[stat] += modifiers[stat]
		if stat == "slown_enemy":
			Player.slownEnemy += modifiers[stat]
	
	update_health_ui()
	
	var timer = get_tree().create_timer(duration, 0)
	timer.timeout.connect(func():
		if not is_instance_valid(self):
			return
		for stat in modifiers:
			if stat in consumable_modifiers:
				consumable_modifiers[stat] -= modifiers[stat]
				if stat == "slown_enemy":
					Player.slownEnemy -= modifiers[stat]
		print("buff removed")
		update_health_ui()
	)

func add_permanent_buff(modifiers:Dictionary) ->void:
	for stat in modifiers:
		if stat == "shield":
			shield += modifiers[stat]
			continue
		if stat not in buff_modifiers:
			buff_modifiers[stat] = 0.0
		buff_modifiers[stat] += modifiers[stat]
		if stat == "slown_enemy":
			Player.slownEnemy += modifiers[stat]
		if stat == "max_hp":
			hp += modifiers[stat]
	update_health_ui()

func update_health_ui() -> void:
	health_changed.emit(hp, get_stat("max_hp", maxHp))
	shield_changed.emit(shield)

func update_animation(dir):
	if abs(dir.x) > abs(dir.y):
		if dir.x > 0:
			anim_player.play("walk_right")
		else:
			anim_player.play("walk_left")
	else:
		if dir.y > 0:
			anim_player.play("walk_down")
		else:
			anim_player.play("walk_up")

func handle_parry():
	if Input.is_action_just_pressed("parry") and parry_timer.is_stopped():
		match selected_parry:
			ParryType.NORMAL:
				trigger_normal_parry()
			ParryType.MEDIUM:
				trigger_medium_parry()
			ParryType.CHARGE:
				trigger_charge_parry()

func trigger_normal_parry():
	$ParrySound.play()
	can_take_dmg = false
	parry_timer.start(1)
	# Visual cue (Green flash)
	var p_tween = create_tween()
	p_tween.tween_property($PlayerSprite, "modulate", Color(0, 1, 0, 1), 0.1)
	p_tween.tween_property($PlayerSprite, "modulate", Color(1, 1, 1, 1), 0.1)
	get_tree().create_timer(0.2, 0).timeout.connect(func(): can_take_dmg = true)

func trigger_medium_parry():
	$ParrySound.play()
	can_take_dmg = false
	parry_timer.start(1.5)
	if inventory:
		var current_item = inventory.items[inventory.selected_slot_index]
		if current_item:
			if current_item.get("is_sniper"):
				current_item.current_ammo = current_item.ammo_capacity # Logic for full reload
			elif current_item.ammo_capacity > 0:
				current_item.current_ammo += 10 # Logic for 5 bullets
	var p_tween = create_tween()
	p_tween.tween_property($PlayerSprite, "modulate", Color(1.0, 0.8, 0.0, 1.0), 0.1)
	p_tween.tween_property($PlayerSprite, "modulate", Color(1, 1, 1, 1), 0.1)
	get_tree().create_timer(0.2, 0).timeout.connect(func(): can_take_dmg = true)

func trigger_charge_parry():
	$ParrySound.play()
	is_charging_parry = true
	speed = 0
	parry_timer.start(10.0)
	$PlayerSprite.modulate = Color(2, 0.5, 0.5) 
	# If no one hits us within 1 second, the stance ends naturally
	await get_tree().create_timer(1.0, 0).timeout
	if is_charging_parry: 
		_end_charge_parry()

func _end_charge_parry():
	is_charging_parry = false
	speed = BASE_SPEED
	$PlayerSprite.modulate = Color(1, 1, 1, 1)

func play_weapon_sound(stream: AudioStream) -> void:
	if stream == null:
		return
	
	var audio_player = AudioStreamPlayer.new()
	audio_player.stream = stream
	audio_player.set_bus("SFX")
	audio_player.finished.connect(audio_player.queue_free)
	
	add_child(audio_player)
	audio_player.play()
