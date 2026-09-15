# On Board

A narrative point-and-click made in 2019 by **PIX MEN** for the **Jamming Assembly** jam. Ross travels through four
carriages of a train, one for each stage of his life — child, teenager, soldier, married —
solving a small puzzle in each. What you do along the way decides which of the two endings
you get.

Built with libGDX. Originally Java 8 and libGDX 1.9.10; brought up to Java 21 and libGDX
1.14.2 without changing how the game looks or plays.

---

## Running it

```bash
./gradlew :desktop:runGame                        # the whole thing, from the logos
./gradlew :desktop:runGame -Donboard.start=game   # straight into level 1
./gradlew :desktop:runGame -Donboard.start=game -Donboard.level=3   # or into level 3
./gradlew :editor:runEditor                       # the level editor
```

`-Donboard.start=` accepts `logo`, `intro`, `start_screen`, `game`, `outro`, `credits` —
the start screen with the credits already open, so Retour lands on the menu — and
`sound_lab` — a development screen, not part of the game, listing every music track and
sound effect the game names, with Play/Stop through the game's own audio path — and
`item_lab`, another: a carriage with sliders for the yellow outline a hovered item gets
(width, opacity, stroke, speed), and a button that outlines every item at once.

`-Donboard.level=` (1 to 4) picks the carriage `game` or `item_lab` starts in. Nothing is carried
over from the carriages skipped, which costs only karma: from level 3 or 4 the leaving ending
cannot be reached — unless you add `-Donboard.lab=true`, which puts a karma slider in the right
end of the black bar under the carriage (`item_lab` always has it). It sets the karma live, and
says which ending that karma leads to.

Nothing needs to be installed first — Gradle fetches its own Java 21.

### Controls

| | |
|---|---|
| Arrow keys / Q D | move (AZERTY-aware — see `GVars_Heart.isAzerty`) |
| Mouse | interact with objects |
| DEL | restart the level |
| Gamepad | supported; buttons come from the device's own mapping |

## Packaging

All four builds are made from this one machine, whatever it is — construo downloads the
target JDK, and jlink trims it. No Mac or Windows machine is involved.

```bash
./gradlew :desktop:packageWindows           # Windows x86-64
./gradlew :desktop:packageLinux             # Linux x86-64
./gradlew :desktop:packageMacAppleSilicon   # macOS arm64
./gradlew :desktop:packageMacIntel          # macOS x86-64
```

Everything lands in `dist/` at the top of the project — a zip to send people, and the
same thing unpacked to run right now:

| | zip | run |
|---|---|---|
| Windows | `onboard-winX64.zip` | `OnBoard-windows/onboard.exe` |
| Linux | `onboard-linuxX64.zip` | `OnBoard-linux/onboard` |
| macOS Apple Silicon | `onboard-macArm64.zip` | `OnBoard-macapplesilicon/On Board.app` |
| macOS Intel | `onboard-macX64.zip` | `OnBoard-macintel/On Board.app` |

Each is self-contained — launcher, trimmed Java 21 runtime, and one jar with the code and
assets. Players install nothing. Roughly 54-58 MB zipped, ~90 MB unpacked.

Build them **one at a time**. The fat jar carries a single platform's native libraries, so
asking for two targets in one invocation is refused rather than silently shipping the wrong
ones. `checkDistNatives` fails the build if a package contains a library belonging to
another platform, which is otherwise invisible until a player runs it.

### macOS: the builds are unsigned

They have no Apple Developer signature, because signing needs a paid certificate and,
for notarisation, Apple's servers. Gatekeeper will refuse a double-click with *"On Board
is damaged and can't be opened"* — which is misleading; it means unsigned, not broken.

Whoever you send it to opens it once with **right-click → Open**, or clears the quarantine
flag themselves:

```bash
xattr -dr com.apple.quarantine "On Board.app"
```

After that it opens normally. Signing properly needs a Mac and a developer account.

## Tests

```bash
./gradlew build                          # 39 fast tests, no display needed
./gradlew :verify:test -PwithGl          # + 10 that boot the real game, offscreen
```

The GL tests each open a real window to get a GPU surface. The build runs them inside
[`cage`](https://www.hjdskes.nl/projects/cage/), a headless wlroots compositor, so they get
their own invisible display instead of five windows opening on top of whatever you were
doing. GPU rendering is preserved. Set `ONBOARD_NO_OFFSCREEN=1` when you want to watch.
Without cage installed they open on your screen, as they always did.

`:desktop:runGame` and `:editor:runEditor` go offscreen the same way when a board agent runs
them (`ATELIER_AGENT` is set), since nobody is watching those windows. When you run them,
or click the board's "Play it" buttons, they open on your screen; `ONBOARD_OFFSCREEN=1`
sends them offscreen anyway. The mechanism is in `gradle/offscreen.gradle`. Anything that
opens a window without going through Gradle, such as a packaged build or a fat jar, still
needs the wrapper: `tools/offscreen.sh <command>`.

The sound goes with the windows. Whenever a task goes offscreen, the game's audio plays into
a WAV file instead of your speakers: `verify/build/audio/test.wav` for the GL tests,
`desktop/build/audio/runGame.wav` for an agent's run. That uses OpenAL Soft's WAV writer,
which LWJGL ships, so it works with or without cage and on every OS. MusicTest reads its
file back to check the music really came out, and that muting silenced it. The files are
overwritten on each run, at about 10 MB a minute of play. `ONBOARD_NO_OFFSCREEN=1` brings the
speakers back along with the windows. `tools/offscreen.sh` discards the sound
(`ALSOFT_DRIVERS=null`).

Two consequences worth knowing. The GL tests run at **1280x720**, because that is the size
of cage's headless output and neither cage nor wlroots lets you change it. And they capture
frames on **the game's own clock** rather than at a frame index: anything driven by a timer —
a fade, the typing effect, the parallax scroll — sits somewhere different at frame N
depending on how fast the machine drew those N frames, which made the goldens flap between
an idle run and a busy one.

The GL tests render the game and compare against golden frames. To re-record after an
intentional visual change: `-PwithGl -PrecordGolden`, and look at the images before you
commit them.

`verify/src/test/resources/golden/reference-1.9.10/` holds the frame the game drew on
libGDX 1.9.10, before any migration work. Nothing compares against it now; it is the
evidence that 1.14.2 renders level 1 the same way it always did.

## Layout

| | |
|---|---|
| `core/` | the game — views, input, UI, level model |
| `desktop/` | launcher, and `desktop/assets/` (all art, audio, levels) |
| `editor/` | the level editor that produced the `.wa` files — its item palette, `JksTextureList`, lives in `core/` though the game never uses it |
| `test/` | a scratch module for trying widgets out |
| `verify/` | the test suite |

Levels are `desktop/assets/game/wagon/wa{1..4}.wa` — JSON written by the editor. The
scrolling backdrops are `.plax` files, and `.plaxpj` are the editor's project files for them.

---

## Things worth knowing before you change anything

**The backdrops come from a library, which also owns the `.plax` format.**
[`io.github.javakhanstudio:parallax-background`](https://github.com/JavaKhanStudio/JKS_Tools2D_ParallaxBackground)
(`parallaxVersion` in the root build) draws every carriage's backdrop and reads its `.plax`.
It replaced a frozen `parallaxReader0.7.jar` in r33. The `.plax` files are Kryo binaries
written in 2019 by kryo 5.0.0-RC1; the library runs on kryo 5.6.2 and reads that old layout
with its own serializer. Do not add a Kryo of your own: Kryo's format is not stable across
releases and a mismatch fails silently — the file still loads, the strings come back as
mojibake, and you get a missing-file error several frames later. `ParallaxAssetTest` reads
the real files through the library's own `GVars_Serialization`, the one the game uses.
The library's editor writes a newer `.plax` layout that the library still reads; the
repo's files were read field for field the same by the old jar and by 2.1.0.

**Backdrop region names must be plain ASCII.** Each layer is looked up by name in the atlas,
and libGDX reads an `.atlas` in the JVM's default charset: cp1252 on Windows before Java 18,
UTF-8 since. Level 2's backdrop named its region `été` as cp1252 had decoded it, so on
current JVMs nothing matched and the game crashed moving to level 2. It is `ete` now. The
crash showed up as `Asset not loaded: game/wagon/wa2/WAGON.png`, because `WagonLevel.init`
swallows the real exception. `ParallaxAssetTest` rejects non-ASCII names and layers the atlas
lacks, and `LevelWalkTest` (GL) walks all four levels in order.

**The backdrop atlas comes from the asset root.** The library loads the atlas a `.plax` names by
its bare name, so level 1 draws `desktop/assets/Printemps.atlas`, not the byte-identical copy
beside the `.plax` in `game/wagon/wa1/`. Edit, recompress or delete the two together;
`ParallaxAssetTest` fails if they drift apart.

**The libGDX version is pinned by resolution strategy** in the root build. vis-ui declares
its own libGDX and Gradle resolves conflicts by taking the highest, which silently gave the
runtime a different engine than the compiler saw.

**Asset filenames are case-sensitive.** The game was written on Windows, and one mismatched
capital (`UI_newGame.png` vs `UI_NewGame.png`) was enough to stop it booting on Linux.
`AssetPathTest` checks every path the code names.

**VisUI prints a version warning on startup.** It targets libGDX 1.14.1 and we're on 1.14.2.
Cosmetic; no vis-ui release targets 1.14.2 yet.

## Built but never finished

Left as the team left it, in case anyone wants to pick it up:

- **Sound effects.** `Enum_Sounds_Game` names jump, run and idle, the loader for them is
  commented out, and there are no files — they were never recorded. Music does work
  (`musics/intro.mp3`, looping, volume-controlled); it is the effects that are missing.
- **A second UI skin**, removed in 2026. `assets/ui/skins/freezing/` was never loaded; the
  game uses libGDX's stock skin. It wasn't the team's art: it was Raymond Buckley's
  *Freezing UI* (CC BY 4.0, which requires crediting him), unmodified, and
  [czyzby/gdx-skins](https://github.com/czyzby/gdx-skins/tree/master/freezing) still hosts
  it. Its fonts are ASCII only, so it could not have shown French accents without
  regenerating them.
- **A pause menu, and its art.** Nothing opens `OverlayPause` — no key or pad button calls
  `togglePauseMenu()`, and it has been that way since 2019. `ui/icon/pause/` holds the kit
  drawn for it: the PAUSE panel, a "Couper le son :" label with its two checkbox states, and
  the Retour sign (which the credits use). The overlay itself reuses the Settings button art
  instead. An older copy of the panel went to the art reserve (see below).
- **A Load Game button image** with no save system behind it.

## Credits

| | |
|---|---|
| Programmation | Simon Bédard |
| Art visuel | Carole Virginie, Claire Montagut, Clement Diolot |

Edit `core/src/jks/index/Index_Credits.java` — that file is the whole of the credits
content, and the screen follows it. Roles are plain strings so they can be reworded.

The smoke in the intro (`ui/story/intro/smoke.jpg`) started as a stock image: its metadata
names **iStock (Getty Images) asset 511936068**, "white smoke cloud" by AnnaFomina
(`exiv2 -pa smoke.jpg` shows it). Settled in 2026 by Simon: one of the team's artists bought
it long ago and modified it — that is the March 2017 Photoshop edit the metadata records —
and On Board is not a commercial product. It is not credited on the screen, and there is
nothing left to confirm before distributing the game.

## Elsewhere

`imageReserve/` — 29 MB of unused button art — was moved out to
`../JavaKhan-onboard-art-reserve/`. Nothing referenced it and it never shipped, but it's
still in this repo's git history if you'd rather have it back. `Pause/pauseMenuOld.png` went
the same way: an earlier cut of the PAUSE panel on a 1920x1080 canvas, superseded by
`ui/icon/pause/pauseMenu.png`.
# OnBoard_GJ_2019
