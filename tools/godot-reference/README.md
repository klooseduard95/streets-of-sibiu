# Godot reference (Phase 0)

Ground truth for the port. Probes run in Godot **4.6.1-stable (official)** against a **copy** of the original project; the original in `2026-Game_of_the_year-main/newgame/` is never touched.

## Layout

| Path | Contents | In git |
|---|---|---|
| `setup_copy.sh` | Recreates `project-copy/` from the original, adds `probes/`, imports it | yes |
| `run_probe.sh <name>` | Runs `probes/<name>.gd` headless; writes `results/<name>.json` and `.log` | yes |
| `probes/*.gd` | Probe scripts (SceneTree scripts extending `probe_base.gd` by path) | yes |
| `results/*.json` | Recorded reference data (golden traces, paths, defaults) | yes |
| `engine-source/` | Relevant Godot 4.6.1 source files (MIT; see `engine-source/LICENSE.txt`, `COPYRIGHT.txt`), used as the porting reference | yes |
| `engine/`, `downloads/`, `project-copy/` | Godot binary, download, working copy | no (reproducible) |

## Reproduce

```bash
mkdir -p tools/godot-reference/downloads tools/godot-reference/engine
cd tools/godot-reference/downloads
curl -LO https://github.com/godotengine/godot/releases/download/4.6.1-stable/Godot_v4.6.1-stable_macos.universal.zip
curl -LO https://github.com/godotengine/godot/releases/download/4.6.1-stable/SHA512-SUMS.txt
grep 'macos.universal.zip' SHA512-SUMS.txt | grep -v mono | shasum -a 512 -c
ditto -x -k Godot_v4.6.1-stable_macos.universal.zip ../engine/
cd ../../.. && tools/godot-reference/setup_copy.sh
tools/godot-reference/run_probe.sh v9_player_traces   # etc.
```

**Why 4.6.1:** the project declares 4.6 and its files were last modified on 2026-02-22. 4.6.1 (released 2026-02-16) is the newest version its developers could have used. Signed by team `6K46PWY5DM`; zip SHA-512 verified.

**Probe gotchas:**
- New `class_name` scripts are only registered by an editor import, so probes extend `"res://probes/probe_base.gd"` by path.
- The headless root viewport is 64×64, so camera probes use a 1920×1080 SubViewport.
- The navigation map needs a few physics frames to sync; wait until `map_get_iteration_id` advances.
- `Input.action_press` takes effect for `is_action_just_pressed` on the **next** physics tick.

## Results (V1–V15)

| ID | Question | Answer | Evidence |
|---|---|---|---|
| V1 | Do `pause_menu.gd` / `death_screen.gd` load? | **No.** Both fail with *Parse Error: Unindent doesn't match the previous indentation level*. At runtime the PauseMenu and DeathScreen are created **without a script**. The in-game pause panel is **visible permanently** from the start (Resume/Settings/Main Menu with empty stats; buttons do nothing; P does nothing). The death screen cannot be set up (`setup_death_screen` missing), so the game does not pause on death and Retry/Main Menu do nothing. Decision Q4 (port the intended behaviour) stands | `--check-only`; `results/v1_runtime.json` |
| V2 | `spawnRadius` when the base scene stores `null` | **100.** The null is rejected by the typed `int` property; every room's spawn nodes use 100 | `v2_defaults.json` |
| V3 | Sprite frame after `AnimationPlayer.stop()` | **Resets to the first keyframe of the current animation** (walk_left 6 → 8, walk_up 0 → 2). Idle frames are therefore up 2, down 5, left 8, right 11 | `v9_player_traces.json` (walk_*_then_stop) |
| V4 | Camera2D scale, limits, smoothing | Node **scale ignored** (canvas scale 1). If the limit span on an axis is **smaller than the screen**, the camera **centres on the limits' midpoint** on that axis (SpawnRoom y = 544.5). Smoothing (physics callback): `pos += (target − pos) · speed · Δt`, speed 5 (first step after a 600 px jump moves 50 px). Smoothed limits ease the same way. `reset_smoothing()` snaps | `v4_camera.json`, `engine-source/scene/2d/camera_2d.cpp` |
| V5 | AudioStreamPlayer2D formula | `dist = |pos − listener|`; silent if `dist > max_distance` (default 2000). Gain = `(1 − dist/max)^attenuation · db_to_linear(volume_db)`. `pan = clamp(rel.x / screen_w, −1, 1) · panning_strength(1) · 2d_panning_strength(0.5) · 0.5 + 0.5`, clamped to 0..1; L = 1 − pan, R = pan. Listener = screen centre (no AudioListener2D; the root viewport is the 2D listener). **Consequence: the music player sits at world origin, so the music is panned left**: L/R 0.625/0.375 in the menu, 0.63/0.37 in the spawn room, up to 0.75/0.25 far east | `engine-source/scene/2d/audio_stream_player_2d.cpp` (source-derived; headless has no audio output) |
| V6 | Re-enabling a shape re-emits `body_entered` | **Yes.** Disable (deferred) at tick 5 → `body_exited` at tick 5; re-enable at tick 15 → `body_entered` at tick 15. Enemy attacks repeat while the player stays in range | `v6_areas.json` |
| V7 | Signal order, bullet overlapping enemy hitbox + body | **Hitbox first, then body**, in every trial where the bullet appears onto an existing enemy (any child order or offset). So point-blank ranged shots deal damage. Quirk: a static hitbox *added onto* a stationary area is not detected (cannot happen in-game: bullets move every tick) | `v6_areas.json` |
| V8 | Navigation paths with the enemy agent settings | Recorded 12 queries per room in 5 rooms, with the 500 px search limit and without. The limit truncates paths in medium and big rooms (5/12 in SpawnRoom, 9–12/12 elsewhere), so enemies chase in partial hops re-planned every 1 s. Funnel output contains a point at every tile-polygon crossing. Agent defaults: path_desired 20, target_desired 10, path_max_distance 100, A*, corridor funnel, no simplify. Chase traces (240 ticks) in SpawnRoom and FIGHTROOM_13_BIG | `v8_navigation.json` |
| V9 | Player positions per tick | 21 golden scenarios in the real SpawnRoom: walls, corners, pillars, NONE/SHORT/MEDIUM dash, dash into walls with and without the room clamp, parries, error paths. Notable: a Short Dash into the east wall *without* the clamp pushes the player out of the room (x 2510); *with* MainBackground's clamp they end inside at x ≈ 1870 | `v9_player_traces.json` |
| V10 | Float→int truncation; runtime errors | `dmgMult = 1.3` → **1** (int). A runtime error aborts the rest of the function: Reload Parry with a consumable selected leaves `can_take_dmg = false` permanently; Medium Dash with a consumable selected teleports but skips that tick's slide/move | `v2_defaults.json`, `v9_player_traces.json` |
| V11 | Fonts | On this Mac none of Montserrat / JetBrains Mono (NL) / Hack are installed, so **all text renders in Godot's default font, Open Sans SemiBold, size 16** (the same fallback for every machine without those fonts) | `v2_defaults.json` |
| V12 | Shared sub-resources | Confirmed shared across instances: the EnemyProjectile RectangleShape2D, the Enemy AgroRange circle and the HitRange circle (the latter is replaced per enemy at runtime) | `v2_defaults.json` |
| V13 | Integer camera limits per room | Truncation toward zero, e.g. SpawnRoom LTRB (19, 24, 1973, 1065) from raw (19, 24.875, 1973, 1065.125); 12_BIG (62, −635, 4845, 2935) | `v4_camera.json` |
| V14 | Tween/timer processing; easing | Tweens default to **idle** processing, `TWEEN_PAUSE_BOUND`, linear, EASE_IN_OUT. `create_timer(t, 0)` = idle, pausable, not process-always. SINE: in `1 − cos(πt/2)`, out `sin(πt/2)`, in_out `(1 − cos πt)/2`; samples recorded | `v2_defaults.json`, `engine-source/scene/animation/tween.h`, `scene_tree.cpp` |
| V15 | `is_action_just_pressed` in physics | Real input events: true in the **first physics tick after the event**, exactly once. `action_press` is registered for the **next** physics tick. `legacy_just_pressed_behavior = false`, so an edge counts even if the key was released again before that tick | `v9_player_traces.json` (V15), `engine-source/core/input/input.cpp` |

### Other engine settings recorded (`v0_smoke.json`, `v2_defaults.json`)

- **Physics:** 60 ticks/s, max 8 steps per frame, jitter fix 0.5, no physics interpolation.
- **Player body:** grounded mode, safe margin 0.08, max slides 4, floor snap 1, floor max angle 45°, wall_min_slide_angle 15°, floor_block_on_wall true, up (0, −1).
- **Enemy body:** floating mode, wall_min_slide_angle **0**, floor_block_on_wall false.
- **King Rotta body:** floating mode, wall_min_slide_angle **15°** (default).
- **Navigation:** edge connection margin 1, cell size 1.
- **Audio:** 2D panning strength 0.5.
- **Display:** stretch mode disabled; 2D transform snapping off.
