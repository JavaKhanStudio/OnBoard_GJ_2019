# On Board — itch.io page text

Everything below the first rule is meant to be pasted into itch.io's page editor, one block
per field. The field each block goes in is named in its heading. Images are a separate task
(r129); this file has no image references.

Sources: README.md ("The story", "Controls"), core/src/jks/index/Index_Credits.java,
core/src/jks/input/IKM_Game_Keyboard.java, desktop/assets/ui/fonts/Mansalva-OFL.txt,
tools/package_all.sh (zip names).

---

## Title

On Board

## Short description or tagline

Un voyage en train à travers la vie de Ross. / A train ride through Ross's life.

## Classification

- Kind of project: Downloadable
- Release status: Released
- Pricing: No payments (On Board will never be sold)
- Genre: Adventure
- Tags (10): point-and-click, narrative, short, 2d, hand-drawn, game-jam, story-rich,
  emotional, atmospheric, singleplayer
- Languages: French
- Inputs: Keyboard, Mouse
- Platforms: Windows, macOS, Linux (set per upload)

## Uploads

| File | Platform box to tick |
|---|---|
| onboard-winX64.zip | Windows |
| onboard-macArm64.zip | macOS (name it "macOS — Apple Silicon (M1 and later)") |
| onboard-macX64.zip | macOS (name it "macOS — Intel") |
| onboard-linuxX64.zip | Linux |

## Description

### Français

**On Board** est un petit point-and-click narratif. Ross monte dans le train de 9 h 30, et
chaque wagon est un âge de sa vie : l'enfant qui rêve de partir, l'adolescent en guerre
contre son père, le soldat photographe de guerre, le jeune marié. Quatre wagons, quatre
saisons.

Dans chaque wagon, Ross cherche les trois morceaux d'une clé pour ouvrir la porte du
suivant. Et dans chaque wagon, un objet lui laisse un choix. Le jeu ne dit jamais lequel est
le bon : chacun a son prix. Ce que vous faites en chemin décide de l'une des deux fins.

Le jeu est en français.

Fait en 2019 par l'équipe PIX MEN pendant la game jam Jamming Assembly, et remis à jour
en 2026 sans rien changer à ce que l'équipe a dessiné.

### English

**On Board** is a short narrative point-and-click. Ross boards the 9:30 train, and each
carriage is an age of his life: the child who dreams of running away, the teenager at war
with his father, the soldier and war photographer, the young groom. Four carriages, four
seasons.

In each carriage Ross looks for the three pieces of a key that opens the door to the next.
And in each carriage, one object leaves him a choice. The game never says which one is
right: each has its cost. What you do along the way decides which of the two endings you
reach.

The game's text is in French only.

Made in 2019 by the PIX MEN team for the Jamming Assembly game jam, and brought up to date
in 2026 without changing what the team drew.

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

**Linux** (x86-64) — Unzip, then run `./onboard` in the "On Board" folder.
*Décompressez, puis lancez `./onboard` dans le dossier « On Board ».*

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
