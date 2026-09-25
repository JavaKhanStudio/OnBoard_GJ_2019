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

## Où les déposer, et comment comparer (r94)

Chaque piste générée se dépose sous `desktop/assets/musics/wagons/`, au nom de son wagon :

    wa1_ideal.ogg   wa2_ideal.ogg   wa3_ideal.ogg   wa4_ideal.ogg

(`.mp3` ou `.wav` marchent aussi, rien d'autre à changer). À côté, `wa<n>_modulated.ogg` est
l'autre option : la musique principale (`intro.mp3`) légèrement vieillie pour chaque wagon par
`tools/make_wagon_music.py` — un ton plus haut et un peu plus vite pour l'enfance, telle
quelle pour la jeunesse, un ton plus bas avec une salle autour pour l'âge adulte, une tierce
plus bas, plus lente, lointaine comme une vieille radio pour la vieillesse.

Deux endroits pour comparer :

- **Le labo de son** (lab SoundLab) : une ligne par wagon, Main / Modulated / Ideal. Le bouton
  Ideal s'allume dès que le fichier est là. Lance le lit de rails depuis l'onglet RAILS pour
  entendre la musique comme en jeu.
- **Les labos Carriage1 à Carriage4** : en haut au milieu, Music Main / Modulated / Ideal, dans
  le wagon, avec les rails. Main et Modulated sont le même morceau : on passe de l'un à l'autre,
  ou d'un wagon au suivant, sans revenir au début.

Le jeu livré n'en joue aucune : sans labo, chaque wagon garde `intro.mp3`.

## La musique idéale, selon moi (r94)

Les quatre prompts ci-dessus restent ma réponse — une seule voix, un piano droit qui vieillit —
mais pour que la comparaison avec la version modulée soit honnête, les pistes idéales doivent
vivre dans le même monde que la musique principale, qu'on entend au menu, dans l'intro et
dans la fin. Ce que j'ai mesuré de `intro.mp3` : environ **70 BPM**, tonalité de **do majeur /
la mineur** (ré, sol, do, mi dominent), une ouverture douce de 40 s puis un tutti avec beaucoup
de basse. Donc, ajoute ceci à chacun des quatre prompts :

> **ajout** — Around 70 BPM. Key centre C major / A minor, so it sits next to the main theme
> heard in the menu.

et, par wagon, la couleur tonale que je leur donnerais — le même centre, vu sous quatre angles :

| Wagon | Tonalité | Pourquoi |
|---|---|---|
| wa1 Printemps | do majeur | l'énoncé simple, la tonalité de la musique principale |
| wa2 Été | la mineur | la relative : les mêmes notes, assombries — la faute sous la chaleur |
| wa3 Automne | fa majeur | la sous-dominante : plus chaude et plus grave, ce qu'on a construit |
| wa4 Hiver | do majeur, sans jamais conclure | le retour au départ, qui ne se pose plus |

Si l'outil accepte des notes, le motif que je proposerais, en do majeur : **mi – sol – la – sol
– mi – ré**, une noire par note, puis un silence d'une mesure. Transposé tel quel dans les
trois autres tonalités, il reste reconnaissable d'un wagon à l'autre.

Si le générateur accepte un audio de référence, donne-lui `intro.mp3` pour wa1 plutôt que
rien : c'est la parenté avec la musique principale qui fera la différence avec les pistes
modulées, pas la qualité seule.
