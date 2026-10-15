extends ItemData
class_name WeaponItem

@export var projectile_scene: PackedScene

@export var modifiers: Dictionary = {}

@export var dmg : float
@export var current_ammo : int
@export var ammo_capacity : int
@export var fire_rate : float
@export var critChance : float
@export var reloadTime : float
@export var maxRange : float
@export var is_sniper: bool = false
@export var bullet_speed: float = 1200.0
@export var weapon_sound: AudioStream

@export_group("rampup mechanics")
@export var is_ramping: bool = false
@export var ramp_gain_per_hit: float = 1.0
@export var ramp_decay_per_sec: float = 2.0

var ramp_stack: float = 0.0
var last_calc_time: int = 0

var isReloading: bool = false
var canShoot: bool = true

func shoot(player: Node) -> void:
	if isReloading or not canShoot:
		return
	
	if ammo_capacity != -1 and current_ammo <= 0:
		_start_reload(player)
		return
	
	if player.has_method("play_weapon_sound"):
		player.play_weapon_sound(weapon_sound)
	
	if is_ramping:
		var now = Time.get_ticks_msec()
		if last_calc_time == 0:
			last_calc_time = now
		
		var time_passed = (now - last_calc_time) / 1000.0
		var decay = time_passed * ramp_decay_per_sec
		
		ramp_stack = max(0.0, ramp_stack - decay)
		last_calc_time = now
	
	if projectile_scene:
		var bullet = projectile_scene.instantiate()
		bullet.global_position = player.global_position
		var direction = (player.get_global_mouse_position() - player.global_position).normalized()
		bullet.direction = direction
		bullet.rotation = direction.angle()
		
		var damageMultiplier = 1.0
		damageMultiplier += player.get_stat("damage_bonus", 0.0) / 100.0
		
		var almost_final_damage = dmg
		if is_ramping:
			almost_final_damage += ramp_stack
			
		var finalDamage = almost_final_damage * damageMultiplier
		
		var totalCritChance = critChance + player.get_stat("crit_chance", 0.0)
		
		if randf() * 100 < totalCritChance:
			finalDamage *= 2.0
		
		bullet.dmg = finalDamage
		bullet.maxRange = maxRange
		bullet.speed = bullet_speed
		
		if "weapon_texture" in bullet and ammo_capacity == -1:
			bullet.weapon_texture = icon
		
		if is_ramping:
			bullet.body_entered.connect(func(body):
				if body.name == "Player": return
				if body.has_method("takeDamage"):
					ramp_stack += ramp_gain_per_hit
			)
		
		player.get_parent().add_child.call_deferred(bullet)
		
		if ammo_capacity != -1:
			current_ammo -=1
			if is_sniper:
				current_ammo = 0
			print("Ammo: %d/%d" % [current_ammo, get_total_capacity(player)])
		
		canShoot = false
		
		var delay = 0.1
		if fire_rate > 0:
			delay = 1.0 / fire_rate
		
		var tree = player.get_tree()
		if tree:
			var timer = tree.create_timer(delay, 0)
			timer.timeout.connect(func(): canShoot = true)
	

func get_total_capacity(player: Node) -> int:
	if ammo_capacity == -1:
		return -1
	
	var bonus = 0
	bonus = int(player.get_stat("bonus_weapon_capacity", 0.0))
	return ammo_capacity + bonus

func reload(player: Node) -> void:
	if ammo_capacity == -1 or isReloading:
		return
	
	if current_ammo < get_total_capacity(player):
		_start_reload(player)

func _start_reload(player: Node) -> void:
	isReloading = true
	print("Reloading")
	
	var tree = player.get_tree()
	if tree:
		var timer = tree.create_timer(reloadTime - player.get_stat("sniper_cooldown_reduction", 0.0), 0)
		timer.timeout.connect(_on_reload_finished.bind(player))

func _on_reload_finished(player: Node) -> void:
	current_ammo = get_total_capacity(player)
	isReloading = false
	print("Reload complete")

func equip(player: Node) -> void:
	player.apply_weapon_modifiers(modifiers)

func unequip(player: Node) -> void:
	player.clear_weapon_modifiers()
