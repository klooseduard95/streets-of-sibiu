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

(continued below)
