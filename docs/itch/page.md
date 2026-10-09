# On Board — itch.io page text

Everything below the first rule is meant to be pasted into itch.io's page editor, one block
per field. The field each block goes in is named in its heading. Images are a separate task
(r129); this file has no image references.

Sources: README.md ("The story", "Controls"), core/src/jks/index/Index_Credits.java,
core/src/jks/input/IKM_Game_Keyboard.java, desktop/assets/ui/fonts/Mansalva-OFL.txt,
tools/package_all.sh (zip names), desktop/src/jks/launcher/LinuxMenuEntry.java (the Linux
menu entry, r276), core/src/jks/vinterface/LanguageFlags.java and desktop/assets/i18n/textes.tsv
(French and English, r162: every row has both).

---

## Title

On Board

## Short description or tagline

Le 9 h 30 va partir. Ross sera-t-il à bord ? / The 9:30 is leaving. Will Ross be on board?

## Classification

- Kind of project: HTML (the downloads stay attached as well)
- Release status: Released
- Pricing: No payments (On Board will never be sold)
- Genre: Adventure
- Tags (10): point-and-click, narrative, short, 2d, hand-drawn, game-jam, story-rich,
  emotional, atmospheric, singleplayer
- Languages: French, English
- Inputs: Keyboard, Mouse
- Platforms: Windows, macOS, Linux, and played in the browser (set per upload)

## Uploads

| File | Platform box to tick |
|---|---|
| onboard-winX64.zip | Windows |
| onboard-macArm64.zip | macOS (name it "macOS — Apple Silicon (M1 and later)") |
| onboard-macX64.zip | macOS (name it "macOS — Intel") |
| onboard-linuxX64.zip | Linux |
| onboard-html.zip | "This file will be played in the browser" (and no platform box) |

## Embed options (Kind of project = HTML)

- Viewport dimensions: 1280 × 720 (html/src/jks/html/HtmlLauncher.java, getConfig)
- Mobile friendly: off (never tested on a phone or a tablet)
- Automatically start on page load: off (browsers hold sound back until a click anyway)
- Fullscreen button: on
- Enable scrollbars: off

## Description

### Français

**Le 9 h 30 va partir. Ross sera-t-il à bord ?**

Un billet, un quai d'automne, un homme et sa valise. Ross monte, et le train traverse sa
vie : un wagon par âge, chacun un souvenir à traverser pour atteindre la porte du bout.

**La salle de jeux.** Il est gamin, tout ce qu'il possède tient dans un baluchon, et il va
enfin partir d'ici. Dans un coin, un oiseau a l'air affamé dans sa cage dorée.

**Le wagon de l'ado.** Des disques, des clopes, un dossier de police épais comme un roman,
et le portrait du paternel en uniforme. « Même en peinture, il me juge. »

**La guerre.** Un wagon éventré. Son appareil photo sur son trépied : « Il a vu plus de
choses que moi. » Une lettre pour Diana, jamais envoyée. Un trou dans le mur, qu'il pourrait
boucher… ou finir.

**Le mariage.** Le voile, le gâteau, le portrait de sa femme enceinte, un hochet à emballer.
Et un verre vide, qui attend qu'il décide quoi y verser.

Dans chaque wagon, les trois morceaux d'une clé, et un objet qui ne le laisse pas s'en tirer
à bon compte. Nourrir l'oiseau ou ouvrir la cage. Dessiner des cornes au paternel ou crever
la toile avec son propre couteau. L'eau ou le whisky. Personne ne dit à Ross lequel est le
bon, et le jeu non plus. Chaque choix a son prix, et au bout de la ligne il y a une porte
ouverte, et un petit garçon qui joue avec un train en bois.

Un court point-and-click narratif, dessiné à la main par l'équipe PIX MEN pendant la game
jam Jamming Assembly en 2019, remis à jour en 2026. En français et en anglais : les drapeaux
du menu de départ choisissent la langue.

### English

**The 9:30 is leaving. Will Ross be on board?**

A ticket, an autumn platform, a man with a suitcase. Ross steps aboard, and the train runs
through his life: one carriage for each age, each one a memory he has to walk through to
reach the door at the far end.

**The playroom.** He's a kid, everything he owns fits in a bundle, and he is finally running
away. In the corner, a bird in a golden cage looks hungry.

**The teenager's carriage.** Records, smokes, a police file thick as a novel, and his
father's portrait in uniform. "Even in paint, he judges me."

**The war.** A carriage blown open. His camera on its tripod: "It has seen more than I
have." A letter to Diana he never sent. A hole in the wall he could patch… or finish.

**The wedding.** The veil, the cake, a portrait of his pregnant wife, a baby's rattle to
wrap. And an empty glass, waiting for him to decide what to pour.

In each carriage, three pieces of a key, and one object that won't let him off easy. Feed
the bird or open the cage. Draw horns on the old man or take his own knife to the canvas.
Water or whisky. Nobody tells Ross which is right, and neither does the game. Every choice
costs something, and at the end of the line there is an open door, and a little boy
playing with a toy train.

A short narrative point-and-click, hand-drawn by the PIX MEN team at the Jamming Assembly
game jam in 2019 and brought up to date in 2026. In French and English: the flags on the
start menu pick the language.

### Commandes / Controls

| | |
|---|---|
| ← → (ou Q / D) | marcher / walk |
| Souris / Mouse | examiner, ramasser, utiliser / look, pick up, use |
| Échap / Esc | pause |

### Installation

Aucune installation : décompressez le fichier et lancez le jeu. Java est inclus.
No installer: unzip the file and start the game. Java is included.

**Windows** — Double-click `onboard.exe` in the "On Board" folder. Windows may show
"Windows protected your PC" (SmartScreen, unknown publisher): click **More info**, then
**Run anyway**. The game is not signed; it is not harmful.
*Windows peut afficher « Windows a protégé votre ordinateur » : cliquez sur **Informations
complémentaires**, puis **Exécuter quand même**.*

**macOS** — Take the download that matches your Mac: **Apple Silicon** for M1 and later,
**Intel** for older Macs ( → About This Mac → Chip / Processor). The game is not signed
by Apple, so the first time macOS says *"On Board is damaged and can't be opened"*. It is
not damaged. **Right-click `On Board.app` → Open**, then Open again. If that is not offered,
open Terminal in the "On Board" folder and run:

    xattr -dr com.apple.quarantine "On Board.app"

After that it opens normally.
*Le jeu n'est pas signé par Apple : macOS dit « On Board est endommagé ». Il ne l'est pas.
Clic droit sur `On Board.app` → Ouvrir, ou la commande ci-dessus dans le Terminal.*

**Linux** (x86-64) — Unzip, then run `./onboard` in the "On Board" folder. The game adds
itself to your applications menu, with its logo, when it starts. If you delete the folder,
delete `~/.local/share/applications/com.pixmen.onboard.desktop` too.
*Décompressez, puis lancez `./onboard` dans le dossier « On Board ». Le jeu s'ajoute au menu
des applications, avec son logo, quand il démarre. Si vous supprimez le dossier, supprimez
aussi `~/.local/share/applications/com.pixmen.onboard.desktop`.*

### Crédits / Credits

**PIX MEN** — Jamming Assembly, 2019

| | |
|---|---|
| Programmation | Simon Bédard |
| Art visuel | Carole Virginie, Claire Montagut, Clement Diolot |

Police des pensées de Ross / Font of Ross's thoughts: **Mansalva** by Carolina Short,
SIL Open Font License 1.1 — https://github.com/carolinashort/mansalva

Polices des menus / Menu fonts: **Optimus Princeps** and **Geo Sans Light** by Manfred Klein.

Réalisé avec / Made with libGDX.
