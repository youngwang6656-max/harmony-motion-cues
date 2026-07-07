# Motion Cue Overlay Design

## Goal

Build a personal-use motion cue overlay for HarmonyOS 4.2 on Huawei Mate60 Pro+ that closely matches Apple's Vehicle Motion Cues behavior: when the phone is used in a moving vehicle, animated dots near the screen edges provide peripheral visual motion references to reduce sensory conflict.

The first implementation will be an Android-compatible Kotlin APK because HarmonyOS 4.2 on Mate60 Pro+ can run Android applications, and Android's overlay window and sensor APIs are the most practical route for a cross-app, always-visible personal tool.

## Product Scope

The app provides a full-screen transparent overlay above other apps. It renders sparse black or white dots along the screen perimeter. The dots move smoothly in response to vehicle acceleration, braking, and turning while staying visually quiet enough not to block reading or app interaction.

The first version prioritizes usefulness and native-feeling behavior over battery optimization, store compliance, or distribution constraints.

In scope:

- Cross-app overlay using Android `TYPE_APPLICATION_OVERLAY`.
- Foreground service to keep the overlay and sensors alive.
- Accelerometer, gyroscope, and rotation-vector based motion estimation.
- Edge-only dot rendering with adjustable sensitivity, dot size, opacity, and color mode.
- Manual always-on mode, plus optional simple auto-detection hooks.
- Touch pass-through as far as the platform allows.

Out of scope for the first version:

- App store readiness.
- Long-term background battery tuning.
- System-signed hidden APIs or root-only hooks.
- Medical claims or guaranteed motion-sickness prevention.

## Evidence Basis

Apple describes Vehicle Motion Cues as animated dots on screen edges that use built-in sensors to detect vehicle motion and reduce sensory conflict while using iPhone or iPad in a moving vehicle.

Android provides motion sensors including accelerometer, gyroscope, gravity, linear acceleration, and rotation vector sensors. These are sufficient for a practical phone-coordinate motion estimate.

Android overlay windows can be implemented with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, gated by the user's "draw over other apps" permission.

## Architecture

### MainActivity

MainActivity is the control surface. It checks overlay permission, starts and stops the overlay service, and exposes the core settings.

Controls:

- Enable motion cues.
- Open overlay permission screen if needed.
- Sensitivity slider.
- Dot size slider.
- Opacity slider.
- Color mode selector: white, black, automatic.
- Motion mode selector: always on, auto detect vehicle.

MainActivity does not own sensor subscriptions or overlay windows. It only writes settings and sends start/stop commands to the service.

### MotionOverlayService

MotionOverlayService is a foreground service. It owns the overlay lifecycle and keeps the app active while motion cues are enabled.

Responsibilities:

- Create the transparent full-screen overlay window.
- Attach MotionCueOverlayView to WindowManager.
- Start MotionSensorController.
- Stop sensors and remove the overlay when disabled.
- Show a persistent notification with a stop action.

Window behavior:

- Full screen.
- Transparent background.
- Not focusable.
- Not touch modal.
- Layout covers display cutouts where possible.
- Does not intercept touches unless HarmonyOS modifies Android behavior.

### MotionSensorController

MotionSensorController converts raw device sensor streams into a stable motion cue vector.

Preferred sensors:

- Rotation vector for phone attitude.
- Accelerometer for total acceleration.
- Gyroscope for short-term turn response.
- Linear acceleration when available, as a secondary signal.

Processing:

1. Convert raw acceleration into screen coordinates using the current display rotation and rotation-vector attitude.
2. Remove gravity using sensor fusion or a low-pass gravity estimate.
3. Apply a high-frequency suppression filter to reduce hand tremor and road vibration.
4. Preserve vehicle-scale motion changes, roughly the low-frequency range that users perceive as acceleration, braking, and turning.
5. Apply a dead zone so tiny movements do not animate the dots.
6. Use asymmetric response: fast attack for real motion and slower release to return to center.
7. Clamp output to a maximum cue displacement to avoid distracting jumps.

Output:

```text
MotionCueState(
  lateral: Float,      // left/right turn cue, normalized -1..1
  longitudinal: Float, // acceleration/braking cue, normalized -1..1
  rotation: Float,     // subtle yaw/turn cue, normalized -1..1
  confidence: Float    // vehicle-like motion estimate, 0..1
)
```

### MotionCueModel

MotionCueModel maps MotionCueState to per-dot offsets.

Rules:

- Horizontal acceleration shifts left and right edge dots horizontally and slightly vertically.
- Longitudinal acceleration shifts top and bottom edge dots vertically.
- Rotation adds a small differential offset, stronger near corners.
- Offsets are intentionally understated, with default maximum movement around 32dp.
- Dot positions ease toward their target every frame, but target computation remains low latency.

### MotionCueOverlayView

MotionCueOverlayView is a custom Canvas view that renders the dots.

Rendering:

- 5 to 8 dots per edge, depending on screen size.
- Dot radius default 5dp.
- Edge inset default 18dp to avoid accidental overlap with system gestures.
- Opacity default 75%.
- Color can be white, black, or automatic.
- Automatic mode starts simple: choose white or black based on user setting fallback. Later versions can sample screen brightness only if a reliable API exists.

The overlay contains no text while active. It must stay quiet and peripheral.

## Vehicle Detection

The first version defaults to manual always-on because it is more useful for personal testing and avoids false negatives.

Auto-detect can be added as a setting:

- Detect sustained low-frequency acceleration above a threshold.
- Require several seconds of vehicle-like motion before showing cues.
- Hide only after a longer stable period.
- Do not depend on location or speed APIs in the first version.

## Failure Handling

If overlay permission is missing:

- MainActivity shows a direct action to open the permission settings.
- The service does not start the overlay.

If required sensors are unavailable:

- Use available fallbacks in this order: rotation vector plus accelerometer, accelerometer plus gyroscope, accelerometer only.
- Show a small warning in MainActivity, not in the overlay.

If the overlay cannot be added:

- Stop the service.
- Log the platform exception.
- Return to MainActivity with a clear status.

If HarmonyOS blocks overlays on secure screens:

- Accept the system behavior. The overlay resumes where the platform permits.

## Test Plan

Local unit tests:

- Filter dead-zone behavior.
- Fast attack and slow release response.
- Clamp behavior for large acceleration spikes.
- MotionCueModel mapping signs for left/right and acceleration/braking.

Device checks on Mate60 Pro+:

- Permission flow opens and returns correctly.
- Overlay appears over launcher and normal third-party apps.
- Overlay does not block normal taps and scrolling.
- Sensor response is visible when tilting and translating the phone.
- Stop action removes the overlay and notification.
- Changing settings updates the overlay without restarting the phone.

Road-test tuning:

- Passenger test in city traffic.
- Adjust sensitivity and release constants to reduce lag.
- Confirm dots remain peripheral and do not interfere with reading.

## Build Target

Project type:

- Android Gradle project.
- Kotlin source.
- Minimum SDK compatible with Mate60 Pro+ Android compatibility layer.
- No external runtime dependencies unless the local toolchain requires them.

Package name:

```text
com.local.motioncue
```

## Initial Defaults

```text
dotRadiusDp = 5
edgeInsetDp = 18
maxDisplacementDp = 32
opacity = 0.75
sensitivity = 1.0
deadZone = 0.035g
attackTimeMs = 90
releaseTimeMs = 420
manualAlwaysOn = true
colorMode = white
```

## Open Implementation Constraint

The current workspace has no existing project and no detected Android or HarmonyOS command-line build tools in PATH. The implementation will create a complete project structure in this workspace. If Gradle or Android SDK is unavailable locally, source generation can still be completed, but APK compilation will require Android Studio or installing the command-line toolchain.
