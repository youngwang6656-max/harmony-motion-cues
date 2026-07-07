# Motion Cue

Personal motion cue overlay for HarmonyOS 4.2 / Mate60 Pro+ using the Android compatibility layer.

## Build

Open this folder in Android Studio and run:

```bash
gradle :app:assembleDebug
```

If Android Studio prompts for Gradle sync, accept the Android Gradle Plugin and SDK downloads.

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

With the phone connected and USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If `adb` is not in PATH, install the APK through Android Studio, DevEco Studio, or the system package installer.

## First Run

1. Open Motion Cue.
2. Tap Open overlay permission.
3. Enable drawing over other apps for Motion Cue.
4. Return to Motion Cue.
5. Turn on Enable motion cues.
6. Switch to another app and confirm dots remain on the screen edges.

## Tuning

- Increase Sensitivity if the dots barely move in a car.
- Decrease Sensitivity if the dots react to hand movement.
- Increase Movement for a stronger visual cue.
- Use white dots on dark apps and black dots on light apps.

## Notes

The overlay is intentionally touch-through, so it should not block taps or scrolling in other apps. HarmonyOS may still suppress overlays on secure screens.
