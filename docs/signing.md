# Signing On Board's packages

Why the Windows and Mac builds warn, what signing them costs, what Simon has to do, and what a
session does once he has. Written for atelier r222 (2026-10-02). Prices were read that day from
prices.azure.com and developer.apple.com/programs/enroll.

## Where things stand

| Package | Signed? | What the player sees | Needs signing? |
|---|---|---|---|
| `onboard-winX64.zip` | no | "Windows protected your PC" (SmartScreen): More info, then Run anyway | yes, to lose "Unknown publisher" |
| `onboard-macArm64.zip`, `onboard-macX64.zip` | no | "On Board is damaged and can't be opened" (Gatekeeper): right-click, Open | yes, plus notarization |
| `onboard-linuxX64.zip` | no | nothing: Linux does not check | no |
| `onboard-html.zip` | n/a | nothing: a website is trusted through its https | no |

Until the builds are signed, `dist/LISEZMOI - README.txt` tells players how to get past both warnings.

Signing is not something the build can do alone. It proves WHO made the program, so it needs an
identity that Microsoft or Apple checked, and that identity is paid for. No session can create it:
it needs Simon's name, his government ID and his card.

## Windows: Azure Trusted Signing (now named Artifact Signing)

The cheapest route that SmartScreen honours. **9.99 USD a month** (Basic: 5,000 signatures a
month, far more than releases need). Individual developers are accepted if they live in the
United States or Canada. Cancel it between releases and nothing already signed stops working:
each signature is timestamped.

WHAT SIMON DOES (about an hour, then a wait of a few days for the identity check):
1. Create an Azure account whose billing account type is **Individual**, with his legal name and
   address exactly as on his ID.
2. In the Azure portal, create a *Trusted Signing account* (Basic SKU), then an *identity
   validation* request (Public, Individual). He submits his ID, and Microsoft verifies it.
3. Once it is validated, create a *certificate profile* (Public Trust) and an app registration
   (service principal) with the *Trusted Signing Certificate Profile Signer* role. Write the
   tenant id, client id, client secret, account endpoint, account name and profile name in
   `~/.config/onboard/signing.env`, never in the repo.

WHAT A SESSION THEN DOES: sign `On Board/onboard.exe` inside the Windows zip with
[jsign](https://ebourg.github.io/jsign/) (`--storetype TRUSTEDSIGNING`), which runs on Linux.
This becomes a step in `tools/package_all.sh` that runs only when `signing.env` exists.

WHAT IT DOES NOT BUY: a signed .exe still starts with no SmartScreen reputation. The publisher
then reads "Simon Bédard" instead of "Unknown publisher", and the warning fades as downloads
accumulate. No certificate, at any price, removes it on day one any more.

The other routes: a classic OV certificate costs about 200-400 USD a year and ships on a hardware
token. SignPath Foundation signs free for open-source projects, but it requires an OSI licence,
and this repo has none. Choosing a licence is the 2019 team's decision (D1), not ours.

## macOS: Apple Developer Program

**99 USD a year**, and there is no cheaper route: Gatekeeper accepts only Apple's own Developer ID
certificates, and only on notarized apps (Apple scans the app and issues a ticket).

WHAT SIMON DOES:
1. Enroll at developer.apple.com/programs/enroll as an individual, with an Apple Account that
   has two-factor authentication. Apple checks his identity, which takes a day to a few days.
2. Create a **Developer ID Application** certificate. This needs a certificate signing request,
   and `rcodesign generate-certificate-signing-request` makes one on Linux.
3. In App Store Connect, create an **API key** (Users and Access, Integrations, Keys) for notarization.
4. Put the certificate (.p12 and its password) and the API key in `~/.config/onboard/`, never in
   the repo.

WHAT A SESSION THEN DOES: sign and notarize both Mac apps from this machine. **No Mac is
needed**: [rcodesign](https://gregoryszorc.com/docs/apple-codesign/) (apple-codesign) signs,
submits to Apple's notary service and staples the ticket, all on Linux. The real work is that a
Java app must have EVERY native binary signed with the hardened runtime: each .dylib of
construo's bundled JRE, the `java` launcher, and the LWJGL .dylib files inside the fat jar.
Java also needs three entitlements:
`com.apple.security.cs.allow-jit`, `com.apple.security.cs.allow-unsigned-executable-memory` and
`com.apple.security.cs.disable-library-validation` (LWJGL loads its natives from a temp folder).
This is a step in `tools/package_all.sh`, plus a check that runs
`rcodesign verify` on every binary.

What a session can NOT check here: whether a real Mac opens the result without a warning. Apple's
notary answer ("Accepted") is the best proof this machine can get. The first signed build needs
one person on a Mac to double-click it.

## Linux and the browser build

Neither is signed, and neither needs to be. To let the old team check that a zip on the drive is
the one we built, `dist/SHA256SUMS` can list each zip's checksum (`sha256sum dist/*.zip`).

## What it costs in total

Both, for one release: 99 USD (Apple, a year) + 9.99 USD (Azure, one month, cancelled after) =
about **109 USD**, then 99 USD a year to keep Mac releases signed. On Board is never sold (D5), so
this is money out with nothing coming back. Unsigned plus the README's two workarounds costs
nothing, and it is how most free itch.io games ship.
