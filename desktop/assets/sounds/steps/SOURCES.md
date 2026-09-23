# Footsteps: where they come from

Candidates for r87: one of Ross's steps on the carriage floor. The game plays one each time a
heel comes down in his walk (frames 4 and 8 of `move`), a little higher or lower each time.
The sound lab's Play walks eight steps at that pace, and **Use** makes it the one the game plays.
`tools/make_footsteps.py` builds all of them.

| File | Floor | Made from |
|---|---|---|
| `steps_shoe` | wooden floor | synthesised: a soft heel, then the ball of the foot, over the floor's low modes |
| `steps_boot` | wooden floor | synthesised: a harder heel with a click, the floor ringing longer |
| `steps_runner` | carpet runner | synthesised: a muffled press, the floor barely heard |
| `steps_thud` | — | [Dull thud](https://commons.wikimedia.org/wiki/File:Dull_thud.ogg), gregoryweir, **public domain**, cut to 0.2 s |

None needs a credit.
