# NFC Card

Android app that makes your phone act like a tappable NFC tag. Pick a profile link (LinkedIn, WhatsApp, or any custom URL); when another phone taps yours, it receives that link — with an on-screen QR code as fallback.

Uses Host Card Emulation to emulate an NFC Forum Type 4 Tag serving an NDEF URI record.

## Usage

1. Tap **+** to add profiles: LinkedIn (URL or username), WhatsApp (phone number → wa.me link), or any custom link.
2. Tap a profile to make it the shared link, or type a one-off link and hit Share.
3. Hold the other phone's NFC antenna against yours (screen on, unlocked), or let them scan the QR code.

Long-press a profile to edit or delete it.

## Build

```
./gradlew assembleDebug   # requires Android SDK (set sdk.dir in local.properties)
./gradlew test            # protocol + link/profile unit tests
```
