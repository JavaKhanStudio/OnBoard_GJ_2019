# On Board: Strategy

> **PROPOSED** by r246 (find-strategy, carrying r190's answers) on 2026-10-03. It binds
> nothing until the Commissar upholds it against the doctrine.

A strategy line says how this board applies the doctrine. Tactics (a skill or a pack with
`serves = ["Sn"]`, a closed ticket with `--serves Sn`) carry it out. A decision that goes
against a doctrine line is a doubt `--against Dn`.

**S1. What we add beside the 2019 game (labs, dev tools, the browser build) lives apart from it and can be taken out without touching what the team made.**
From: D1
A new tool goes beside their code and assets, behind its own flag or folder, not woven
into their classes.
Carried by: `-Donboard.lab=true` gating every lab, the ideal tracks kept in
`lab-assets/` (r99).

**S2. When a fix has to choose how something should behave, the team's own files decide: their data, art and code are read as the intent, not our taste.**
From: D1
An ambiguous bug is settled by the 2019 level data, painting or comment; nothing is
improved while fixing the thing next to it.
Carried by: the #level pack's reading of the 2019 level files, r169 keeping the painted
RETOUR sign. Nothing checks it yet.

**S3. The game never shows a score: karma and the choices counted stay out of the player's screens, visible only on dev surfaces.**
From: D2
A HUD, a save screen or an ending never reads out karma or a tally of good choices.
Carried by: the karma slider living in the labs only.

**S4. When code and a painting disagree on size, the code gives way: frames, bubbles and screens are fitted around the art, and a new screen shape is met by framing, never by stretching.**
From: D3
A new window shape or screen gets bars or a frame, not a scaled painting.
Carried by: the #ui pack (the bubble drawn square as painted, the HUD kept unstretched
after a resize).

**S5. Work on what is seen or heard is closed with the picture or the recording attached to its ticket, so Simon judges it without running the game.**
From: D3, D4
A layout, colour or volume ticket hands back a render or a WAV, not "tests pass".
Carried by: the render tests' frames, `build/audio/*.wav`, `tools/lab_shots.sh`,
`atelier attach`, the #verify pack.

**S6. Every effect (fade, flash, shake, volume) starts at the softest setting that still reads, and is turned up only after Simon has seen or heard it.**
From: D4
A new effect ships soft; louder or brighter is his call after a capture (S5).
Carried by: nothing yet beyond the Sound lab.

**S7. An asset that did not come from the 2019 team enters with its source and licence written down beside it, and the credits follow that record.**
From: D5
Carried by: README.md's asset notes and Credits section, `Index_Credits.java`,
CreditsRenderTest.

**S8. On Board is given away where it can be played for free, with nothing that ties it to money: no store, no ads, no tracking, no paid dependency.**
From: D5
Carried by: the #ship pack's itch release (free to play). Nothing checks it.

**S9. Staying off Simon's screen and speakers is done by the build itself, never by a wrapper or a flag someone has to remember.**
From: D6
Carried by: the Gradle offscreen cage for GL tests and agent runs, their sound sent to
`build/audio/`, the run-offscreen and capture-audio skills.
