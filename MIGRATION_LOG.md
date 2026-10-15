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
