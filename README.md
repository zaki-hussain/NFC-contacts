# NFC Card

Android app that makes your phone act like a tappable NFC tag. Enter any URL; when another phone taps yours, it receives that URL (like scanning a physical NFC tag).

Uses Host Card Emulation to emulate an NFC Forum Type 4 Tag serving an NDEF URI record.

## Usage

1. Open the app, enter a link, tap Save.
2. Keep the screen on and unlocked, hold the other phone's NFC antenna against yours.
3. The other phone gets the link as if it scanned an NFC tag.

## Build

```
./gradlew assembleDebug   # requires Android SDK (set sdk.dir in local.properties)
./gradlew test            # protocol unit tests
```
