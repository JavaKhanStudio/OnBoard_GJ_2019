# Reference capture

`level1-1600x900-libgdx-1.9.10.png` is the frame the game drew on **libGDX 1.9.10**, before
any of the migration work — the original against which the engine upgrade was checked, and
the reason we know 1.14.2 renders level 1 the same way.

It is kept for the record only; nothing compares against it any more.

The live goldens moved to 1280x720 when the GL tests started running offscreen under `cage`,
whose headless output is that size and cannot be resized. Comparing a 1600x900 capture with a
1280x720 one is not a like-for-like test: the game uses a ScreenViewport, so a different
window size shows a different amount of the carriage, not the same picture at another scale.
