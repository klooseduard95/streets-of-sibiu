# Migration Status — Streets of Sibiu (Godot → Kotlin/libGDX/Android)

Plan: `MIGRATION_PLAN.md` (audit approved 2026-10-15; all §9 recommendations accepted).

**Update 2026-11-06: Delivery 2 (dash, parry, ghost trail, damage; branch `d2-dash-parry-ghost-trail`) is implemented — see `DELIVERY_D2.md`. 101 tests, 0 failures.**

**Overall state (2026-10-15): Phases 0–4 are done, and Delivery 1 (menu + start of the game) is ready (see `DELIVERY_D1.md`). Physical-device testing is deferred (no Android phone available; the emulator and desktop are used instead). The migration is NOT complete.** No gameplay has been ported yet.

Next delivery: **D1 on Wednesday 2026-10-21, morning** (your deadline): the main menu and the start of the game. Scope in plan §10.

| Phase | State |
|---|---|
| 0 — Decisions and Godot reference data | **Done** (2026-10-15): Godot 4.6.1 installed, V1–V15 answered, golden data recorded |
| 1 — Minimal libGDX project builds and runs | **Done** on desktop and emulator; physical device **deferred** |
| 2 — Runtime foundation (`:logic`) | **Done** (2026-10-15): 39 new unit tests, all passing |
| 3 — Data and rooms | **Done** (2026-10-15): 20 rooms converted, all game data transcribed, 24 parity tests passing |
| 4 — Room rendering and camera | **Done** (2026-10-15): 34/40 reference views pixel-identical to Godot, the other 6 differ only by sub-pixel texel selection (D14); runs on desktop and emulator |
| D1 (Phase 5 walking subset + Phase 12 menu subset) | **Ready** (2026-10-15): menu, tutorial, options, centred music, level-1 start, walking verified against Godot, HUD, pause |
| 5 (rest), 6–14 | Not started |

---

## Decisions (approved 2026-10-15)

| ID | Decision |
|---|---|
| Q1 | Left 8-way virtual stick; right half of the screen = mouse cursor (hold = shoot); buttons for Dash, Parry, Interact, Reload, Pause; tap HUD slots. Keyboard/mouse also supported |
| Q2 | FitViewport 1920×1080 (letterbox/pillarbox) |
| Q3 | Preserve every original behaviour except B1 (port the intended pause/death behaviour), B11 (fresh item instances per run), B18 (single death screen) and **B2 (fix the Medium Dash invulnerability; decided 2026-10-15, see D-B2)** |
| Q4 | Port the intended pause/death behaviour |
| Q5 | Desktop LWJGL3 dev module (not shipped) |
| Q6 | Bundle Montserrat, JetBrains Mono (NL), Hack and the Godot default font |
| Q7 | Launcher icon derived from the player sprite (front-facing frame) |
| Q8 | Modules `:logic` / `:core` / `:app` / `:desktop` |
| Q9 | Install Godot 4.6 and run probes in a copy of the project (done with 4.6.1) |
| Later decisions | B2 fixed (D-B2); git initialised; physical-device testing deferred (2026-10-15) |

---

## Delivery 1 — completed tasks (2026-10-15, branch `d1-menu-and-start`)

* **Physics port** (`logic/physics`): Godot 4.6.1 `test_body_motion` for vertical capsule/circle bodies vs axis-aligned boxes:
  * recovery, 4 passes at 40 %, min depth = margin × 0.05;
  * an exact time of impact fed through Godot's 8-step bisection;
  * the rest contact.
  * Plus `move_and_slide` grounded/floating with cancel sliding and the floor snap.
* **Walking:**
  * `Player` (8-way input, speed, walk animations) and `FrameAnimator` (AnimationPlayer timing: key 0 on `play`, idle-time advance, reset on `stop`).
  * `RoomCollision` (solid tiles + door Preventers).
  * The room clamp in `GameScreen`.
* **UI:**
  * Fonts via gdx-freetype: Godot's own Open Sans SemiBold (converted from the 4.6.1 sources), Montserrat, JetBrains Mono NL (OFL licences in `assets/fonts`).
  * Godot `StyleBoxFlat`/`StyleBoxLine` drawables, default theme values (`default_theme.cpp`), the exact default slider grabber.
  * `MainMenuScreen` + tutorial, `OptionsScreen`/`SettingsView`, `HudView`, `PauseView` (intended `pause_menu.gd`, D-B1), `GameScreen` (level-1 start sequence, camera, black screen under the HUD, pause, Android lifecycle → pause).
* **Audio:** `AudioBuses` (original dB values), `MusicDirector` (looping, centred music D13, horse 1/250 per second).
* **Input:**
  * keyboard (project.godot map; P/Esc/Back → pause; 1–5 → slot)
  * `TouchStick` (8-way, left half)
  * slot taps
  * on-screen pause button on Android
* **Godot reference renders** (`v11_ui_screens`, windowed): menu, tutorial, options and game start, used for side-by-side comparison.
* **Delivery note:** `DELIVERY_D1.md`.

## Delivery 1 — test results (2026-10-15)

| Check | Result |
|---|---|
| `./gradlew :logic:test :core:test` | **74 tests, 0 failures** (adds `PlayerWalkTraceTest`, `PlayerTraceTest`) |
| Walking vs Godot (9 recorded scenarios: walls, corners, pillars, diagonal slides, stop, room clamp) | **Max position error 0.073 px at any tick (most ≤ 0.01 px); sprite frame identical at every tick** |
| Layout vs Godot renders (menu, tutorial, options, game start) | Close match after fixes; remaining small offsets (tutorial Start button ≈ 10 px). Game start: player (996, 545) and camera centre (996, 544.5), identical to Godot |
| Build (`--warning-mode all`) | Successful, no warnings; APK contains libgdx + libgdx-freetype for 4 ABIs and the fonts |
| Emulator (API 37, audio on) | Menu → tutorial → game → walk (touch stick) → pause (Back) → Main Menu → Options → slider → Go Back; slot tap; no crash. MP3 decoder created (music playing). Logcat errors only from the system (SurfaceSyncGroup at startup, codec resource query) |

### Delivery 1 — mistakes found and fixed on the way
* Comparing with the Godot renders found **real transcription errors**:
  * the tutorial text has an empty line after "TUTORIAL" (my audit printout had filtered out blank lines);
  * Godot centres "Exit\n";
  * the title shadow is a 17 px halo;
  * labels have +3 px line spacing.
* **Decimal formatting** used the Mac's locale ("1,00X"); now `Locale.ROOT` ("1.00X", as in the original).
* My separator drawable could only draw horizontal lines (the inventory/pause dividers were missing). Fixed.
* Emulator taps: my first pause-menu tap coordinates were wrong. Also, a tap in Android's bottom gesture zone opened the system assistant (recorded as risk D16).

### Delivery 1 — known gaps
See `DELIVERY_D1.md`:
* no level generation, enemies or combat yet;
* the CRT effect is not ported;
* not tested on a physical phone.

---

## Phase 4 — completed tasks (2026-10-15)

* **`CameraRig`** (`logic/world`): port of Godot 4.6.1 `Camera2D::get_camera_transform` for the game's settings (drag centre, zoom 1, smoothing 5, smoothed limits, physics callback):
  * limits applied before smoothing
  * centring when the room is smaller than the screen
  * `reset_smoothing` = one update, then snap
  * integer limits from the module box, truncated toward zero
  * float32 arithmetic
* **`:core` rendering:**
  * `GodotView`: y-down camera, so world coordinates are identical to Godot's; FitViewport 1920×1080; black bars; Godot's default grey clear colour (0.3) inside the view.
  * `PaddedTileAtlas`: Godot's `use_texture_padding`, a 1 px duplicated border per tile, so there are no seams at sub-pixel camera positions.
  * `RoomRenderer`: culled tile drawing.
  * `LoopScreen`: a screen driven by `GameLoop`, the first use of the Phase 2 loop in the app.
* **`RoomViewerScreen`** (development/demo):
  * all 20 rooms with the original camera
  * keyboard: arrows/WASD move, N/P switch rooms
  * touch: drag to pan, tap an edge to switch rooms
  * Desktop: `./gradlew :desktop:run -Psos.screen=rooms [-Psos.room=ID]`.
  * Android: `adb shell am start -n ro.klooseduard.streetsofsibiu/.AndroidLauncher -e screen rooms`.
* **Pixel comparison tooling:**
  * Godot probe `v10_room_screens` (windowed) writes 40 reference renders (20 rooms × centre/corner views) to `tools/godot-reference/results/room_screens/`.
  * `RoomCaptureHarness` (`-Psos.captureRooms=<dir>`) writes the same 40 views from the port.
  * `tools/compare_room_screens.py` compares them.

## Phase 4 — test results (2026-10-15)

| Check | Result |
|---|---|
| `./gradlew :logic:test :core:test` | **72 tests, 0 failures** (adds `CameraRigTest`, 4 tests) |
| `CameraRigTest` vs Godot recordings (`v4_camera.json`) | Limit cases, **70 ticks of smoothing** and the reset all match within 1e-3; integer room limits match V13 |
| Pixel comparison, 40 views (`compare_room_screens.py`) | **34/40 pixel-identical.** The 6 that differ are the centre views of the 5 boss rooms and `FIGHTROOM_34_BIG`. In all of them the camera's x lands on a half pixel (centre x = …​.5). ~96 % of the differing pixels are exact one-pixel horizontal texel shifts; the layout is identical (D14) |
| Build (`--warning-mode all`) | Successful, no warnings |
| Emulator (API 37, 2400×1080) | Room viewer runs: spawn room pillarboxed, tap-edge room switching and drag panning work, 0 logcat errors |

### Phase 4 — mistakes found on the way (logged for context)
* My first Godot reference renders used a `SubViewport`, which defaults to **linear** filtering (the game's root viewport uses the project's Nearest). Fixed in the probe.
* My viewer snapped rooms with `enterTree()`, which skips the limits. Godot uses `reset_smoothing()`, and that only shows on the next update. Fixed in the viewer and the capture harness.
* Atlas bleeding at half-pixel positions. Fixed with Godot's texture padding (`PaddedTileAtlas`).
* My own extra camera test expected exactly 544.5. Float32 smoothing stalls at 544.50037, the same as Godot's arithmetic, so the test now uses a tolerance.
* The emulator screen had gone to sleep, which gave black screenshots. Woke it and set `svc power stayon true`.

---

## Phase 3 — completed tasks (2026-10-15)

* **Room converter** `tools/convert_rooms.py`: reads the 20 room modules and `GenerationNode.tscn` (read-only) and generates `logic/.../data/rooms/GeneratedRooms.kt`. It includes the tile grids, doors with their transforms and the derived teleporter, preventer and arrival geometry, the module boxes, and the spawn nodes. Floats are rounded to Godot's float32. Output is deterministic (re-running reproduces the file byte for byte).
* **Room model** (`data/rooms/RoomModel.kt`, `RoomCatalog.kt`): `RoomDef`, `TileGrid`, `DoorDef`, `SpawnNodeDef`, the shared `RoomTileSet` semantics and the room pools from `GlobalUtils.gd`.
* **Game data** (`data/`), transcribed by hand:
  * `Enemies.kt`: 7 enemy stat blocks, looks, level scaling, the 5 attack kinds and their parameters.
  * `Items.kt`: 8 weapons, 8 consumables.
  * `Upgrades.kt`: 11 card pools (37 cards), the reward-pool rules, dash and parry types.
  * `LevelRules.kt`: generation depth, pools per depth, barricade cells, exit and room rewards, timings, spawn tables.
  * `PlayerDefs.kt`: constants, cooldowns, capsule, animations, starting inventory, slots.
  * `Stats.kt`: stat names.
* **Math types** `Vec2` and `Rect` (Godot float32 precision).
* **Parity tests** (logic test sources) use their own Godot text parser (`parity/GodotText.kt`), written independently of the Python converter:
  * `RoomParityTest` (9 tests): every one of the **10,565 tiles**; 40 tile definitions × 20 rooms; node structure; **46 doors** (orientation, transform, geometry recomputed in Kotlin); module boxes; **47 spawn nodes** (all on walkable tiles, radius 100); barricade tile.
  * `GameDataParityTest` (15 tests): weapons, consumables and enemy stats field by field against the `.tres` files and the script defaults; looks and level scaling against `IEnemy.gd`; attack call sites against `enemy.gd`/`KingRotta.gd`/`SupportEnemy.gd`/`enemy_projectile.gd`; all 37 upgrade cards and the show rules against `upgrade_window.gd`; pools against `GlobalUtils.gd`; depth and pools against `main_background.gd`/`Module.gd`; exit rewards against `Module.gd`; spawn tables against `EnemySpawnNode.gd`; barricades against `Barricade.gd`; player constants, animations and inventory against `player.gd`, the player scene, `ghost.gd` and `inventory_component.gd`; every referenced asset exists in `assets/`.
* **Mutation check:** I changed one weapon value and one generated tile. The matching tests failed (`weaponsMatchTheirResources`, `everyTileMatchesTheOriginal`), and the original data was then restored.

### Phase 3 — findings
* The parity test caught a **real transcription error**: the Adrenaline description has an empty line before "After Effect:". Fixed.
* There are **20** room modules, not 21 (the 21st decoded scene, `DummyRoom`, is unused). The plan is corrected.
* `GlobalUtils.roomSceneList` is declared but never read by the original code (kept for completeness).
* The `small_consumable` reward pool exists in `upgrade_window.gd` but is never requested.

## Phase 3 — test results (2026-10-15)

`./gradlew :logic:test`: **66 tests, 0 failures** (3 settings + 39 runtime + 24 parity). Full build (`:core:test :app:assembleDebug :desktop:installDist`, `--warning-mode all`): successful, no warnings. Original project untouched.

### Incident during Phase 3
At 16:07 the root `build.gradle.kts` and `settings.gradle.kts` were changed outside this session, most likely by Android Studio's "Configure Kotlin in Project" prompt. The change added a duplicate `kotlin("jvm")` plugin, `jvmToolchain(8)` and a test dependency, and broke every Gradle build. **With your approval both files were reverted** to the committed version. **If Android Studio offers to "Configure Kotlin" again, dismiss it**: Kotlin is already configured per module.

---

## Phase 2 — completed tasks (2026-10-15)

Package `ro.klooseduard.streetsofsibiu.logic.runtime` (pure Kotlin, no libGDX). Each part is ported from the Godot 4.6.1 source in `tools/godot-reference/engine-source/`:

| File | Godot equivalent | Behaviour kept |
|---|---|---|
| `GameLoop.kt` | `Main::iteration` | 60 ticks/s, max 8 per frame, excess dropped and subtracted from the idle delta; per tick: overlap callbacks → `_physics_process` → deferred flush → physics timers/tweens → delete queue → navigation sync → physics step; then one idle frame |
| `GameRuntime.kt` | `SceneTree`, `MessageQueue` | Pause flag, physics/process frame counters, scene timers, node timers, tweens, `call_deferred` (FIFO, nested calls run in the same flush), `queue_free` at the delete-queue flush |
| `Timers.kt` | `SceneTreeTimer`, `Timer` | Scene timers fire at `time_left <= 0` and skip timers created mid-pass; Timer nodes fire at `< 0`, add the wait time back when repeating, `stop()` sets −1, freeze when their owner can't process |
| `Tween.kt` | `Tween`, `PropertyTweener`, easing equations | Sequential/parallel steps, leftover delta carried, start value read at step start, trans/ease captured at append, `setLoops()` = forever, bound-node pause/detach/free rules, idle processing; LINEAR and SINE equations in 32-bit float like Godot |
| `SceneNode.kt` | `Node` lifecycle, `process_mode` | INHERIT/PAUSABLE/ALWAYS/DISABLED, detached subtrees (inactive rooms) and freed nodes |
| `GameRandom.kt` | `randi`, `randi_range`, `randf`, `Array.shuffle` | Inclusive ranges, reversed bounds, Godot's Fisher–Yates with `randi() % (i+1)`; seedable |
| `ActionInput.kt` | `Input` action state | Just-pressed in the next physics tick, once, even if released before it (V15); held state immediate |

**Not built yet (no consumer):** the `GameEvents` interface from the plan. It will be added in the first phase that needs to send events to `:core` (Phase 4/5), to avoid an unused abstraction. The loop is not wired into `:core` yet; Phase 4 will drive it from the room viewer.

## Phase 2 — test results (2026-10-15)

`./gradlew :logic:test`: **42 tests, 0 failures** (3 Phase 1 + 39 Phase 2). Full build (`:core:test :app:assembleDebug :desktop:installDist`, `--warning-mode all`): successful, no warnings.

| Suite | Tests | What it proves |
|---|---|---|
| `GameLoopTest` | 6 | 1 tick per frame at 60 Hz; 60 ticks/s at 120 Hz; 2 per frame at 30 Hz; cap of 8 with dropped excess and Godot's idle-delta correction; exact hook order; frame counters |
| `TimersTest` | 7 | Scene timers fire on the same frames as Godot's double arithmetic (0.1 s → frame 7, 0.2 → 13, 0.25 → 16, 1.0 → 60); pause vs process-always; timers created in a callback wait a frame; Timer node fires on frames 10/19/28 for 0.15 s; one-shot/stop; freeze when detached, disabled or paused; ALWAYS runs while paused |
| `TweenTest` | 9 | **88 easing samples match Godot's recorded `interpolate_value` output** (`v2_defaults.json`, tolerance 1e-7); leftover-delta carry; parallel + finished (death animation); infinite loop (boss bob); bound tween freezes when detached and dies when freed; pause rules; trans captured at append; empty tween discarded |
| `GameRandomTest` | 6 | Inclusive uniform ranges, reversed bounds, unit interval, 32-bit `randi`, shuffle permutations uniform, seeded reproducibility |
| `ActionInputTest` | 6 | V15 edge rules: next-tick visibility, press+release before the tick counts once, press inside a tick → next tick, no-tick frames on fast displays, held state, no repeat edge while held |
| `DeferredAndFreeTest` | 5 | Deferred FIFO and nested flush, deferred before the physics step, `queue_free` at end of tick, detached/freed subtrees, process-mode resolution |

---

## Phase 0 — completed tasks (2026-10-15)

* Installed **Godot 4.6.1-stable (official)** in `tools/godot-reference/engine/` (git-ignored). SHA-512 verified, Apple-signed (team 6K46PWY5DM).
* `setup_copy.sh` builds `project-copy/` from the original (git-ignored); `run_probe.sh` runs the probes. The original project stays untouched (checked: no file newer than the zip).
* Probes `v0_smoke`, `v1_runtime`, `v2_defaults`, `v4_camera`, `v6_areas`, `v8_navigation`, `v9_player_traces`; results in `tools/godot-reference/results/`.
* Relevant Godot 4.6.1 source files (MIT) in `tools/godot-reference/engine-source/` as the porting reference.
* All answers are summarised in `tools/godot-reference/README.md` and plan §8.3.

### Phase 0 — key findings

| Finding | Effect on the port |
|---|---|
| V1: pause menu and death screen scripts fail to parse; the original shows a permanently visible, dead pause panel and a broken death screen | Port the intended behaviour (D-B1, already approved) |
| V2: `spawnRadius` is 100 in every room | B16 closed |
| V3: idle sprite = first keyframe of the last walk animation | Phase 5 |
| V4: camera centres on the limits when the room is smaller than the screen; exponential smoothing at speed 5 | Phase 4 |
| V5: the music is panned left (the source sits at world origin) | Preserved by default; open question O2 |
| V6/V7: area re-entry fires on re-enable; hitbox before body | Phase 2 (overlap system), Phase 8 |
| V8: the 500 px navigation search limit truncates paths; golden paths recorded | Phase 8 |
| V9: 21 golden movement traces recorded (incl. dash through walls and the room clamp) | Phase 5/6 acceptance |
| V10: runtime errors abort the function (B7 confirmed) | Phase 6 |
| V11: the named fonts are not installed on typical machines, so the original effectively renders all text in **Open Sans SemiBold 16** | Open question O1 |
| V14/V15: tweens and timers in idle frames; input edges on the next physics tick | Phase 2 frame-order contract |

### Open questions for you

| ID | Question | Default until you decide |
|---|---|---|
| O1 | Fonts | **Answered 2026-10-15: "fine for now"** → keep Q6 (bundle the intended fonts); revisit later if needed |
| O2 | Music panning | **Answered 2026-10-15: fix → centred music** (D13) |
| O3 | Delivery dates | **Answered 2026-10-15:** first delivery **Wednesday 2026-10-21 morning**, "the start of the game and the menu"; later dates every two weeks |

---

## Phase 1 — completed tasks

* **Gradle:** modules `:logic` (pure Kotlin, no libGDX), `:core` (libGDX), `:app` (Android launcher, reuses the existing module and namespace `ro.klooseduard.streetsofsibiu`), `:desktop` (LWJGL3, dev only).
* **Versions:** libGDX 1.14.2, Kotlin 2.4.21, AGP 9.4.1 (unchanged), Gradle 9.6.0 (unchanged), JUnit 4.13.2. JVM modules target Java 11 bytecode.
* **Android native libraries:** a configuration-cache-compatible `copyAndroidNatives` Sync task extracts `libgdx.so` for armeabi-v7a, arm64-v8a, x86 and x86_64 into `app/build/gdx-natives/`, packaged as jniLibs.
* **Template clean-up (`:app`):** removed AppCompat, Material, core-ktx and Espresso, the template example tests, the template theme colours and the Android-logo launcher icons. New: framework fullscreen theme, `AndroidLauncher` (immersive, landscape `sensorLandscape`, `isGame`, GLES 2.0 required), and launcher icons generated by `tools/make_launcher_icons.py`.
* **Assets:** 57 runtime assets (43 PNG, 14 MP3) copied into `assets/` under their original relative paths. This folder serves as the Android assets directory and as the desktop working directory.
* **`MenuBackdropScreen`:** shows `menu.png` in a 1920×1080 FitViewport with Nearest filtering.
* **Desktop dev tooling:** the run task uses a JDK 21 toolchain (LWJGL warns about JNI on JDK 25). Options `-Psos.window=WxH` and `-Psos.screenshot=path` (screenshot harness for future screenshot comparisons).
* **Transitive dependencies (noted, not added by us):** `gdx-backend-android` pulls in `androidx.core:1.17.0`, which pulls androidx lifecycle and `kotlinx-coroutines`. The game code does not use coroutines; R8 strips unused code in release.

## Phase 1 — test results (2026-10-15)

Environment: macOS, Gradle JVM = Android Studio JBR 25.0.3 (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`).

| Acceptance criterion | Command / method | Result |
|---|---|---|
| Debug APK builds | `./gradlew clean` then `./gradlew :logic:test :core:test :app:assembleDebug :desktop:installDist` | **PASS** — BUILD SUCCESSFUL. No Gradle deprecation warnings from our scripts (`--warning-mode all`); configuration cache reused. Only note: "Unable to strip libgdx.so" (no NDK installed; harmless) |
| `:logic:test` runs ≥ 1 test | `GodotProjectSettingsParityTest` (parses the original `project.godot`) | **PASS** — 3 tests, 0 failures |
| Asset MD5 test | `AssetIntegrityTest` in `:core:test` | **PASS** — 2 tests, 0 failures: the 57 expected assets are present and byte-identical to the originals |
| Desktop opens the same screen | `./gradlew :desktop:run -Psos.screenshot=…` (960×540 window = 1920×1080 Retina back buffer) | **PASS** — screenshot is **pixel-identical** to `menu.png` (0 differing pixels). 1200×900 window: letterbox bars of 225 px top/bottom, as expected |
| APK installs and launches on the emulator | `adb install -r`, `am start -W` on AVD `Medium_Phone_API_37.0` (API 37, arm64, 2400×1080) | **PASS** — cold start 4.65 s, activity resumed, landscape, immersive |
| Viewport and letterbox per Q2 | `adb exec-out screencap` | **PASS** — content 1920×1080 with 240 px pillarbox bars; content area **pixel-identical** to `menu.png` |
| Logcat has no errors | `adb logcat --pid=<app> '*:E'` | **PASS** — 0 error lines. Only emulator graphics-stack warnings (libc property access, HWUI EGL config) |
| Survives background/foreground | HOME → relaunch, twice | **PASS** — same process, frame pixel-identical after resume, no crash. (The first relaunch showed Android's one-time "Viewing full screen" immersive hint, a system overlay.) |
| Release (R8) build | `./gradlew :app:assembleRelease` | **PASS** (builds; unsigned). R8 warnings only for unused androidx/coroutines classes. Not installed or tested yet (Phase 13) |
| Launches on a **physical Android device** | — | **DEFERRED**: no Android phone available (you only have Apple devices). Re-run when a device is available |

## Remaining work

1. **Physical device (deferred):** when an Android phone is available, run `./gradlew :app:installDebug` and repeat the Phase 1 device checks.
2. Answer open questions O1–O3.
3. Optional before Wednesday: the "Cool thingo" CRT shader (lower priority than walking, as agreed).
4. **D2 (2026-11-04):** rest of Phase 5 and Phase 6 (dash with the D-B2 fix, parry, ghost trail), level generation can start in parallel.
5. Phases 7–14 as in the plan (delivery mapping in plan §10).

## Known differences (register)

| ID | Difference | Reason | Status |
|---|---|---|---|
| D1 | Touch controls (Q1) replace keyboard/mouse; keyboard/mouse still supported | Android | approved, not implemented |
| D2 | Pillarbox/letterbox on non-16:9 screens (FitViewport) | Device aspect ratios | **implemented** (Phase 1) |
| D3 | Auto-pause on backgrounding; Android Back = pause | Android lifecycle | approved, not implemented (Back currently uses libGDX's default handling) |
| D11 | Physical-device validation postponed; emulator (API 37) used meanwhile | No Android phone available | temporary |
| D13 | Background music is centred instead of panned left (the original's AudioStreamPlayer2D at world origin panned it left: 62.5 % / 37.5 % in the menu) | Your decision (O2), 2026-10-15 | approved, implement with the music (D1) |
| D15 | UI re-created in Scene2D from the scene offsets; small layout offsets vs Godot remain (e.g. tutorial Start button ≈ 10 px). Title uses Montserrat (the original's named font; Godot falls back to Open Sans where it is missing) | Engine UI system differs; font decision Q6/O1 | accepted (D1) |
| D16 | Android: the bottom edge of the inventory slots overlaps the system gesture zone; taps on the lowest pixels can go to the system | Android platform | open (revisit with the full touch layout) |
| D18 | Dash teleport deep into a 2-tile wall next to a door Preventer (`short_dash_into_east_wall`): depenetration ends up to ~20–30 px from Godot's (x agrees within 1 px). Cause not confirmed; probably the order Godot's broadphase visits the overlapping shapes, which feeds the cumulative 40 % recovery. Every other dash/parry/walk trace matches (≤ 0.01 px). The room clamp keeps the player in the room | Godot broadphase order not reproducible | accepted; test tolerance 30 px |
| D19 | Parry CHARGE tint `(2, 0.5, 0.5)` is overbright in Godot; libGDX's `SpriteBatch` colour clamps to 1, so red is capped (R8) | SpriteBatch limitation | accepted (shader if wanted later) |
| D20 | Player sounds (dash, parry, death) play non-positionally at the SFX volume; the original uses `AudioStreamPlayer2D` centred on the player | Listener is at the player anyway | accepted |
| D17 | Level start: the fade-in is not delayed by level generation frames (the original generates behind the black screen for a few frames) | D1 has no level generation yet | temporary (Phase 10) |
| D14 | When the camera's x position is exactly on a half pixel, some texels are picked one pixel to the side compared with Godot's Forward+ renderer (34/40 static reference views identical; the rest differ by 1-px texel shifts, layout identical) | Renderer rasterisation detail; in-game the camera moves through sub-pixel positions continuously | accepted (Phase 4) |
| D12 | Godot's `MainTimerSync` jitter-fix smoothing is not replicated: a tick may land one rendered frame earlier or later; tick rate and per-tick logic are identical. Idle-frame order: Timer nodes are processed after all entities' `_process`, not interleaved by tree order | Simplicity; no gameplay effect | **implemented** (Phase 2) |
| D4 | Fullscreen window → immersive landscape (`sensorLandscape`). On Android 16+ large screens (≥ 600 dp) the system may ignore the orientation lock | Platform | **implemented** (Phase 1) |
| D5 | Hover tooltips → long-press | No hover on touch | approved, not implemented |
| D6 | Bundled fonts instead of machine-dependent system fonts | Fonts absent from the repo | approved, not implemented |
| D7 | RNG sequences differ (distributions identical) | Different PRNG; original unseeded | by nature |
| D8 | Audio backend differences (mixing, latency, polyphony) | Engine | to measure |
| D9 | Shader precision on mobile GPUs | GLSL ES | to measure |
| D10 | App launcher icon from the player sprite instead of the Godot logo | Q7 | **implemented** (Phase 1) |
| D-U1 | Unused scenes, scripts and assets not ported (plan §1.3, §1.5) | Unreachable in the original | **applied to the asset copy** (Phase 1) |
| D-B11 | Fresh item instances per run | Q3 | approved, not implemented |
| D-B18 | Single death screen | Q3 | approved, not implemented |
| D-B1 | Intended pause/death behaviour ported regardless of the original's parse error | Q4 | approved, not implemented |
| D-B2 | Medium Dash: invulnerability ends when the slide ends, as for the Short Dash (the original never restores `can_take_dmg`, so the player stays invulnerable until the next room warp or parry). Exact fix: on slide end, MEDIUM restores `can_take_dmg = true` like SHORT, without the SHORT dash's blue tint | Your decision, 2026-10-15 | **implemented** (D2, `PlayerMechanicsTest.mediumDashB2Fix…`) |
