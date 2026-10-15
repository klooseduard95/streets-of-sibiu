extends CanvasLayer

@export var player: Player
@export var cards: Array[Panel]
@export var item_drop_scene: PackedScene

var small_upgrades = [
	{"desc": "+1 ammo capacity\nor\n0.05sec sniper cooldown reduction", "icon": preload("res://enemy/enemy_projectile/enemy_projectile_assets/bullet.png"), "modifiers": {"bonus_weapon_capacity": 1, "sniper_cooldown_reduction": 0.05}},
	{"desc": "+15 max hp", "icon": preload("res://item/item_assets/item icons/heal bottle.png"), "modifiers": {"max_hp": 15}},
	{"desc": "+1% speed", "icon": preload("res://item/item_assets/item icons/newbottle3.png"), "modifiers": {"speed": 0.01}},
	{"desc": "+0.5 damage reduction", "icon": preload("res://item/item_assets/item icons/newbottle1.png"), "modifiers": {"dmg_reduction_raw": 0.5}},
	{"desc": "+2% damage bonus", "icon": preload("res://item/item_assets/item icons/punch.png"), "modifiers": {"damage_bonus": 2}},
	{"desc": "+10 shield", "icon": preload("res://item/item_assets/item icons/Shield.png"), "modifiers": {"shield": 10}}
]

var big_upgrades = [
	{"desc": "+5 ammo capacity\nor\n0.1sec sniper cooldown reduction", "icon": preload("res://enemy/enemy_projectile/enemy_projectile_assets/bullet.png"), "modifiers": {"bonus_weapon_capacity": 5, "sniper_cooldown_reduction": 0.1}},
	{"desc": "+20 max hp", "icon": preload("res://item/item_assets/item icons/heal bottle.png"), "modifiers": {"max_hp": 20}},
	{"desc": "+5% speed", "icon": preload("res://item/item_assets/item icons/newbottle3.png"), "modifiers": {"speed": 0.05}},
	{"desc": "+2 damage reduction", "icon": preload("res://item/item_assets/item icons/newbottle3.png"), "modifiers": {"dmg_reduction_raw": 2}},
	{"desc": "+50 shield", "icon": preload("res://item/item_assets/item icons/Shield.png"), "modifiers": {"shield": 50}},
	{"desc": "+3% crit chance", "icon": preload("res://item/item_assets/item icons/knife.png"), "modifiers": {"crit_chance": 3}},
	{"desc": "+5% enemy slow", "icon": preload("res://item/item_assets/item icons/newbottle4.png"), "modifiers": {"slown_enemy": -0.05}},
	{"desc": "+10% damage bonus", "icon": preload("res://item/item_assets/item icons/punch.png"), "modifiers": {"damage_bonus": 10}},
	{"desc": "+30 max hp\n-5% speed", "icon": preload("res://item/item_assets/item icons/heal bottle.png"), "modifiers": {"max_hp": 30, "speed": -0.05}},
	{"desc": "+25% damage bonus\n-20 max hp", "icon": preload("res://item/item_assets/item icons/punch.png"), "modifiers": {"damage_bonus": 25, "max_hp": -20}}
]

var first_weapon_choice = [
	{"desc": "Bat", "icon": preload("res://item/item_assets/item icons/bat.png"), "is_item": true, "item_data": preload("res://item/weapons/secondary/secondary_bat.tres")},
	{"desc": "Chainsaw", "icon": preload("res://item/item_assets/item icons/chainsaw.png"), "is_item": true, "item_data": preload("res://item/weapons/secondary/secondary_chainsaw.tres")}
]

var second_weapon_choice = [
	{"desc": "SMG", "icon": preload("res://item/item_assets/item icons/smg.png"), "is_item": true, "item_data": preload("res://item/weapons/primary/primary_short.tres")},
	{"desc": "Negev", "icon": preload("res://item/item_assets/item icons/negev.png"), "is_item": true, "item_data": preload("res://item/weapons/primary/primary_alternative.tres")},
	{"desc": "Sniper", "icon": preload("res://item/item_assets/item icons/sniper.png"), "is_item": true, "item_data": preload("res://item/weapons/primary/primary_sniper.tres")},
]

var dash_choices = [
	{"desc": "Medium Dash\n2 Tiles\nReloads 2 bullets\nReduces sniper cooldown by 0.2s\nHas invincibility frames\n0.5s Cooldown", "icon": preload("res://item/item_assets/item icons/dash2.png"), "dash_style": Player.DashType.MEDIUM},
	{"desc": "Teleport Dash\nMove to cursor\n-33% Speed for 1s\nHas invincibility frames\n2s Cooldown", "icon": preload("res://item/item_assets/item icons/dash1.png"), "dash_style": Player.DashType.LONG}
]

var small_dash_choice = [
	{"desc": "Short Dash\n2 Tiles\nHas invincibility frames\n1s Cooldown", "icon": preload("res://item/item_assets/item icons/normaldash.png"), "dash_style": Player.DashType.SHORT}
]

var knife_choice = [
	{"desc": "Knife", "icon": preload("res://item/item_assets/item icons/knife.png"), "is_item": true, "item_data": preload("res://item/weapons/secondary/secondary_knife.tres")},
]

var basic_gun_choice = [
	{"desc": "Basic rifle", "icon": preload("res://item/item_assets/item icons/rifle.png"), "is_item": true, "item_data": preload("res://item/weapons/primary/primary_medium.tres")},
]

var basic_parry_choice = [
	{"desc": "Normal Parry\nNegates damage\n0.5s Cooldown", "icon": preload("res://item/item_assets/item icons/parry1.png"), "parry_style": Player.ParryType.NORMAL}
]

var parry_choices = [
	{"desc": "Charge Parry\nCharges for 1s then parries\nCan't move during charge up\nIf successful x2 primary weapons damage for 1s, reduces parry cooldown by 4s and +25% speed\n10s Cooldown", "icon": preload("res://item/item_assets/item icons/parry2.png"), "parry_style": Player.ParryType.CHARGE},
	{"desc": "Reload Parry\nNegates damage\nReloads 10 bullets\n1s Cooldown", "icon": preload("res://item/item_assets/item icons/parry3.png"), "parry_style": Player.ParryType.MEDIUM}
	
]

var consumable_choices = [
	{"desc": "Health Potion", "icon": preload("res://item/item_assets/item icons/heal bottle.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_health.tres")},
	{"desc": "Shield", "icon": preload("res://item/item_assets/item icons/Shield.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_shield.tres")},
	{"desc": "Speed", "icon": preload("res://item/item_assets/item icons/newbottle3.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_speed.tres")},
	{"desc": "Double Damage", "icon": preload("res://item/item_assets/item icons/newbottle2.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_x2dmg.tres")},
	{"desc": "Damage Reduction", "icon": preload("res://item/item_assets/item icons/newbottle4.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_dmg_reduction.tres")},
	{"desc": "Stun Enemies", "icon": preload("res://item/item_assets/item icons/newbottle3.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_enemy_stun.tres")},
	{"desc": "Invincibility", "icon": preload("res://item/item_assets/item icons/newbottle1.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_ghost.tres")},
	{"desc": "Temporary Damage Bonus", "icon": preload("res://item/item_assets/item icons/newbottle2.png"), "is_item": true, "item_data": preload("res://item/consumables/consumable_ups_and_downs.tres")}
]

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	visible= false
	get_tree().paused=false

func show_upgrades(reward_type: String) -> void:
	var pool: Array = []
	var num_choices: int = 3
	var shuffle: bool = true
	
	match reward_type:
		"small_upgrades":
			pool = small_upgrades.duplicate()
			num_choices = 3
		"big_upgrades":
			pool = big_upgrades.duplicate()
			num_choices = 3
		"first_weapons":
			pool = first_weapon_choice.duplicate()
			num_choices = 2
		"second_weapons":
			pool = second_weapon_choice.duplicate()
			num_choices = 3
			shuffle = false
		"dash":
			pool = dash_choices.duplicate()
			num_choices = 2
			shuffle = false
		"small_dash":
			pool = small_dash_choice.duplicate()
			num_choices = 1
			shuffle = false
		"knife":
			pool = knife_choice.duplicate()
			num_choices = 1
			shuffle = false
		"basic_gun":
			pool = basic_gun_choice.duplicate()
			num_choices = 1
			shuffle = false
		"basic_parry":
			pool = basic_parry_choice.duplicate()
			num_choices = 1
			shuffle = false
		"parry":
			pool = parry_choices.duplicate()
			num_choices = 2
			shuffle = false
		"consumables":
			pool = consumable_choices.duplicate()
			num_choices = 2
		"small_consumable":
			var temp_small = small_upgrades.duplicate()
			temp_small.shuffle()
			
			var temp_cons = consumable_choices.duplicate()
			temp_cons.shuffle()
			
			pool = [temp_small[0], temp_cons[0]]
			num_choices = 2
	
	if shuffle:
		pool.shuffle()
	
	for card in cards:
		card.visible = false
	
	var limit = min(num_choices, cards.size())
	for i in range(limit):
		if i < pool.size():
			cards[i].setup_card(pool[i], player, self)
			cards[i].visible = true
	
	visible = true
	get_tree().paused = true
	
	$SelectUpgrade.modulate.a = 0.0
	var fade_in = create_tween().set_trans(Tween.TRANS_SINE)
	fade_in.tween_property($SelectUpgrade, "modulate:a", 1.0, 0.3)

func close_upgrade_window() -> void:
	var fade_out = create_tween().set_trans(Tween.TRANS_SINE)
	fade_out.tween_property($SelectUpgrade, "modulate:a", 0.0, 0.3)
	await fade_out.finished
	
	visible = false
	get_tree().paused = false

func _input(event: InputEvent) -> void:
	#if Input.is_action_just_pressed("upgrade") and not visible:
		#show_upgrades("small_upgrades")
	pass
