# Delivery 3 — weapons, inventory, interaction, navigation, basic enemies (branch `d3-weapons-inventory-enemies`)

## What is in it
- **Weapons and bullets** (plan Phase 7): shooting with the original damage formula, crit, ammo, reload (R), capacity and reload upgrades, sniper magazine rule, chainsaw ramp-up (including the double gain per enemy), ranged and melee bullet rules, bullet shapes.
- **Inventory and interaction:** five slots, Punch in slot 3, equip/unequip of modifiers, `add_item`/`swap_item` (B10 kept, B11 fixed), ground items with the "Pick up X" / "Switch with X" label, E key and E button.
- **Consumables:** health, shield, speed, rage, armor, ghost, adrenaline (with after-effect) and stun grenade, used on the press.
- **Navigation** (plan E4): A*, funnel and agent ported from Godot 4.6.1; the 120 recorded Godot paths are reproduced exactly (worst 0.001 px) and both chase traces match within 0.0002 px. Cell order from the tile data is stored by `tools/convert_rooms.py`.
- **Basic enemies** (plan Phase 8): rat, pigeon, thief, criminal with level scaling, size roll, aggro ticks, line of sight, chase, repeating attack loop, projectiles, health bars, death, collisions with the player and each other, spawn tables.
- **HUD and input:** inventory icons, ammo label ("Ammo: a / b", "∞", "Reloading..."), left mouse or right-half touch = aim and shoot, buttons DASH, PARRY, R, E on Android.

## Dev switches (rooms do not hand out items or enemies yet; level generation is D4)
Desktop: `-Psos.items=primary_medium,Knife`, `-Psos.ground="primary_sniper@120,0;consumable_health@-300,0"`, `-Psos.enemies="rat@300,0;thief@0,-200"`, `-Psos.dash=SHORT`, `-Psos.parry=NORMAL`. Android: `-e items ... -e ground ... -e enemies ...` (escape `;` as `\;` in `adb shell "..."`). Offsets are from the spawn-room centre. Desktop screenshots can script input: `-Psos.keys="100:down:E,102:up:E"` or `100:touch:800:270,111:untouch:800:270`.

## Tests
`./gradlew :logic:test :core:test`: **152 tests, 0 failures** (78 new in D3): weapons 16, interaction and consumables 17, navigation parity 3, enemies 17, plus D2 additions carried over. Notable numbers: sniper crit rate 20.1 % (expected 20 %), size roll χ² 0.48 (limit 13.8), navigation 120/120 paths.

## Emulator
Started with `-no-snapshot-load -no-snapshot-save` and stopped afterwards. The game runs with enemies, shooting, ground items and the four action buttons; a rat chased and hurt the player. No crash in the log.

## Not in D3 (next: D4)
Ghost, manhole spawner and King Rotta (Phase 9); procedural level generation with enemies spawned from rooms, warps, barricades and exits (Phase 10); upgrades and rewards (Phase 11).
