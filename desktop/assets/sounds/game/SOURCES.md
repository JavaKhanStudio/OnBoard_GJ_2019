# Game event sounds: where they come from

Candidates for r45. The sound lab plays each one under its tab, and **Use** makes it the one
the game plays. `tools/make_jingles.py` builds all of them, and none needs a credit.

| File | Moment | Made from |
|---|---|---|
| `key_chime` | a piece of the key | synthesised glockenspiel, D6 then A6 |
| `key_sparkle` | a piece of the key | synthesised music box, G6 A6 D7 E7 |
| `key_clink` | a piece of the key | synthesised key clinks, then a D7 bell |
| `key_wood` | a piece of the key | synthesised marimba, D5 then A5 |
| `level_glockenspiel` | the key is whole | synthesised glockenspiel, G5 D6 G6, then G6+D7 |
| `level_music_box` | the key is whole | synthesised music box, D6 E6 G6 A6, then G6+D7 |
| `level_swell` | the key is whole | synthesised G-D-A chord swell under a G6+D6 bell |
| `level_whistle` | the key is whole | [Parovoz sound](https://commons.wikimedia.org/wiki/File:Parovoz_sound.ogg), Alex Lep, **public domain**, then two synthesised bells |

Every note is one of G, D, A and E. Those four fit G major, E minor, D major and D minor alike,
and `musics/intro.mp3` sits somewhere among those keys, so no chime clashes with the music.
