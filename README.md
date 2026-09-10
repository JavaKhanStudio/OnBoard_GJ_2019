# On Board

A narrative point-and-click made in 2019 by **PIX MEN**. Ross travels through four
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

`-Donboard.start=` accepts `logo`, `intro`, `start_screen`, `game`, `outro`.

Nothing needs to be installed first — Gradle fetches its own Java 21.

### Controls

| | |
|---|---|
| Arrow keys / Q D | move (AZERTY-aware — see `GVars_Heart.isAzerty`) |
| Mouse | interact with objects |
| DEL | restart the level |
| Gamepad | supported; buttons come from the device's own mapping |

## Packaging a Windows build

```bash
./gradlew :desktop:packageWinX64
# -> desktop/build/construo/dist/onboard-winX64.zip   (~54 MB)
```

A self-contained folder: `onboard.exe`, a trimmed Java runtime, and one jar with the code
and assets. Players need nothing installed. Cross-builds from Linux or macOS — no Windows
machine required. `packageLinuxX64` does the same for Linux.

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
| `editor/` | the level editor that produced the `.wa` files |
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

- **The Credits screen.** There's a Credits button on the start menu and it does nothing.
  For a project being handed back to the people who made it, this is the obvious first thing
  to fill in — **nobody's name is anywhere in this repository.**
- **Sound effects.** `Enum_Sounds_Game` names jump, run and idle, the loader for them is
  commented out, and there are no files — they were never recorded. Music does work
  (`musics/intro.mp3`, looping, volume-controlled); it is the effects that are missing.
- **A second UI skin**, `assets/ui/skins/freezing/` — 31 atlas regions and 17 widget styles,
  never loaded. The game uses libGDX's stock skin instead. Two of its three bitmap-font
  pages are missing, so it needs those regenerated before it can be switched on.
- **Pause-menu art** (`ui/icon/pause/`) — 1.4 MB of it. `OverlayPause` reuses the Settings
  button art instead.
- **A Load Game button image** with no save system behind it.
- `JksTextureList`, 386 lines of texture-picker widget, referenced by nothing.

## Elsewhere

`imageReserve/` — 29 MB of unused button art — was moved out to
`../JavaKhan-onboard-art-reserve/`. Nothing referenced it and it never shipped, but it's
still in this repo's git history if you'd rather have it back.
