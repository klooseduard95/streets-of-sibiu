# Migration Status — Streets of Sibiu (Godot → Kotlin/libGDX/Android)

Plan: `MIGRATION_PLAN.md` (audit approved 2026-10-15; all §9 recommendations accepted).

**Overall state (2026-10-15): Phases 0–3 are done. Physical-device testing is deferred (no Android phone available; the emulator and desktop are used instead). The migration is NOT complete.** No gameplay has been ported yet.

Next delivery: **D1, target 2026-10-22 (to confirm)**, covering Phases 2–4 (see plan §10).

| Phase | State |
|---|---|
| 0 — Decisions and Godot reference data | **Done** (2026-10-15): Godot 4.6.1 installed, V1–V15 answered, golden data recorded |
| 1 — Minimal libGDX project builds and runs | **Done** on desktop and emulator; physical device **deferred** |
| 2 — Runtime foundation (`:logic`) | **Done** (2026-10-15): 39 new unit tests, all passing |
| 3 — Data and rooms | **Done** (2026-10-15): 20 rooms converted, all game data transcribed, 24 parity tests passing |
| 4–14 | Not started |

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
| O1 | Fonts: Q6 approved bundling Montserrat / JetBrains Mono / Hack. V11 shows that players without those fonts installed saw **Open Sans SemiBold** everywhere. Which look should the port reproduce: the designers' intended fonts or what most players saw? | Bundle the intended fonts (Q6) |
| O2 | Music panning: the original music is panned left (62.5 % L / 37.5 % R in the menu) due to a Godot positional-audio side effect. Keep it, or centre the music? | Keep it (faithful) |
| O3 | Delivery dates in plan §10 (Thursdays from 2026-10-22). Correct? | As proposed |

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
3. **Phase 4:** room rendering and camera, wiring `GameLoop` into `:core` (completes D1).
4. Phases 5–14 as in the plan (delivery mapping in plan §10).

## Known differences (register)

| ID | Difference | Reason | Status |
|---|---|---|---|
| D1 | Touch controls (Q1) replace keyboard/mouse; keyboard/mouse still supported | Android | approved, not implemented |
| D2 | Pillarbox/letterbox on non-16:9 screens (FitViewport) | Device aspect ratios | **implemented** (Phase 1) |
| D3 | Auto-pause on backgrounding; Android Back = pause | Android lifecycle | approved, not implemented (Back currently uses libGDX's default handling) |
| D11 | Physical-device validation postponed; emulator (API 37) used meanwhile | No Android phone available | temporary |
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
| D-B2 | Medium Dash: invulnerability ends when the slide ends, as for the Short Dash (the original never restores `can_take_dmg`, so the player stays invulnerable until the next room warp or parry). Exact fix: on slide end, MEDIUM restores `can_take_dmg = true` like SHORT, without the SHORT dash's blue tint | Your decision, 2026-10-15 | approved, implement in Phase 6 |
