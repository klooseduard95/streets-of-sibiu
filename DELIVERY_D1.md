# Streets of Sibiu (Android port) — Delivery 1

**Date:** Wednesday 2026-10-21 · **Scope:** the main menu and the start of the game (the first part of the Godot → Kotlin/libGDX migration). This is not the whole game.

## What is included

| Area | Behaviour (matches the original Godot 4.6 game unless noted) |
|---|---|
| Main menu | Original background and title; **Start Game**, **Options** and **Exit** buttons; tutorial overlay with the original controls text and its **Start** button; "Cool thingo" button (see gaps) |
| Options | Master / Music / SFX volume sliders working on the original audio buses (−5.93 / −5.93 / −10.25 dB defaults, 0.05 steps); **Go Back**. Volumes are not saved, as in the original |
| Music | Original background track, looping and playing while paused; centred instead of the original's left pan (your decision). The rare horse sound (1 in 250 per second) is included |
| Start of the game | Level 1 begins with the black screen fading into the spawn room; HUD with the HP bar and shield (top left) and the inventory (Punch in slot 3; keys 1–5 or taps move the highlight) |
| Walking | 8-direction movement at the original speed, wall and pillar collisions, wall sliding, the room clamp and the original camera (limits and smoothing). Walk animations frame-identical to the original |
| Pause | P, Esc, Android Back or the on-screen **II** button. **Resume**, **Settings**, **Main Menu** and the statistics panel. This is the *intended* behaviour: in the original snapshot the pause and death scripts fail to load, so the pause panel is stuck on screen there |
| Controls | Desktop: WASD to move, P to pause. Android: drag on the left half of the screen (virtual stick, 8 directions), tap the inventory slots, **II** or Back to pause |

## How to run it

**Desktop (fastest):**
```bash
cd ~/AndroidStudioProjects/StreetsofSibiu
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :desktop:run
```

**Android emulator or phone:**
1. In Android Studio, open the project, choose the **app** configuration and a device, then press **Run**.
2. Or from a terminal:
   ```bash
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   The APK is `app/build/outputs/apk/debug/app-debug.apk`.

## How it was checked

* **74 automated tests pass.** Among them:
  * 20 rooms and all game data checked against the original files;
  * camera and timers checked against recordings from Godot 4.6.1;
  * **9 recorded walking scenarios from Godot replayed through the port: positions within 0.073 px at every tick, animation frames identical.**
* Godot renders the original menu, tutorial, options and game start; the port's screenshots were compared with them. Several differences found this way were fixed (tutorial text, button text, title shadow, line spacing, separators, decimal format).
* Android emulator (API 37): full flow tested (menu → tutorial → game → walk → pause → main menu → options → back), no crashes.

## Known gaps and differences in this delivery

| Item | Status |
|---|---|
| Level generation, room-to-room travel | Not yet (later delivery). Only the spawn room exists; walking into an opening stops at the doorway |
| Enemies, combat, weapons, dash, parry, items, upgrades | Not yet (later deliveries). Space/E/R/click do nothing yet |
| "Cool thingo" retro (CRT) effect | Button present; **effect not yet ported** (lower priority than walking, as agreed) |
| Title font | Montserrat, the font named by the original. On machines without it, the original falls back to Open Sans |
| UI layout | Matches the Godot renders closely. Small offsets remain, e.g. the tutorial's Start button is about 10 px higher |
| Level start | The original spends a few extra frames generating the level behind the black screen; here the fade is not delayed by generation |
| Android: bottom screen edge | The lowest pixels of the inventory slots overlap Android's gesture area; taps there can be taken by the system |
| Physical Android phone | **Not tested** (no Android phone available). Tested on the emulator and desktop |

Full details: `MIGRATION_STATUS.md` (status, tests, differences register) and `MIGRATION_LOG.md` (history).
