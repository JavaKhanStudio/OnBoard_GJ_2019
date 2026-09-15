# Une musique par wagon — prompts de génération

Quatre pistes, une par wagon, dans l'ordre du voyage de Ross : wa1 Printemps (l'enfance),
wa2 Été (la jeunesse), wa3 Automne (l'âge adulte), wa4 Hiver (la vieillesse). Les prompts
sont en anglais, c'est la langue que les générateurs comprennent le mieux ; colle le bloc
`prompt` tel quel.

## Ce qui vaut pour les quatre

Une même voix qui vieillit, pas quatre morceaux différents. Le fil qui les tient ensemble :

- **Le même motif**, cinq ou six notes, présent dans les quatre. Le générateur ne peut pas
  garder un thème d'une piste à l'autre — génère donc wa1 en premier, puis, si l'outil
  accepte un extrait de référence (continuation, audio prompt, style reference), donne-lui
  wa1 pour les trois suivantes. Sinon, la parenté tiendra à l'instrument et au tempo.
- **Le même instrument principal du début à la fin** : un piano droit un peu désaccordé,
  proche du micro, avec le bruit des marteaux. C'est lui qui vieillit d'un wagon à l'autre —
  l'accompagnement change autour de lui.
- **Instrumental, sans voix.** Pas de batterie, pas de percussion marquée, pas de synthé
  moderne, pas de basse appuyée : le lit de rails tourne déjà en dessous en permanence et
  occupe tout le bas du spectre. Ce qui est écrit doit vivre entre 200 Hz et 4 kHz.
- **Une nappe très peu chargée**, pensée pour passer sous les dialogues : de longs silences,
  pas de mélodie qui réclame l'attention, rien qui monte brusquement.
- **Tempo lent et régulier**, 60–72 BPM — le rythme du train, jamais contre lui.
- **2 à 3 minutes, bouclables sans couture** : même nuance au départ et à l'arrivée, pas de
  fin qui conclut, pas de fondu. La piste tourne en boucle tant que le joueur est dans le
  wagon.
- **Format** : WAV ou MP3 44,1 kHz stéréo. La conversion et le niveau se règlent ensuite.

---

## wa1 — Printemps, l'enfance

Le wagon des jouets : un baluchon, des cubes, une cage avec sa clef. Naïf et lumineux, mais
déjà un peu solitaire — un enfant qui joue seul dans un train qui roule. C'est l'énoncé du
thème, au plus simple : une seule main, presque une comptine, laissée respirer.

> **prompt** — Gentle, sparse solo upright piano lullaby, slightly out of tune, close-miked
> with audible hammers and room tone. Simple childlike five-note motif in a major key,
> played once and left to ring, with long pauses between phrases. A soft glockenspiel
> doubles a few notes here and there. 66 BPM, very quiet, tender and a little lonely.
> No drums, no bass, no vocals, no synths. Seamless loop, 2–3 minutes, constant dynamics
> throughout, no intro build and no ending.

## wa2 — Été, la jeunesse

La cigarette, le couteau, le dossier de police, le tableau. La chaleur et la faute : le motif
de l'enfance revient mais s'est troublé, plus rapide, plus sûr de lui, et quelque chose de
tendu passe dessous. La rue en été, tard le soir.

> **prompt** — The same upright piano, now restless: the childlike motif returns in a minor
> key, syncopated, with a smoky late-night jazz feel. A muted upright double bass walks
> softly and a brushed snare whispers in the background, both far back in the mix. One
> distant, breathy clarinet line, hesitant. 72 BPM, warm and humid, confident on the surface
> with an uneasy undercurrent. No vocals, no synths, no loud percussion, nothing sharp or
> sudden. Seamless loop, 2–3 minutes, even dynamics, no intro and no ending.

## wa3 — Automne, l'âge adulte

L'appareil photo, le chapeau, la lettre d'amour, la grenade, le trou dans la porte. Ce qu'on
a construit et ce qu'on a perdu, dans le même wagon. La musique la plus pleine des quatre,
et la plus grave : le thème est enfin harmonisé, adulte — et il y a la guerre au fond.

> **prompt** — The same upright piano, fuller and heavier: the motif now harmonised in slow,
> weighty chords. A small string section — cello leading, violins high and thin — swells
> underneath and fades back without ever resolving. A single distant muted trumpet, far away
> as if from another carriage. 60 BPM, autumnal, dignified, carrying grief and pride at once.
> No drums, no bass guitar, no vocals, no synths. Nothing triumphant, no climax. Seamless
> loop, 2–3 minutes, steady dynamics, no intro and no ending.

## wa4 — Hiver, la vieillesse

Le cadre, le hochet gardé toute une vie, le verre et le whisky, le papier. Tout s'est vidé.
Le thème revient tel qu'au premier wagon, une seule main, mais plus lent, avec des notes qui
manquent — comme un souvenir qu'on n'arrive plus à jouer en entier.

> **prompt** — Solo upright piano alone again, very slow and very quiet, playing the same
> childlike motif from the first track but incomplete: notes missed, phrases trailing off
> into silence, the sustain pedal held so the room rings. A faint bowed-glass or high string
> harmonic hovers above, barely audible. 56 BPM, frail, still, at peace rather than sad.
> No drums, no bass, no vocals, no synths, no melody in the foreground. Seamless loop,
> 2–3 minutes, unchanging dynamics, no intro and no ending — it should sound like it was
> already playing before you walked in.

---

## Quand les pistes sont générées

Elles ne se branchent pas toutes seules : GVars_AudioManager ne connaît aujourd'hui qu'une
piste (musics/intro.mp3) et rien ne change de musique en changeant de wagon. Poser les
fichiers ici ne suffit pas — il faudra une tâche pour les jouer, avec un enchaînement d'un
wagon au suivant. Comme pour les bruitages, chaque candidat retenu se juge dans le labo de
son avant d'être choisi.
