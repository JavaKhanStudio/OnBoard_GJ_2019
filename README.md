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
./gradlew :editor:runEditor                       # the level editor
```

`-Donboard.start=` accepts `logo`, `intro`, `start_screen`, `game`, `outro`, and
`sound_lab` — a development screen, not part of the game, listing every music track and
sound effect the game names, with Play/Stop through the game's own audio path.

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
./gradlew build                                        # 39 fast tests, no display needed
tools/offscreen.sh ./gradlew :verify:test -PwithGl     # + 10 that boot the real game
```

The GL tests each open a real window to get a GPU surface. `tools/offscreen.sh` runs them
inside [`cage`](https://www.hjdskes.nl/projects/cage/), a headless wlroots compositor, so
they get their own invisible display instead of five windows opening on top of whatever you
were doing. GPU rendering is preserved. Drop the wrapper — or set `ONBOARD_NO_OFFSCREEN=1` —
when you want to watch. It also works for the game itself:
`tools/offscreen.sh ./gradlew :desktop:runGame`.

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

**Kryo is pinned to `5.0.0-RC1` and must stay there.** The `.plax` backdrops are Kryo
binaries written by that exact version. Kryo's format is not stable across releases, and the
failure is silent — the file still loads, the strings just come back as mojibake, and you get
a missing-file error several frames later. `ParallaxAssetTest` reads the real files and fails
loudly if the version drifts.

**`core/jars/parallaxReader0.7.jar` has no source.** It draws every parallax backdrop and it
also owns the `.plax` format above. It survives the engine upgrade because it only touches
stable libGDX API. If it ever breaks, it has to be rewritten from scratch.

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
- **A second UI skin**, `assets/ui/skins/freezing/` — 31 atlas regions and 17 widget styles,
  never loaded. The game uses libGDX's stock skin instead. Two of its three bitmap-font
  pages are missing, so it needs those regenerated before it can be switched on.
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

The smoke in the intro (`ui/story/intro/smoke.jpg`) is a stock image and carries
`dc:creator: AnnaFomina` in its own metadata. It is credited on the screen, and the licence
is worth confirming before the game is distributed widely.

## Elsewhere

`imageReserve/` — 29 MB of unused button art — was moved out to
`../JavaKhan-onboard-art-reserve/`. Nothing referenced it and it never shipped, but it's
still in this repo's git history if you'd rather have it back. `Pause/pauseMenuOld.png` went
the same way: an earlier cut of the PAUSE panel on a 1920x1080 canvas, superseded by
`ui/icon/pause/pauseMenu.png`.
