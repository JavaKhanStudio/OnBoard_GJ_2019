On Board — version @VERSION@

FRANÇAIS

Prenez le zip de votre ordinateur, décompressez-le : il donne un dossier « On Board ».

  Windows          onboard-winX64.zip     ouvrez onboard.exe
  Linux            onboard-linuxX64.zip   lancez ./onboard
  Mac (M1 et +)    onboard-macArm64.zip   ouvrez « On Board.app »
  Mac (Intel)      onboard-macX64.zip     ouvrez « On Board.app »

Sur Linux, le jeu s'ajoute au menu des applications, avec son logo, quand il
démarre. Si vous supprimez le dossier, supprimez aussi
~/.local/share/applications/com.pixmen.onboard.desktop

Le jeu n'est pas signé :
  - Windows dit « Windows a protégé votre ordinateur » : cliquez « Informations
    complémentaires », puis « Exécuter quand même ».
  - macOS dit « On Board est endommagé » : il ne l'est pas. Clic droit sur
    « On Board.app », Ouvrir, une seule fois. Sinon, dans le Terminal :
        xattr -dr com.apple.quarantine "On Board.app"

Dans le navigateur : onboard-web.zip donne « On Board (navigateur - browser) ».
Ouvrez « Jouer - Play » pour votre ordinateur (.bat sur Windows, .command sur
Mac, .sh sur Linux, à lancer dans un terminal). Il ouvre le jeu dans votre
navigateur ; laissez sa fenêtre ouverte pendant que vous jouez. N'ouvrez pas
le dossier « fichiers - files » : un navigateur ne lance pas le jeu depuis un
disque.


ENGLISH

Take the zip for your computer and unzip it: it gives one "On Board" folder.

  Windows          onboard-winX64.zip     open onboard.exe
  Linux            onboard-linuxX64.zip   run ./onboard
  Mac (M1 and up)  onboard-macArm64.zip   open "On Board.app"
  Mac (Intel)      onboard-macX64.zip     open "On Board.app"

On Linux, the game adds itself to the applications menu, with its logo, when it
starts. If you delete the folder, delete
~/.local/share/applications/com.pixmen.onboard.desktop too.

The game is not signed:
  - Windows says "Windows protected your PC": click "More info", then "Run anyway".
  - macOS says "On Board is damaged": it is not. Right-click "On Board.app",
    Open, once. Or, in Terminal:
        xattr -dr com.apple.quarantine "On Board.app"

In the browser: onboard-web.zip gives "On Board (navigateur - browser)".
Open "Jouer - Play" for your computer (.bat on Windows, .command on Mac, .sh
on Linux, run in a terminal). It opens the game in your browser; leave its
window open while you play. Do not open the "fichiers - files" folder: a
browser will not run the game from a disk.
