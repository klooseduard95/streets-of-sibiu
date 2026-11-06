# Migration Log — Streets of Sibiu

A chronological record of everything done in this migration: requests, decisions, commands, findings and results. New entries are appended at the end. Use it to rebuild context in a new session.

Related documents:
- `MIGRATION_PLAN.md` — the audit and the phased plan.
- `MIGRATION_STATUS.md` — current state, test results and the differences register.

## Working agreements (from the user)

- Recreate the Godot 4.6 game `2026-Game_of_the_year-main/newgame/` as a Kotlin/libGDX Android app. Never modify the original project.
- Preserve the original gameplay. Record every unavoidable difference in the register in `MIGRATION_STATUS.md`.
- Work iteratively, one phase at a time. **The user delivers every 2 weeks; the first deadline is the week of 2026-10-19 (date to confirm).**
- **No Android phone is available (Apple devices only).** Test on the Android emulator (AVD `Medium_Phone_API_37.0`) and the desktop build. Physical-device validation is deferred.
- Log everything here.

## Environment facts

- macOS (arm64). Repository root: `/Users/kloos/AndroidStudioProjects/StreetsofSibiu`.
- Run Gradle with Android Studio's JDK: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (JBR 25.0.3). The desktop run task uses a JDK 21 toolchain (found automatically).
- Android SDK at `~/Library/Android/sdk`. Emulator: `~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_37.0 -no-window -no-audio`. adb at `~/Library/Android/sdk/platform-tools/adb`.
- Screen capture from the terminal is not permitted. Use the desktop screenshot harness (`-Psos.screenshot=…`) or `adb exec-out screencap -p`.

---

## 2026-10-15 — Session 1: audit (Phase 1 of the request, "audit only")

**Request:** audit the Godot project and write `MIGRATION_PLAN.md`; do not implement anything.

**Done:**
- Read all 42 `.gd` files, 57 `.tscn` scenes, 24 `.tres` resources, `project.godot`, the bus layout, export presets and import settings.
- Decoded the binary TileMapLayer data of all 21 room scenes with a scratch Python script. Format 0: 2-byte header, then 12 bytes per cell (int16 x, int16 y, uint16 source, atlasX, atlasY, alt). All rooms share the same tile semantics: solid = row 0 except (1,0), plus row 4; walkable = (1,0) and rows 1–3.
- Measured all images and audio. Found no plugins, no save system and no font files.

**Key findings** (details in plan §6–§7):
- `death_screen.gd` line 28 and `pause_menu.gd` line 47 use mixed tab/space indentation and may fail to parse (V1).
- Gameplay-relevant original bugs B1–B19, e.g. B2 (Medium Dash permanent invulnerability) and B4 (ghost buff truncated to int).
- Engine-emergent behaviour E1–E18, e.g. a shared projectile hitbox, area re-enable re-firing `body_entered`, navigation search limits.
- Framework choice: libGDX without Box2D, plus a custom kinematic/overlap/navigation layer.

**Output:** `MIGRATION_PLAN.md`.

## 2026-10-15 — Session 2: approval and Phase 1

**User:** "Approved, go with your recommendations and start Phase 1." Q1–Q9 accepted. Q7: launcher icon derived from the player sprite.

**Done:**
- Baseline: the template project built as-is.
- Found libGDX 1.14.2 and Kotlin 2.4.21 on Maven Central. AGP 9.4.1 uses its built-in Kotlin; the root declares the `kotlin-jvm` plugin 2.4.21.
- Created modules `:logic` (pure Kotlin), `:core` (libGDX), `:desktop` (LWJGL3, dev only). Converted `:app` into the libGDX Android launcher.
- Removed template parts: AppCompat, Material, core-ktx, Espresso, the example tests, the template theme and icons.
- Native-libs Sync task `copyAndroidNatives` (configuration-cache safe: one configuration per ABI; `ArchiveOperations` injected via an interface, held in a local val).
- Copied 57 runtime assets into `assets/` under their original relative paths (Android assets dir + desktop working dir).
- `MenuBackdropScreen` shows `menu.png` in a FitViewport 1920×1080.
- Tests: `GodotProjectSettingsParityTest` (3), `AssetIntegrityTest` (2, MD5 against the originals).
- Desktop `ScreenshotHarness` (`-Psos.screenshot`, `-Psos.window`).
- `tools/make_launcher_icons.py`.

**Problems hit and fixes:**
- `variantOf` is only usable inside a `dependencies {}` block.
- The configuration cache cannot serialize a script `zipTree` reference → use an injected `ArchiveOperations`.
- LWJGL 3.3.3 JNI warning on JDK 25 → JDK 21 toolchain for `:desktop:run`.

**Results:**
- All builds and tests pass.
- Desktop frame pixel-identical to `menu.png`.
- Emulator: installs, launches, 240 px pillarbox, pixel-identical content, survives home/resume, no logcat errors.
- Release R8 build OK.
- Physical device: not tested (none available).

**Notes:**
- `gdx-backend-android` pulls in `androidx.core` (and transitively lifecycle and coroutines). We do not use them.
- The first relaunch shows Android's "Viewing full screen" system hint for immersive mode.

## 2026-10-15 — Session 3: decisions, git, Phase 0

**User:** "Fix the Medium Dash, init git, and continue with Phase 0. I have to deliver something each 2 weeks (first deadline is next week) so we will work iteratively in phases, not all at once. I don't have an Android phone to use right now. I have only Apple devices, so we'll test later. Save everything we do in a log file for future use and context."

**Decisions recorded:**
- **B2 → fix (D-B2):** Medium Dash invulnerability ends when its slide ends, like the Short Dash. To be implemented in Phase 6.
- Physical-device testing deferred. The emulator is the Android test target until a phone is available.
- This log file created.

**Git:**
- `git init -b main`. `.gitignore` additions: `.kotlin/`, the original zip (duplicate of the committed folder), and the Godot engine/download/copy folders.
- Checked the original project for CRLF files before committing: none, so its nested `.gitattributes` changes nothing.
- Baseline commit `cd77101` on `main`: audit + Phase 1 + the original project as read-only reference.
- Phase 0 work is on branch `phase-0-godot-reference`.

**Phase 0 (Godot reference):**
- Chose **Godot 4.6.1-stable**: newest release before the project's last file date (2026-02-22). Downloaded the official macOS universal zip into `tools/godot-reference/downloads/`, verified SHA-512 against `SHA512-SUMS.txt`, extracted to `engine/`. `--version` → `4.6.1.stable.official.14d19694e`.
- `setup_copy.sh` copies the original to `project-copy/` and imports it headless. (The editor UI language there is Romanian; harmless.)
- `--check-only` on every script: only `death_screen.gd` and `pause_menu.gd` have real parse errors. The "GlobalUtils not found" errors are a `--check-only` artifact, because autoloads are not registered in that mode.
- Probe framework: SceneTree scripts extending `probe_base.gd` by path (new `class_name`s need an editor import). The runner writes JSON plus a log to `results/`.

**Probes and fixes along the way:**
- `v0_smoke`: autoloads load in script mode; headless root viewport is 64×64, so camera probes use a 1920×1080 SubViewport.
- `v1_runtime`: V1 confirmed at runtime. In the real game the pause panel is visible and dead from the start. Level 1 generated 6 rooms in the probe run.
- `v2_defaults`: V2 = 100, V10 truncation, V11 fonts all fall back to Open Sans SemiBold 16, V12 shared shapes, agent/camera/body/audio defaults, easing samples.
- `v9_player_traces`:
  - First run: dashes did not trigger, because `Input.action_press` is only "just pressed" on the next physics tick and my schedule released the move key on that tick. Fixed the schedules and documented the semantics in the results.
  - Added room-clamp scenarios after seeing a Short Dash push the player out through the east wall (x 2510) without MainBackground's clamp.
  - Moved the stop ticks so V3 is unambiguous.
- `v4_camera`: scale ignored, midpoint centring, exponential smoothing, integer limits per room.
- `v6_areas`: re-enable re-fires `body_entered`; bullet order is hitbox then body.
- `v8_navigation`: the first run returned empty SpawnRoom paths because the navigation map had not synced. Now waits for `map_get_iteration_id` to advance; all rooms return paths. The 500 px limit truncates most medium/big-room paths.
- Engine source (MIT) for audio panning, tween, scene tree, input, camera, character body, navigation agent/map/queries, area, physics space, easing, animation player, audio server → `tools/godot-reference/engine-source/` (with LICENSE and COPYRIGHT).
- V5 from source: the music source at world origin makes the music left-panned (0.625/0.375 in the menu). The root viewport is the 2D audio listener (`scene_tree.cpp:2076`).
- V14/V15 from source: tween default idle / PAUSE_BOUND / linear / EASE_IN_OUT; `action_press` sets `pressed_physics_frame = physics_frames + 1`; `legacy_just_pressed_behavior = false`.

**Documents updated:**
- `tools/godot-reference/README.md`: reproduction steps and the V1–V15 answers table.
- `MIGRATION_PLAN.md`: §8.3 answered; B16 and E18 resolved; B2 decision; new §10 delivery schedule proposal (D1 2026-10-22 … D6 2027-01-14).
- `MIGRATION_STATUS.md`: Phase 0 done; physical device deferred (D11); open questions O1 (fonts), O2 (music panning), O3 (delivery dates).

**Next:** Phase 2 (runtime foundation), Phase 3 (data/room conversion), Phase 4 (room rendering + camera), targeting D1.

## 2026-10-15 — Session 4: run instructions and Phase 2

**User:** asked how to run the build. Explained the desktop run (`./gradlew :desktop:run` with Android Studio's JDK), the emulator (Android Studio or adb) and opening the Godot reference copy. The user's first attempt typed the folder path without `cd` (zsh "permission denied" means a directory was executed); fixed. **User:** "it works, continue with Phase 2."

**Phase 2, on branch `phase-2-runtime-foundation`** (branched from `phase-0-godot-reference`, so it contains Phase 0):
- Fetched more Godot 4.6.1 sources: `main/main.cpp`, `main/main_timer_sync.*`, `scene/main/timer.cpp`, `core/math/random_pcg.*`, `core/variant/array.cpp`, `scene/animation/tween.h`. Read the exact rules: `Main::iteration` order, max-steps excess handling, `SceneTree::physics_process`/`process` order, `process_timers` (`<= 0`, skip new timers), Timer node (`< 0`, `+= wait_time`), `Tween::step` (leftover delta, loops), `PropertyTweener` (start value at step start), easing tables (sine in double, returned as float), `randi_range`, `Array.shuffle`, `action_press` (+1 frame).
- Decided not to port `MainTimerSync`'s jitter fix (D12). It only shifts ticks between rendered frames.
- Wrote `logic/runtime/`: `SceneNode`, `Timers`, `Tween` (+`Easing`), `GameRandom`, `GameRuntime`, `GameLoop` (+`FrameHooks`), `ActionInput`.
- Computed the expected timer frames independently with Python double arithmetic (0.1 s fires on frame 7, not 6, because of floating point; Godot does the same).
- Tests: 6 suites, 39 tests. The easing parity test reads Godot's recorded samples from `tools/godot-reference/results/v2_defaults.json`.

**Interruption:** the user's laptop slept mid-session. My last message had announced the tween and random files without writing them. On resume I checked the disk (only `SceneNode.kt` and `Timers.kt` existed) and continued from there. Nothing was lost.

**Results:** `:logic:test` 42/42 pass. The full build is clean (`--warning-mode all`). The original project is untouched.

**Deferred on purpose:** `GameEvents` (no consumer yet), and wiring `GameLoop` into `:core` (Phase 4).

**Next:** Phase 3 (room converter + data), Phase 4 (room rendering + camera), aiming at D1 (2026-10-22, to confirm).

## 2026-10-15 — Session 5: Phase 3 (data and rooms)

**User:** "continue with Phase 3". Branch `phase-3-data-and-rooms` (from `phase-2-runtime-foundation`).

**Done:**
- Audited the room scene structure. Every room has the same node layout: root, TileMapLayer (no transform), GenerationNodes and ModuleBox with no props, GN instances (position/rotation/scale/orientation only), Shape, EnemySpawnNodes. No overrides inside instances; one atlas definition everywhere.
- Found there are **20** room modules, not 21. Corrected the plan.
- Data model `logic/math` (`Vec2`, `Rect`) and `logic/data/rooms` (`RoomModel`, `RoomCatalog`).
- Decided on generated Kotlin instead of JSON for room data: no JSON dependency in `:logic`, compile-time checked, still verified against the original.
- `tools/convert_rooms.py` generates `GeneratedRooms.kt` (float32 rounding, door geometry from `GenerationNode.tscn`).
- Transcribed data: `Stats`, `Enemies` (stats, looks, scaling, attacks), `Items` (weapons, consumables), `Upgrades` (pools, reward rules), `LevelRules` (depth, pools, barricades, rewards, spawn tables), `PlayerDefs`.
- Test-side Godot text parser `parity/GodotText.kt`, plus `RoomParityTest` (9) and `GameDataParityTest` (15).

**Problems and fixes:**
- Kotlin nested comments: `/*` inside KDoc paths (`enemy_types/*.tres`) opened comments that never closed. Reworded.
- zsh aborted a command line on an unquoted `--include=*.kt` glob. Quote globs.
- **External edit at 16:07** to `build.gradle.kts`/`settings.gradle.kts` (Android Studio "Configure Kotlin"): duplicate plugin, `jvmToolchain(8)`. It broke the build. Asked the user; **reverted with approval**. User advised to dismiss that prompt in future.
- First parity run: 3 failures.
  - **Real data error:** the Adrenaline description is missing an empty line. Fixed.
  - **Test bugs:** `Array[PackedScene]` contains `]`; `=[` without a space; an over-broad call regex. Fixed.
- My hand-computed tile total (12045) was wrong; the correct sum from the per-room counts is 10565. I also removed a meaningless filler assertion I had written.
- Mutation check: changed a weapon value and a tile. Both caught; restored. The converter is reproducible.

**Results:** `:logic:test` 66/66. The full build is clean. The original is untouched.

**Next:** Phase 4 (room rendering + camera, wiring the game loop into `:core`) to complete D1.

## 2026-10-15 — Session 6: Phase 4 (rooms and camera)

**User:** "continue with Phase 4. Font i guess is fine for now, fix the left-panned music and i have to deliver a first part (not the whole game just maybe the start of the game and the menu) until wednesday morning."

**Decisions recorded:**
- O1: fonts are fine for now (keep Q6).
- O2: centre the music (D13).
- O3: first delivery **Wed 2026-10-21 morning**, covering the menu and the start of the game. D1 scope revised in plan §10.

**Phase 4, on branch `phase-4-rooms-camera`:**
- Read Godot `camera_2d.cpp`: `get_camera_transform`, `_update_scroll`, notifications. With smoothing on, it updates once per physics tick in the internal physics process; `reset_smoothing` = `_update_scroll` + snap; ENTER_TREE sets first = true.
- `CameraRig` (logic) + `CameraRigTest` (replays the `v4_camera` probe: limits, 70 smoothing ticks, reset; integer room limits).
- Default clear colour confirmed from `main.cpp`: (0.3, 0.3, 0.3), not overridden by the project.
- `:core`: `GodotView` (y-down), `RoomRenderer`, `LoopScreen`, `RoomViewerScreen`; start-screen option on desktop (`-Psos.screen=rooms`) and Android (`-e screen rooms`); `RoomCaptureHarness` (`-Psos.captureRooms`).
- Godot probe `v10_room_screens` (windowed; a Godot window appears briefly) + `tools/compare_room_screens.py`.

**Pixel-comparison iterations:**
1. 2/40 identical. Two causes:
   - The probe's SubViewport used linear filtering. Fixed by setting Nearest, as the root viewport does.
   - The corner views were never limited (enterTree snap).
2. 0/40: seams from atlas bleeding at half-pixel positions. Found Godot's `use_texture_padding` (`tile_set.cpp`, default true) and implemented `PaddedTileAtlas`. Also, `reset_smoothing` only takes effect on the next update, so the harness now advances one tick per frame.
3. **34/40 identical.** The remaining 6 have half-pixel x camera positions. Tried ±0.01 and ±0.25 px biases: no change. ~96 % of the differing pixels are exact 1-px texel shifts. Accepted as D14 to protect the deadline.

**Also:**
- Reference PNGs re-saved losslessly optimised (9.6 MB, kept in git).
- Emulator black screenshots were caused by the device sleeping. Fixed with `KEYCODE_WAKEUP` + `svc power stayon true`.

**Results:** 72 tests pass; the build is clean; the emulator room viewer works; the original is untouched.

**Next:** D1. Main menu, tutorial, options, centred music, then the level-1 start screen.

## 2026-10-15 — Session 7: Delivery 1 (menu + start of the game)

**User:** "continue, walking is more important than the retro effect". Branch `d1-menu-and-start` (from `phase-4-rooms-camera`).

**Walking:**
- Read Godot `character_body_2d.cpp` (`move_and_slide`, grounded/floating, floor snap), `physics_body_2d.cpp` (`move_and_collide` cancel-sliding) and `godot_space_2d.cpp` (`test_body_motion`: recovery 4×, 40 %, min contact depth margin × 0.05, 8-step bisection with margin 0, rest contact at the unsafe position; `TEST_MOTION_MARGIN_MIN_VALUE` 0.0001).
- Implemented `logic/physics` (exact TOI via a ray vs rounded-rectangle Minkowski region, fed into Godot's bisection) and `KinematicBody`.
- First replay of the 9 walking traces: max error 0.073 px. Then `Player` + `FrameAnimator`; end-to-end replay through `GameLoop`: frames identical. Committed `73b81a9`.

**UI and audio:**
- Fetched `default_theme.cpp`, Godot's `OpenSans_SemiBold.woff2` (converted with fontTools) and the slider grabber SVGs.
- Downloaded Montserrat (JulietaUla repo) and JetBrains Mono NL (release v2.304 zip; the raw URL had returned 404). OFL licences copied.
- Added gdx-freetype (core, Android natives per ABI, desktop natives).
- Wrote `Fonts`, `StyleBoxFlat`/`StyleBoxLine`, `UiKit`, `SettingsView`, `HudView`, `PauseView`, `MainMenuScreen`, `OptionsScreen`, `GameScreen`, `KeyboardInput`, `TouchStick`, `AudioBuses`, `MusicDirector`. Removed `MenuBackdropScreen`.
- Dev start screens: `tutorial`, `options`, `paused`, `game`.
- Found the Godot LabelSettings rule: when label_settings is set, its font size wins over the theme override, so the tutorial text is 16 px.

**Godot comparison:**
- Probe `v11_ui_screens` (windowed; it crashes on exit after saving, which is harmless).
- Fixed after comparing: tutorial blank line, Exit text, title shadow (stamped halo), +3 line spacing, VBox spacing, vertical separators, `Locale.ROOT` formatting.
- The Godot game-start render also shows the original's stuck pause panel (V1 confirmed visually).

**Emulator:**
- Full flow tested (audio on). Logcat errors were system-only.
- Mistakes: wrong tap coordinates first; a tap in the bottom gesture zone opened Gemini (risk D16).
- Added slot selection (keys 1–5, taps).

**Results:** 74 tests pass; the build is clean; `DELIVERY_D1.md` written. Status and plan updated (D15–D17).

**Not done:** the CRT shader (optional before Wednesday); physical device.

## 2026-10-15 — Session 8: merge to main

**User:** "merge into main".

- Fast-forwarded `main` to `d1-menu-and-start`, bringing in Phases 0, 2, 3, 4 and Delivery 1 (no merge commit needed: the branches are linear).
- The phase branches are kept for reference: `phase-0-godot-reference`, `phase-2-runtime-foundation`, `phase-3-data-and-rooms`, `phase-4-rooms-camera`, `d1-menu-and-start`.
- Verified on `main` after the merge: build and all tests pass.
- From now on, new work branches off `main`.

## 2026-10-15 — Session 9: questions answered, stopping for the week

**User:**
- Asked which GitHub repo we work on. **Answer: none.** The repo is local only (`git init` in session 3, no remote, nothing pushed); `gh` is not installed. The original folder name suggests a GitHub ZIP download of `2026-Game_of_the_year`, but it has no `.git` data. If a remote is wanted later: ask for the repo URL and visibility first (recommend private: the original team's code and assets, music rights R15). Push only after explicit confirmation.
- "stop for now. it's fine. Just log everything in our log for next week so you know where to continue from."

---

# HANDOFF — where to continue (written 2026-10-15)

## State
- `main` (`ddd3d62` + this log commit) contains everything: audit, Phases 0–4 and **Delivery 1 (ready for Wednesday 2026-10-21 morning)**. Clean build; **74 tests pass**.
- Phase branches are kept for reference (not needed): `phase-0-godot-reference`, `phase-2-runtime-foundation`, `phase-3-data-and-rooms`, `phase-4-rooms-camera`, `d1-menu-and-start`. **New work: branch off `main`.**
- Delivery documents: `DELIVERY_D1.md` (hand-over note), `MIGRATION_STATUS.md` (status, tests, differences register D1–D17, decisions), `MIGRATION_PLAN.md` (audit, phases, §10 delivery schedule).

## Open questions for the user
1. Port the "Cool thingo" CRT shader before Wednesday, or start D2? (Asked; not answered.)
2. GitHub remote: none so far; only if the user asks.
3. Delete the old phase branches? (Offered; not answered.)

## Next work, in priority order
1. *(Optional, before Wed 2026-10-21)* **CRT shader** (`shader_goesbrrrr.gdshader` → GLSL ES on a FrameBuffer; menu only). Notes: use texture2D (no textureLod), `highp` for the hash noise, check the FRAGCOORD origin, keep the scene's parameter values (curvature 0.0, etc.).
2. **D2 (Wed 2026-11-04): rest of Phase 5 + Phase 6.**
   - Dash NONE mini-slide, SHORT, MEDIUM (with the **D-B2 fix**: `can_take_dmg = true` when the slide ends, without the blue tint), LONG (teleport to the cursor; on Android the right-half "cursor" touch, Q1).
   - Ghost trail (every 0.15 s, fade 0.75 s, grey modulate).
   - Parries NORMAL/MEDIUM/CHARGE (B5–B7 rules).
   - Invulnerability flicker, `take_dmg`.
   - Golden traces already exist in `tools/godot-reference/results/v9_player_traces.json` (dash scenarios incl. dash into walls with/without clamp, NONE mini-slide, MEDIUM, parries, error paths).
   - **Input caveat (V15):** in those traces an edge action pressed at tick t is seen at tick t+1 (`action_press` semantics); held keys are seen at tick t.
   - Extend `PlayerTraceTest` to the dash/parry scenarios.
   - Dash velocity changes depend on `move_and_slide` velocity corrections (floor/ceiling/wall rules); these are already ported in `KinematicBody`.
3. Then, as in plan §10:
   - D3: weapons, inventory, interaction, HUD ammo; basic enemies with navigation (port A*/funnel with search limits; golden paths in `v8_navigation.json`).
   - D4: ghost, spawner, King Rotta; level generation (+ verify Godot's Area2D result for exactly touching ModuleBoxes; `Rect.overlaps` is strict for now).
   - D5: upgrades, consumables, death/win, remaining menus and audio.
   - D6: Android hardening + validation on a physical phone.

## How to work (environment)
- Gradle: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, then `./gradlew :logic:test :core:test :app:assembleDebug :desktop:installDist --warning-mode all`.
- **Desktop run:**
  - `./gradlew :desktop:run [-Psos.screen=menu|game|tutorial|options|paused|rooms] [-Psos.room=ID] [-Psos.window=WxH]`
  - screenshot: `-Psos.screenshot=/abs/path.png -Psos.screenshotFrame=N` (the 960×540 window = 1920×1080 back buffer on this Retina Mac)
  - room capture: `-Psos.captureRooms=/dir`, then `python3 -I tools/compare_room_screens.py /dir`
- **Emulator:**
  - start: `~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_37.0 -no-window -no-boot-anim -no-snapshot-load -no-snapshot-save` (run in background; never touch the user's Quick Boot snapshot)
  - then: `adb wait-for-device`; wake with `adb shell input keyevent KEYCODE_WAKEUP; adb shell svc power stayon true`
  - dev start: `adb shell am start -n ro.klooseduard.streetsofsibiu/.AndroidLauncher -e screen game|rooms`
  - screenshot: `adb exec-out screencap -p > file.png`
  - geometry: the view is 1920×1080 at x offset 240 on the 2400×1080 screen
  - avoid taps in the bottom ~84 px (system gestures)
  - stop with `adb emu kill`
- **Godot reference (4.6.1, never touch the original):**
  - `tools/godot-reference/setup_copy.sh` recreates `project-copy/`
  - `tools/godot-reference/run_probe.sh <probe> [--windowed]`
  - probes in `tools/godot-reference/probes/`, results in `results/`, MIT engine sources in `engine-source/` (the porting reference: read the source before porting a behaviour)
- Original Godot project: `2026-Game_of_the_year-main/newgame/`, read-only. Check after each session with `find 2026-Game_of_the_year-main -newer 2026-Game_of_the_year-main.zip` (must print nothing).

## Gotchas learned (do not repeat)
- **zsh:** an unquoted glob with no match aborts the whole command line (`--include='*.kt'`, `rm dir/*.png`). Quote it or use `find -delete`.
- **Kotlin:** `/*` inside KDoc opens a nested comment (e.g. "enemy_types/*.tres").
- **Android Studio "Configure Kotlin in Project" breaks the build** (duplicate `kotlin("jvm")`, `jvmToolchain(8)`). Revert `build.gradle.kts`/`settings.gradle.kts` if it reappears (ask the user first).
- **Godot probes:**
  - new `class_name` scripts need an editor import → extend `"res://probes/probe_base.gd"` by path;
  - the headless root viewport is 64×64 → use a 1920×1080 SubViewport;
  - a SubViewport defaults to **linear** filtering → set Nearest;
  - wait for the navigation map sync (`map_get_iteration_id`);
  - windowed probes may crash at exit after saving (harmless).
- `String.format` uses the Mac's locale (comma decimals) → always `Locale.ROOT`.
- The emulator sleeps, giving black screenshots → wake + `svc power stayon true`.
- **Black screen in Android Studio's Device Manager (reported 2026-10-15):** the AVD uses Quick Boot (`fastboot.forceFastBoot=yes`) and resumed a `default_boot` snapshot after my headless SwiftShader runs. The emulator log showed `Failed to find ColorBuffer` / `invalid color buffer`. Fix for the user: power button → **Cold Boot Now** → if needed, switch Graphics Hardware/Software. **My runs must use `-no-snapshot-load -no-snapshot-save`** and be stopped (`adb emu kill`) before the user opens the emulator.
- When transcribing from `.tscn`, do not filter out blank lines: multi-line strings contain them (the tutorial text bug).

## Constraints recap
- Deliver every two weeks (D1 Wed 2026-10-21; then 11-04, 11-18, 12-02, 12-16, 2027-01-13 per plan §10, to confirm).
- No Android phone (Apple devices only): emulator + desktop; physical-device validation deferred (D11).
- Decisions in force: Q1–Q9, B1/B2/B11/B18 fixed; O1 fonts kept (Montserrat etc.); O2 music centred (D13).
- Log every session here; keep `MIGRATION_STATUS.md` in sync.

## 2026-10-15 — Session 10: black screen in Device Manager (diagnosed)

**User:** the virtual phone shows a black screen when started from Android Studio's Device Manager.

**Diagnosis (via adb on the running instance; Android Studio had launched it with `-qt-hide-window`, embedded in Running Devices):**
1. The phone had booted (`sys.boot_completed=1`) but the **display was off** (`mWakefulness=Asleep`, screen OFF).
2. After waking, Android still drew black. Quick Boot had restored a `default_boot` snapshot from 16:11 (uptime 31 min), with an **old build of our app (Phase 1 era) in the foreground**. That app's GL surface did not render after the snapshot restore.

**Fix applied:**
- Woke the screen.
- Force-stopped and restarted the app: it renders.
- Installed the current build (`:app:installDebug` from `main`): the Delivery 1 menu shows.

**Verified:**
- Screen off → on with today's build pauses and resumes correctly (`AndroidGraphics: paused/resumed`, frame redrawn). **Not a game bug.**
- The emulator-only cause is the snapshot restore and screen sleep.

**For the user:**
- If it happens again: press the emulator's power button. If the game is still black, restart it, or use **Cold Boot Now**.
- After rebuilding, press Run (or `./gradlew :app:installDebug`) so the emulator has the current build. The Quick Boot snapshot may otherwise bring back an old one.

## 2026-10-20 — Session 11: progress summary for the teacher

**User:** "check our progress and explain me what we have done until now. I have to explain it tomorrow to my teacher."

- Re-read the plan, status, delivery note and log; no code changes since 2026-10-15.
- Re-ran `:logic:test :core:test :app:assembleDebug`: build OK, **74 tests, 0 failures**. Original project still untouched.
- Gave the user a plain-language summary of Phases 0–4 and Delivery 1 for the presentation on Wed 2026-10-21.
- **User:** "make a short slide deck for tomorrow". Made a 9-slide deck with speaker notes: project, method, progress table, Delivery 1 features, key numbers, decisions, roadmap D2–D6, demo. Placeholder left: `[Your name]` on the cover.

## 2026-10-20 — Session 12: code comments removed (branch `remove-code-comments`)

**User (in Romanian):** write the progress so far in Romanian for the teacher, and "I would prefer you remove the comments from the code."

**Done:**
- Branch `remove-code-comments` from `main`.
- Removed all comments (line, block, KDoc; Python `#` comments and docstrings; GDScript `#`/`##`; shell comments except the shebang) from our own code: 61 Kotlin files, 6 Gradle scripts, 3 Python tools, 10 Godot probes, 2 shell scripts. Used a string-aware stripper (scratchpad, not committed), so `"res://…"` and raw strings were not touched.
- `tools/convert_rooms.py` no longer emits comments into `GeneratedRooms.kt`; regenerating gives a byte-identical file.
- Not touched: the original Godot project, the Godot engine sources (MIT licence headers), the Gradle wrapper (`gradlew`, `gradlew.bat`), config files (XML, `.properties`, `.toml`, keep rules, `.gitignore`) and the Markdown documents.

**Checks:**
- Every remaining line is an original line, at most with a trailing comment cut off; every deleted line was a comment, docstring or blank.
- Python files parse; `bash -n` OK on the shell scripts; Godot 4.6.1 `--check-only` OK on all 10 probes (sanity-checked that it does catch a broken script).
- Clean build `:logic:test :core:test :app:assembleDebug :desktop:installDist --warning-mode all`: OK, **74 tests, 0 failures**. Original project untouched.

**Note for future sessions:** the code has no comments now, by the user's choice. The explanations (Godot source references, why a behaviour is ported a certain way) remain in this log, `MIGRATION_PLAN.md`, `MIGRATION_STATUS.md`, `tools/godot-reference/README.md` and the git history (`main` before this branch). Do not add comments to new code unless the user asks.

## 2026-11-06 — Session 13: Delivery 2 (branch `d2-dash-parry-ghost-trail`)

**User:** continue with the next implementation, new branch from main, follow the plan.

- Asked about the unmerged `remove-code-comments` branch; user chose "merge first". Merged it into `main` (`782d5cb`), branched `d2-dash-parry-ghost-trail`, no comments in new code.
- Plan §10: D2 = rest of Phase 5 + Phase 6 (dash, parry, ghost trail, touch controls). Implemented `Player` per `player.gd` (see `DELIVERY_D2.md`).
- Found by the dash traces: Godot's SAT gives **two contacts** for horizontal-normal face contacts (edge–edge `_generate_contacts_edge_edge`); ported. Fetched `godot_collision_solver_2d_sat.cpp` (4.6.1-stable, MIT) into `tools/godot-reference/engine-source/modules/godot_physics_2d/`.
- Trace test details: the probe applies the clamp *before* the snapshot; edge-triggered timer states (`can_take_dmg` after the 0.2 s parry timer) are compared with ±1 tick because Godot's idle deltas jitter.
- Open: D18 (dash teleported into a 2-tile wall next to a Preventer ends up to 30 px from Godot's position; cause unconfirmed).
- Emulator: started with `-no-snapshot-load -no-snapshot-save`, stopped with `adb emu kill`. A "System UI isn't responding" dialog appeared (software rendering, not the game); dismissed with Wait.
- Results: `:logic:test :core:test :app:assembleDebug :desktop:installDist` OK, 101 tests, 0 failures; original project untouched.
- Next: D3 (weapons, bullets, inventory, interaction, HUD ammo; basic enemies with navigation), due 2026-11-19.

## 2026-11-06 — Session 14: history rewritten to remove tool attribution

- On request, rewrote all branches: removed the co-author and session trailers from every commit message and the artifact link from `MIGRATION_LOG.md` (all revisions). All commit hashes changed; content is otherwise identical.
- Re-checked messages and file contents of every revision: no match. Authors and committers are only `klooseduard95`.
- From now on commits and pull requests carry no attribution lines.

## 2026-11-06 — Session 15: Delivery 3 started (branch `d3-weapons-inventory-enemies`), milestone 1: weapons

**User:** start Delivery 3 on a new branch; never write any co-author or tool reference (saved to memory).

- Branch from `main`. Plan §10 D3 = Phases 7 and 8 (weapons, bullets, inventory, interaction, HUD; enemies with navigation).
- Done in `:logic`: `HeldWeapon` (shoot, ammo, reload, crit, damage formula, chainsaw ramp with real-time decay, sniper empties the magazine), `Bullet` + `BulletSystem` (movement, range, wall/hitbox/enemy-body rules from `bullet.gd`, hit detection at the start of the next physics tick like Godot's Area2D signals; capsule r11/h32 along travel, r36/h92 perpendicular for melee), `Inventory` (slots, equip/unequip modifiers, `add_item`, `swap_item`; B11 fix: fresh instances; B10 preserved), `Player` hooks (shoot while held, reload on press, aim at the cursor).
- Done in `:core`: left mouse / right-half touch hold = shoot, bullet drawing (melee shows the weapon icon), HUD inventory icons and ammo label states ("Ammo: a / b", "∞", "Reloading..."), R button on Android, weapon sounds. Dev switch `-Psos.items=primary_medium,Knife` (desktop) / `-e items ...` (Android) since pickups do not exist yet; the screenshot harness can script touches (`100:touch:800:270,111:untouch:800:270`).
- Tests: 17 new weapon/inventory/bullet tests; crit rate measured 20.1 % for the sniper (expected 20 %).
- Not done yet in D3: ground items and interaction (E), consumables activation, enemies (stats, AI, navigation, projectiles, health bars), enemy hitboxes as `BulletTarget`.

## 2026-11-06 — Session 16: Delivery 3, milestone 2: ground items, interaction, consumables

**User:** continue with ground items and interaction.

- Ported `interactable.gd`, `interacting_component.gd` and `item_placeholder.gd` (GameItem) to `:logic` (`Interaction.kt`):
  - the interact range is a circle of r 45 (30 × 1.5) against the item's Interactable circle of r 95, so an item is offered up to 140 px away;
  - overlaps are evaluated at the start of the physics tick (area signals); the list keeps entry order and is sorted by distance every idle frame;
  - E acts on the first entry; the label keeps its previous text if the first entry is not interactable;
  - picking up: `add_item` into the first free matching slot (the ground item is freed), otherwise `swap_item` with the selected slot: the old item stays on the ground as "Switch with X". B11: fresh instances.
- Consumables (`consumable_item.gd`): used on the press, Health only below max HP (capped by the `max_hp` stat), negative HP applies directly (stun grenade), shield, timed buffs and the Adrenaline after-effect; potion sound; the slot empties. `slown_enemy` is tracked on the player (`slownEnemy`) for the enemies.
- `ItemDef` interface for weapons and consumables.
- Core: ground item sprites at native size, label 42 px below the player (default font 16), E key and an E button on Android, dev switch `-Psos.ground="primary_sniper@120,0;consumable_health@-300,0"` (desktop) / `-e ground ...` (Android), offsets from the spawn-room centre. Ground items are cleared when a level is generated (B12), dev items are spawned after that.
- Tests: 17 new (interaction range and label, nearest first, pick up, swap rules, every consumable). Screenshots checked on desktop: label "Pick up Sniper Rifle", pick-up puts the sniper in slot 1 with "Ammo: 1 / 1".
- Still open in D3: enemies (Phase 8) and enemy hitboxes as bullet targets.

## 2026-11-06 — Session 17: Delivery 3, milestone 3: navigation (enemy pathfinding)

**User:** start on the enemies. Enemies need `NavigationAgent2D`, which was not ported yet (plan E4), so navigation came first.

- Fetched the missing Godot 4.6.1 sources (MIT) into `tools/godot-reference/engine-source/`: `servers/nav_heap.h`, `modules/navigation_2d/2d/nav_map_builder_2d.cpp`, `nav_region_builder_2d.cpp`, `nav_mesh_queries_2d.h`, `nav_base_iteration_2d.h`, `nav_utils_2d.h`, `triangle2.cpp`, `nav_agent_2d.cpp`, `nav_region_2d.cpp`.
- `logic/nav`:
  - `NavMesh`: one square polygon per walkable cell (tile nav polygon is the full tile, vertex order (+,+), (−,+), (−,−), (+,−)); neighbours linked through shared edges in the order Godot's map builder inserts them.
  - `NavQuery`: Godot's A* with its heap (`nav_heap.h`), f/h-cost tie-break, `path_search_max_polygons` / `path_search_max_distance` (partial path to the closest reachable polygon), corridor funnel and `_query_task_clip_path` (this is why paths contain a point at every portal crossing). Costs are rounded to float32, which Godot uses.
  - `NavAgent`: `NavigationAgent2D` target/waypoint logic (path_desired 20, target_desired 10, path_max_distance 100).
  - `RoomNav.mesh(room)`.
- **The A* tie-breaks depend on the order of the regions** (one region per tile). The order that reproduces Godot is the order of the cells in the `.tscn` `tile_map_data`, not row-major. `tools/convert_rooms.py` now also emits `order = "x,y,…"` for each room (`TileGrid.cellOrder`); regenerating changed only those 20 lines of `GeneratedRooms.kt`.
- Results against Godot: **120 of 120** recorded paths (limited to 500 px and unlimited, 5 rooms) identical, worst deviation 0.001 px; walkable tile counts equal; both recorded chase traces (agent + floating `move_and_slide`) match within 0.0002 px, including the `is_navigation_finished` flag.
- Next: enemy data (stats, scaling, size roll), `BasicEnemy` AI (agro tick, line of sight, attack loop, projectiles), health bar, death, hitboxes as bullet targets, spawning from the room spawn nodes, rendering and sounds.

## 2026-11-06 — Session 18: Delivery 3, milestone 4: basic enemies

**User:** continue with the enemies (after navigation).

- `:logic` `Enemy` + `EnemySystem` (`Enemy.kt`, `EnemySystem.kt`), from `IEnemy.gd`, `enemy.gd`, `EnemyHitbox.gd`, `enemy_projectile.gd`, `EnemySpawnNode.gd`:
  - Stats with per-level scaling (rat hp +3 dmg +1, pigeon +2/+1, thief and criminal +3/+1.5) and the size roll (10 % stay, 75 % → 1.25, 15 % → 1.5; a 1.25 then has a 15 % chance of 1.5; hp and damage bonuses as in the script). The random calls are made in the original order.
  - AI: 1 s tick, aggro circle (vision × 10, scaled), line of sight as a ray that walls, other enemies and the player block; `agro += (11 − agro) / 3` gives 0 → 3 → 5 → 7 → 8 → 9, −1 per tick without sight, idle at 0; path target is set while agro > 0; velocity = direction to the next path point × walkSpeed × `slownEnemy`; floating `move_and_slide` with a circle collider of r 10 × size.
  - Attack loop: the hit range is an area (r = reach × size) that fires when the player enters; it is disabled (deferred) during windup and cooldown and re-enabled afterwards, so attacks repeat while the player stays in reach (V6). The projectile is spawned after the windup at the player's position at that moment.
  - Projectiles: speed in px per tick, life timer, piercing rules, damage to the player with a 0.1 s flicker, walls free non-piercing ones; the rectangle shape is shared, so every live projectile uses the size of the latest spawned one (E12).
  - Sprite animation: the 15-tick frame logic per type, including the `!row` quirk for thief and criminal.
  - Health bar hidden until the first damage, tween 0.4 s; death: count −1, damage and speed 0, 0.3 s fade, removal. Hitbox (circle r 27) is hit first by bullets, the body (circle r 10) blocks ranged bullets and not melee ones.
  - `EnemySystem.spawnFromNode` follows the level spawn tables (ghost and marshmallow are skipped until Phase 9).
- `StaticWorld` now supports dynamic round obstacles (the player capsule and enemy circles) so enemies and the player collide with each other; boxes gained an inflate radius for rounded shapes.
- `:core`: enemies, health bars and projectiles are drawn; enemy sounds (Rat, Pigeon, Grunt, 30 % CeTelefonAi) play non-positionally; dev switch `-Psos.enemies="rat@300,0;pigeon@-300,100"` (desktop) / `-e enemies ...` (Android), offsets from the spawn-room centre.
- Tests: 17 new enemy tests (scaling, χ² of the size roll over 20 000 spawns: 0.48 against a critical value of 13.8, agro sequence, line of sight, chase, attack loop, projectile rules, death, collision with the player, spawn tables). Screenshot on desktop shows all four types chasing and hurting the player.
- Not done in D3: enemy hitbox/body hits for chainsaw ramp are wired through `BulletTarget`; the spawn of enemies from rooms during level generation belongs to Phase 10 (D4).
