# Delivery 2 — dash, parry, ghost trail, damage (branch `d2-dash-parry-ghost-trail`)

## What is in it
- `Player` (logic) now ports all of `player.gd` that is not weapons/inventory: dash NONE/SHORT/MEDIUM/LONG, ghost trail, parry NORMAL/MEDIUM/CHARGE, `take_dmg` (invincible stat → charge counter → `can_take_dmg`, flicker, damage reduction, shield), death sequence, buffs and `get_stat`.
- Decisions applied: D-B2 (Medium Dash invulnerability ends with the slide, no blue tint), B18 (death happens once). Original quirks preserved: NONE mini-slide, dash without direction, B7 errors (Medium dash/parry with a consumable aborts the tick; invulnerability stays off), parry unlocked only by selection.
- Physics: `Kinematics` now generates Godot's **two contacts** for a face contact with a horizontal normal (edge–edge in the SAT). This was found through the dash traces; it also made all walking traces closer (worst error 0.073 → 0.007 px).
- Core: dash/parry sounds, ghost and tint rendering, `AimInput` (mouse position, right click = parry, right-half touch = cursor), DASH/PARRY touch buttons on Android, Space = dash.
- Dev switches (upgrades are not ported yet): desktop `-Psos.dash=SHORT|MEDIUM|LONG -Psos.parry=NORMAL|MEDIUM|CHARGE`, Android `-e dash SHORT -e parry NORMAL`. Desktop screenshots can script keys: `-Psos.keys="100:down:D,104:down:Space,105:up:Space"`.

## Tests
`./gradlew :logic:test :core:test`: **101 tests, 0 failures** (27 new). Golden traces (`v9_player_traces.json`): walking, NONE/SHORT dashes (open floor, diagonal, pillar, top door with clamp, standing still), normal parry and both consumable error paths match Godot at every tick (position ≤ 0.01 px, sprite frame identical). One scenario differs (D18).
`PlayerMechanicsTest` covers cooldowns, slide length, B2, LONG dash, ammo rules, parry rules, charge counter, damage order, flicker, death, buffs, ghost lifetime.

## Emulator
Game starts with DASH/PARRY buttons; taps work and nothing crashes. The dash with direction needs two simultaneous touches (stick + button), which `adb input` cannot do; verified on desktop instead (blue tint + grey ghosts).
