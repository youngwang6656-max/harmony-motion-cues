# Motion Cue Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a personal Android/Kotlin APK that runs on HarmonyOS 4.2 Mate60 Pro+ and shows Apple-like edge motion cue dots above other apps.

**Architecture:** A small Android Gradle project contains one Activity, one foreground overlay Service, focused motion-processing classes, and a custom Canvas overlay View. Sensor processing is separated from rendering so the filter and cue mapping can be unit tested without a device.

**Tech Stack:** Android SDK, Kotlin, Gradle Android plugin, JUnit 4, Android platform sensors, `WindowManager.TYPE_APPLICATION_OVERLAY`, foreground service notification.

## Global Constraints

- Package name: `com.local.motioncue`.
- First implementation target: Android-compatible Kotlin APK for HarmonyOS 4.2 on Huawei Mate60 Pro+.
- Overlay mechanism: Android `TYPE_APPLICATION_OVERLAY`.
- Runtime model: foreground service owns sensors and overlay.
- Default motion mode: manual always-on.
- Default values: `dotRadiusDp = 5`, `edgeInsetDp = 18`, `maxDisplacementDp = 32`, `opacity = 0.75`, `sensitivity = 1.0`, `deadZone = 0.035g`, `attackTimeMs = 90`, `releaseTimeMs = 420`, `manualAlwaysOn = true`, `colorMode = white`.
- No external app runtime dependencies beyond AndroidX-free Kotlin/Android platform APIs for the first version.
- No root-only APIs, no system-signed hidden APIs, and no medical claims in the UI.
- If command-line Android tooling is unavailable, source generation is still complete and APK compilation is done through Android Studio.

---

## File Structure

- Create `settings.gradle.kts`: Gradle project name and `:app` module.
- Create `build.gradle.kts`: Android Gradle Plugin and Kotlin plugin versions.
- Create `app/build.gradle.kts`: Android app module settings, SDK targets, JUnit dependency.
- Create `app/src/main/AndroidManifest.xml`: permissions, Activity, Service, foreground service type.
- Create `app/src/main/res/values/strings.xml`: app and notification labels.
- Create `app/src/main/res/drawable/ic_stat_motion_cue.xml`: simple notification icon.
- Create `app/src/main/java/com/local/motioncue/MainActivity.kt`: native control screen and overlay permission flow.
- Create `app/src/main/java/com/local/motioncue/MotionOverlayService.kt`: foreground service, WindowManager overlay lifecycle, sensor lifecycle.
- Create `app/src/main/java/com/local/motioncue/MotionCueOverlayView.kt`: Canvas edge-dot renderer.
- Create `app/src/main/java/com/local/motioncue/MotionSensorController.kt`: Android sensor listener and motion filtering.
- Create `app/src/main/java/com/local/motioncue/MotionCueModel.kt`: pure Kotlin cue-to-dot mapping.
- Create `app/src/main/java/com/local/motioncue/MotionCueSettings.kt`: settings persistence and defaults.
- Create `app/src/main/java/com/local/motioncue/MotionCueState.kt`: shared state data types.
- Create `app/src/test/java/com/local/motioncue/MotionSensorFilterTest.kt`: filter unit tests.
- Create `app/src/test/java/com/local/motioncue/MotionCueModelTest.kt`: cue mapping unit tests.
- Create `README.md`: build, install, and Mate60 Pro+ setup notes.

---

### Task 1: Gradle Android Project Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_stat_motion_cue.xml`

**Interfaces:**
- Consumes: none.
- Produces: an Android application module with package `com.local.motioncue`, a launchable `MainActivity`, and a declared `MotionOverlayService`.

- [ ] **Step 1: Create Gradle settings**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MotionCue"
include(":app")
```

- [ ] **Step 2: Create root Gradle build**

Create `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

- [ ] **Step 3: Create app Gradle build**

Create `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.local.motioncue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.local.motioncue"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 4: Create manifest**

Create `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <uses-feature android:name="android.hardware.sensor.accelerometer" android:required="true" />
    <uses-feature android:name="android.hardware.sensor.gyroscope" android:required="false" />

    <application
        android:allowBackup="false"
        android:theme="@style/AppTheme"
        android:label="@string/app_name"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".MotionOverlayService"
            android:exported="false"
            android:foregroundServiceType="specialUse" />
    </application>
</manifest>
```

- [ ] **Step 5: Create app resources**

Create `app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Motion Cue</string>
    <string name="notification_channel_name">Motion cue overlay</string>
    <string name="notification_title">Motion cues active</string>
    <string name="notification_text">Edge dots are responding to vehicle motion.</string>
    <string name="notification_stop">Stop</string>
</resources>
```

Create `app/src/main/res/drawable/ic_stat_motion_cue.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M6,7a2,2 0,1 0,0.1,0M18,7a2,2 0,1 0,0.1,0M6,17a2,2 0,1 0,0.1,0M18,17a2,2 0,1 0,0.1,0M12,12a2,2 0,1 0,0.1,0" />
</vector>
```

- [ ] **Step 6: Add app theme**

Create `app/src/main/res/values/styles.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:colorAccent">#111111</item>
    </style>
</resources>
```

- [ ] **Step 7: Run project sync/build check**

Run: `./gradlew :app:assembleDebug`

Expected: If Android Gradle tooling is installed and dependencies are cached or downloadable, the task reaches Kotlin compilation and fails only because source files do not exist yet. If `gradlew` is unavailable, record that local build requires Android Studio or a Gradle installation.

- [ ] **Step 8: Commit**

Run:

```bash
git add settings.gradle.kts build.gradle.kts app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res
git commit -m "chore: add Android project skeleton"
```

---

### Task 2: Shared State, Settings, and Pure Motion Filter

**Files:**
- Create: `app/src/main/java/com/local/motioncue/MotionCueState.kt`
- Create: `app/src/main/java/com/local/motioncue/MotionCueSettings.kt`
- Create: `app/src/main/java/com/local/motioncue/MotionSensorController.kt`
- Create: `app/src/test/java/com/local/motioncue/MotionSensorFilterTest.kt`

**Interfaces:**
- Consumes: Android package `com.local.motioncue`.
- Produces:
  - `data class MotionCueState(val lateral: Float, val longitudinal: Float, val rotation: Float, val confidence: Float)`
  - `data class MotionCueConfig(...)`
  - `class MotionCueSettings(context: Context)`
  - `class MotionSignalFilter(private val configProvider: () -> MotionCueConfig)` with `fun update(rawLateralG: Float, rawLongitudinalG: Float, rawYawRadPerSec: Float, timestampNanos: Long): MotionCueState`.

- [ ] **Step 1: Write filter tests**

Create `app/src/test/java/com/local/motioncue/MotionSensorFilterTest.kt`:

```kotlin
package com.local.motioncue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSensorFilterTest {
    private val config = MotionCueConfig()

    @Test
    fun deadZoneSuppressesTinyMotion() {
        val filter = MotionSignalFilter { config.copy(deadZoneG = 0.05f) }
        val state = filter.update(0.02f, -0.01f, 0.0f, 20_000_000L)
        assertEquals(0.0f, state.lateral, 0.0001f)
        assertEquals(0.0f, state.longitudinal, 0.0001f)
        assertEquals(0.0f, state.confidence, 0.0001f)
    }

    @Test
    fun strongMotionRisesQuicklyAndClamps() {
        val filter = MotionSignalFilter { config.copy(sensitivity = 3.0f, deadZoneG = 0.01f) }
        var state = MotionCueState.Zero
        repeat(10) { index ->
            state = filter.update(0.5f, -0.5f, 0.0f, (index + 1) * 20_000_000L)
        }
        assertTrue(state.lateral > 0.5f)
        assertTrue(state.longitudinal < -0.5f)
        assertTrue(state.lateral <= 1.0f)
        assertTrue(state.longitudinal >= -1.0f)
    }

    @Test
    fun releaseIsSlowerThanAttack() {
        val filter = MotionSignalFilter { config.copy(deadZoneG = 0.01f, attackTimeMs = 80, releaseTimeMs = 600) }
        repeat(8) { index ->
            filter.update(0.25f, 0.0f, 0.0f, (index + 1) * 20_000_000L)
        }
        val beforeRelease = filter.update(0.25f, 0.0f, 0.0f, 200_000_000L)
        val afterRelease = filter.update(0.0f, 0.0f, 0.0f, 220_000_000L)
        assertTrue(afterRelease.lateral > beforeRelease.lateral * 0.75f)
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests com.local.motioncue.MotionSensorFilterTest`

Expected: FAIL because `MotionCueConfig`, `MotionSignalFilter`, and `MotionCueState` are not defined.

- [ ] **Step 3: Implement state and settings**

Create `app/src/main/java/com/local/motioncue/MotionCueState.kt`:

```kotlin
package com.local.motioncue

data class MotionCueState(
    val lateral: Float,
    val longitudinal: Float,
    val rotation: Float,
    val confidence: Float
) {
    companion object {
        val Zero = MotionCueState(0f, 0f, 0f, 0f)
    }
}

enum class CueColorMode {
    WHITE,
    BLACK,
    AUTO
}

data class MotionCueConfig(
    val dotRadiusDp: Float = 5f,
    val edgeInsetDp: Float = 18f,
    val maxDisplacementDp: Float = 32f,
    val opacity: Float = 0.75f,
    val sensitivity: Float = 1f,
    val deadZoneG: Float = 0.035f,
    val attackTimeMs: Int = 90,
    val releaseTimeMs: Int = 420,
    val manualAlwaysOn: Boolean = true,
    val colorMode: CueColorMode = CueColorMode.WHITE
)
```

Create `app/src/main/java/com/local/motioncue/MotionCueSettings.kt`:

```kotlin
package com.local.motioncue

import android.content.Context

class MotionCueSettings(context: Context) {
    private val prefs = context.getSharedPreferences("motion_cue_settings", Context.MODE_PRIVATE)

    fun load(): MotionCueConfig = MotionCueConfig(
        dotRadiusDp = prefs.getFloat("dotRadiusDp", 5f),
        edgeInsetDp = prefs.getFloat("edgeInsetDp", 18f),
        maxDisplacementDp = prefs.getFloat("maxDisplacementDp", 32f),
        opacity = prefs.getFloat("opacity", 0.75f),
        sensitivity = prefs.getFloat("sensitivity", 1f),
        deadZoneG = prefs.getFloat("deadZoneG", 0.035f),
        attackTimeMs = prefs.getInt("attackTimeMs", 90),
        releaseTimeMs = prefs.getInt("releaseTimeMs", 420),
        manualAlwaysOn = prefs.getBoolean("manualAlwaysOn", true),
        colorMode = CueColorMode.valueOf(prefs.getString("colorMode", CueColorMode.WHITE.name) ?: CueColorMode.WHITE.name)
    )

    fun save(config: MotionCueConfig) {
        prefs.edit()
            .putFloat("dotRadiusDp", config.dotRadiusDp)
            .putFloat("edgeInsetDp", config.edgeInsetDp)
            .putFloat("maxDisplacementDp", config.maxDisplacementDp)
            .putFloat("opacity", config.opacity)
            .putFloat("sensitivity", config.sensitivity)
            .putFloat("deadZoneG", config.deadZoneG)
            .putInt("attackTimeMs", config.attackTimeMs)
            .putInt("releaseTimeMs", config.releaseTimeMs)
            .putBoolean("manualAlwaysOn", config.manualAlwaysOn)
            .putString("colorMode", config.colorMode.name)
            .apply()
    }
}
```

- [ ] **Step 4: Implement pure signal filter**

Create `app/src/main/java/com/local/motioncue/MotionSensorController.kt` with the pure filter first:

```kotlin
package com.local.motioncue

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class MotionSignalFilter(private val configProvider: () -> MotionCueConfig) {
    private var lastTimestampNanos: Long = 0L
    private var lateral = 0f
    private var longitudinal = 0f
    private var rotation = 0f

    fun update(
        rawLateralG: Float,
        rawLongitudinalG: Float,
        rawYawRadPerSec: Float,
        timestampNanos: Long
    ): MotionCueState {
        val config = configProvider()
        val dtSeconds = if (lastTimestampNanos == 0L) {
            0.02f
        } else {
            ((timestampNanos - lastTimestampNanos).coerceAtLeast(1L) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        }
        lastTimestampNanos = timestampNanos

        val targetLateral = normalizeAxis(rawLateralG, config)
        val targetLongitudinal = normalizeAxis(rawLongitudinalG, config)
        val targetRotation = (rawYawRadPerSec * 0.35f * config.sensitivity).coerceIn(-1f, 1f)

        lateral = approach(lateral, targetLateral, dtSeconds, config)
        longitudinal = approach(longitudinal, targetLongitudinal, dtSeconds, config)
        rotation = approach(rotation, targetRotation, dtSeconds, config)

        val confidence = max(abs(lateral), abs(longitudinal)).coerceIn(0f, 1f)
        return MotionCueState(lateral, longitudinal, rotation, confidence)
    }

    private fun normalizeAxis(rawG: Float, config: MotionCueConfig): Float {
        val magnitude = abs(rawG)
        if (magnitude < config.deadZoneG) return 0f
        val signed = rawG.sign() * (magnitude - config.deadZoneG)
        return (signed * 5.0f * config.sensitivity).coerceIn(-1f, 1f)
    }

    private fun approach(current: Float, target: Float, dtSeconds: Float, config: MotionCueConfig): Float {
        val timeMs = if (abs(target) > abs(current)) config.attackTimeMs else config.releaseTimeMs
        val alpha = 1f - exp((-dtSeconds * 1000f / timeMs.coerceAtLeast(1)).toDouble()).toFloat()
        val next = current + (target - current) * alpha
        return if (abs(next) < 0.001f) 0f else next.coerceIn(-1f, 1f)
    }

    private fun Float.sign(): Float = if (this < 0f) -1f else 1f
}
```

- [ ] **Step 5: Run tests to verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests com.local.motioncue.MotionSensorFilterTest`

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
git add app/src/main/java/com/local/motioncue/MotionCueState.kt app/src/main/java/com/local/motioncue/MotionCueSettings.kt app/src/main/java/com/local/motioncue/MotionSensorController.kt app/src/test/java/com/local/motioncue/MotionSensorFilterTest.kt
git commit -m "feat: add motion cue signal filter"
```

---

### Task 3: Cue Model and Edge Dot Mapping

**Files:**
- Create: `app/src/main/java/com/local/motioncue/MotionCueModel.kt`
- Create: `app/src/test/java/com/local/motioncue/MotionCueModelTest.kt`

**Interfaces:**
- Consumes: `MotionCueState`, `MotionCueConfig`.
- Produces:
  - `data class CueDot(val baseX: Float, val baseY: Float, val x: Float, val y: Float, val radius: Float, val alpha: Float)`
  - `class MotionCueModel` with `fun dots(widthPx: Int, heightPx: Int, density: Float, state: MotionCueState, config: MotionCueConfig): List<CueDot>`.

- [ ] **Step 1: Write mapping tests**

Create `app/src/test/java/com/local/motioncue/MotionCueModelTest.kt`:

```kotlin
package com.local.motioncue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionCueModelTest {
    @Test
    fun createsDotsOnAllEdges() {
        val dots = MotionCueModel().dots(
            widthPx = 1080,
            heightPx = 2400,
            density = 3f,
            state = MotionCueState.Zero,
            config = MotionCueConfig()
        )
        assertEquals(28, dots.size)
    }

    @Test
    fun lateralMotionMovesSideDots() {
        val model = MotionCueModel()
        val neutral = model.dots(1080, 2400, 3f, MotionCueState.Zero, MotionCueConfig())
        val moved = model.dots(1080, 2400, 3f, MotionCueState(lateral = 1f, longitudinal = 0f, rotation = 0f, confidence = 1f), MotionCueConfig())
        val neutralLeft = neutral.first()
        val movedLeft = moved.first()
        assertTrue(movedLeft.x > neutralLeft.x)
    }

    @Test
    fun opacityIsAppliedToAllDots() {
        val dots = MotionCueModel().dots(1080, 2400, 3f, MotionCueState.Zero, MotionCueConfig(opacity = 0.6f))
        assertTrue(dots.all { it.alpha == 0.6f })
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests com.local.motioncue.MotionCueModelTest`

Expected: FAIL because `MotionCueModel` and `CueDot` are not defined.

- [ ] **Step 3: Implement model**

Create `app/src/main/java/com/local/motioncue/MotionCueModel.kt`:

```kotlin
package com.local.motioncue

import kotlin.math.max

data class CueDot(
    val baseX: Float,
    val baseY: Float,
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float
)

class MotionCueModel {
    fun dots(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        state: MotionCueState,
        config: MotionCueConfig
    ): List<CueDot> {
        if (widthPx <= 0 || heightPx <= 0) return emptyList()
        val radius = config.dotRadiusDp * density
        val inset = config.edgeInsetDp * density
        val maxMove = config.maxDisplacementDp * density
        val horizontalCount = 7
        val verticalCount = 7
        val dots = mutableListOf<CueDot>()

        fun addDot(baseX: Float, baseY: Float, edgeFactorX: Float, edgeFactorY: Float, cornerFactor: Float) {
            val dx = state.lateral * maxMove * edgeFactorX + state.rotation * maxMove * 0.25f * cornerFactor
            val dy = state.longitudinal * maxMove * edgeFactorY - state.rotation * maxMove * 0.18f * cornerFactor
            dots += CueDot(
                baseX = baseX,
                baseY = baseY,
                x = baseX + dx,
                y = baseY + dy,
                radius = radius,
                alpha = config.opacity.coerceIn(0f, 1f)
            )
        }

        repeat(verticalCount) { index ->
            val t = (index + 1f) / (verticalCount + 1f)
            val y = heightPx * t
            val corner = (t - 0.5f) * 2f
            addDot(inset, y, 1f, 0.25f, corner)
            addDot(widthPx - inset, y, 1f, 0.25f, -corner)
        }

        repeat(horizontalCount) { index ->
            val t = (index + 1f) / (horizontalCount + 1f)
            val x = widthPx * t
            val corner = (t - 0.5f) * 2f
            addDot(x, inset, 0.25f, 1f, -corner)
            addDot(x, heightPx - inset, 0.25f, 1f, corner)
        }

        return dots
    }
}
```

- [ ] **Step 4: Run tests to verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests com.local.motioncue.MotionCueModelTest`

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```bash
git add app/src/main/java/com/local/motioncue/MotionCueModel.kt app/src/test/java/com/local/motioncue/MotionCueModelTest.kt
git commit -m "feat: map motion cues to edge dots"
```

---

### Task 4: Overlay View Renderer

**Files:**
- Create: `app/src/main/java/com/local/motioncue/MotionCueOverlayView.kt`

**Interfaces:**
- Consumes: `MotionCueModel`, `MotionCueSettings`, `MotionCueState`, `MotionCueConfig`.
- Produces: `class MotionCueOverlayView(context: Context) : View` with `fun updateState(next: MotionCueState)` and `fun updateConfig(next: MotionCueConfig)`.

- [ ] **Step 1: Implement Canvas renderer**

Create `app/src/main/java/com/local/motioncue/MotionCueOverlayView.kt`:

```kotlin
package com.local.motioncue

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class MotionCueOverlayView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val model = MotionCueModel()
    private var state = MotionCueState.Zero
    private var config = MotionCueConfig()

    init {
        setWillNotDraw(false)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun updateState(next: MotionCueState) {
        state = next
        postInvalidateOnAnimation()
    }

    fun updateConfig(next: MotionCueConfig) {
        config = next
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val color = when (config.colorMode) {
            CueColorMode.WHITE -> Color.WHITE
            CueColorMode.BLACK -> Color.BLACK
            CueColorMode.AUTO -> Color.WHITE
        }
        paint.color = color
        val dots = model.dots(width, height, resources.displayMetrics.density, state, config)
        for (dot in dots) {
            paint.alpha = (dot.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(dot.x, dot.y, dot.radius, paint)
        }
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS if previous tasks are complete and Android tooling is available.

- [ ] **Step 3: Commit**

Run:

```bash
git add app/src/main/java/com/local/motioncue/MotionCueOverlayView.kt
git commit -m "feat: render motion cue overlay dots"
```

---

### Task 5: Android Sensor Controller

**Files:**
- Modify: `app/src/main/java/com/local/motioncue/MotionSensorController.kt`

**Interfaces:**
- Consumes: `MotionSignalFilter`, `MotionCueState`.
- Produces: `class MotionSensorController(context: Context, configProvider: () -> MotionCueConfig, onState: (MotionCueState) -> Unit)` with `fun start()` and `fun stop()`.

- [ ] **Step 1: Extend MotionSensorController with Android sensor bridge**

Replace `app/src/main/java/com/local/motioncue/MotionSensorController.kt` with:

```kotlin
package com.local.motioncue

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

class MotionSensorController(
    context: Context,
    private val configProvider: () -> MotionCueConfig,
    private val onState: (MotionCueState) -> Unit
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val filter = MotionSignalFilter(configProvider)
    private var gravity = FloatArray(3)
    private var latestYawRate = 0f
    private var running = false

    fun start() {
        if (running) return
        running = true
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
        gravity = FloatArray(3)
        latestYawRate = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> latestYawRate = event.values[2]
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun handleAccelerometer(event: SensorEvent) {
        val alpha = 0.88f
        gravity[0] = alpha * gravity[0] + (1f - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1f - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1f - alpha) * event.values[2]

        val linearX = (event.values[0] - gravity[0]) / SensorManager.GRAVITY_EARTH
        val linearY = (event.values[1] - gravity[1]) / SensorManager.GRAVITY_EARTH

        val rotated = rotateToDisplay(linearX, linearY)
        onState(filter.update(rotated.first, rotated.second, latestYawRate, event.timestamp))
    }

    private fun rotateToDisplay(x: Float, y: Float): Pair<Float, Float> {
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_90 -> Pair(-y, x)
            Surface.ROTATION_180 -> Pair(-x, -y)
            Surface.ROTATION_270 -> Pair(y, -x)
            else -> Pair(x, y)
        }
    }
}

class MotionSignalFilter(private val configProvider: () -> MotionCueConfig) {
    private var lastTimestampNanos: Long = 0L
    private var lateral = 0f
    private var longitudinal = 0f
    private var rotation = 0f

    fun update(
        rawLateralG: Float,
        rawLongitudinalG: Float,
        rawYawRadPerSec: Float,
        timestampNanos: Long
    ): MotionCueState {
        val config = configProvider()
        val dtSeconds = if (lastTimestampNanos == 0L) {
            0.02f
        } else {
            ((timestampNanos - lastTimestampNanos).coerceAtLeast(1L) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        }
        lastTimestampNanos = timestampNanos

        val targetLateral = normalizeAxis(rawLateralG, config)
        val targetLongitudinal = normalizeAxis(rawLongitudinalG, config)
        val targetRotation = (rawYawRadPerSec * 0.35f * config.sensitivity).coerceIn(-1f, 1f)

        lateral = approach(lateral, targetLateral, dtSeconds, config)
        longitudinal = approach(longitudinal, targetLongitudinal, dtSeconds, config)
        rotation = approach(rotation, targetRotation, dtSeconds, config)

        val confidence = max(abs(lateral), abs(longitudinal)).coerceIn(0f, 1f)
        return MotionCueState(lateral, longitudinal, rotation, confidence)
    }

    private fun normalizeAxis(rawG: Float, config: MotionCueConfig): Float {
        val magnitude = abs(rawG)
        if (magnitude < config.deadZoneG) return 0f
        val signed = rawG.sign() * (magnitude - config.deadZoneG)
        return (signed * 5.0f * config.sensitivity).coerceIn(-1f, 1f)
    }

    private fun approach(current: Float, target: Float, dtSeconds: Float, config: MotionCueConfig): Float {
        val timeMs = if (abs(target) > abs(current)) config.attackTimeMs else config.releaseTimeMs
        val alpha = 1f - exp((-dtSeconds * 1000f / timeMs.coerceAtLeast(1)).toDouble()).toFloat()
        val next = current + (target - current) * alpha
        return if (abs(next) < 0.001f) 0f else next.coerceIn(-1f, 1f)
    }

    private fun Float.sign(): Float = if (this < 0f) -1f else 1f
}
```

- [ ] **Step 2: Run unit tests again**

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS. The Android sensor bridge should not break pure filter tests.

- [ ] **Step 3: Commit**

Run:

```bash
git add app/src/main/java/com/local/motioncue/MotionSensorController.kt
git commit -m "feat: connect motion filter to Android sensors"
```

---

### Task 6: Foreground Overlay Service

**Files:**
- Create: `app/src/main/java/com/local/motioncue/MotionOverlayService.kt`

**Interfaces:**
- Consumes: `MotionCueOverlayView`, `MotionSensorController`, `MotionCueSettings`.
- Produces: `class MotionOverlayService : Service` with actions `ACTION_START` and `ACTION_STOP`.

- [ ] **Step 1: Implement service**

Create `app/src/main/java/com/local/motioncue/MotionOverlayService.kt`:

```kotlin
package com.local.motioncue

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

class MotionOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settings: MotionCueSettings
    private var overlayView: MotionCueOverlayView? = null
    private var sensorController: MotionSensorController? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settings = MotionCueSettings(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopOverlay()
            else -> startOverlay()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (overlayView == null) {
            overlayView = MotionCueOverlayView(this).also { view ->
                view.updateConfig(settings.load())
                windowManager.addView(view, overlayLayoutParams())
            }
        }
        if (sensorController == null) {
            sensorController = MotionSensorController(
                context = this,
                configProvider = { settings.load() },
                onState = { state -> overlayView?.updateState(state) }
            ).also { it.start() }
        }
    }

    private fun stopOverlay() {
        sensorController?.stop()
        sensorController = null
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stopIntent = Intent(this, MotionOverlayService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_stat_motion_cue)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .addAction(R.drawable.ic_stat_motion_cue, getString(R.string.notification_stop), stopPendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.local.motioncue.action.START"
        const val ACTION_STOP = "com.local.motioncue.action.STOP"
        private const val CHANNEL_ID = "motion_cue_overlay"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, MotionOverlayService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MotionOverlayService::class.java).setAction(ACTION_STOP))
        }
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 3: Commit**

Run:

```bash
git add app/src/main/java/com/local/motioncue/MotionOverlayService.kt
git commit -m "feat: add foreground overlay service"
```

---

### Task 7: Native Control Activity

**Files:**
- Create: `app/src/main/java/com/local/motioncue/MainActivity.kt`

**Interfaces:**
- Consumes: `MotionCueSettings`, `MotionOverlayService`.
- Produces: launchable Activity with permission button, enable switch, and settings sliders.

- [ ] **Step 1: Implement activity UI without XML layout**

Create `app/src/main/java/com/local/motioncue/MainActivity.kt`:

```kotlin
package com.local.motioncue

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var settings: MotionCueSettings
    private lateinit var config: MotionCueConfig
    private lateinit var permissionStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = MotionCueSettings(this)
        config = settings.load()
        maybeRequestNotifications()
        render()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 56, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Motion Cue"
            textSize = 28f
        }
        root.addView(title)

        permissionStatus = TextView(this).apply {
            textSize = 15f
            setPadding(0, 20, 0, 12)
        }
        root.addView(permissionStatus)

        root.addView(Button(this).apply {
            text = "Open overlay permission"
            setOnClickListener { openOverlaySettings() }
        })

        root.addView(Switch(this).apply {
            text = "Enable motion cues"
            textSize = 18f
            setPadding(0, 28, 0, 28)
            setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    if (Settings.canDrawOverlays(this@MainActivity)) MotionOverlayService.start(this@MainActivity) else openOverlaySettings()
                } else {
                    MotionOverlayService.stop(this@MainActivity)
                }
            }
        })

        addSlider(root, "Sensitivity", 50, 200, (config.sensitivity * 100).toInt()) {
            updateConfig(config.copy(sensitivity = it / 100f))
        }
        addSlider(root, "Dot size", 3, 10, config.dotRadiusDp.toInt()) {
            updateConfig(config.copy(dotRadiusDp = it.toFloat()))
        }
        addSlider(root, "Opacity", 35, 100, (config.opacity * 100).toInt()) {
            updateConfig(config.copy(opacity = it / 100f))
        }
        addSlider(root, "Movement", 16, 64, config.maxDisplacementDp.toInt()) {
            updateConfig(config.copy(maxDisplacementDp = it.toFloat()))
        }

        root.addView(Button(this).apply {
            text = "Use white dots"
            setOnClickListener { updateConfig(config.copy(colorMode = CueColorMode.WHITE)) }
        })
        root.addView(Button(this).apply {
            text = "Use black dots"
            setOnClickListener { updateConfig(config.copy(colorMode = CueColorMode.BLACK)) }
        })

        setContentView(root)
        updatePermissionStatus()
    }

    private fun addSlider(root: LinearLayout, label: String, min: Int, max: Int, value: Int, onChange: (Int) -> Unit) {
        val text = TextView(this).apply {
            this.text = "$label: $value"
            textSize = 16f
            setPadding(0, 20, 0, 4)
        }
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = value.coerceIn(min, max) - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val next = min + progress
                    text.text = "$label: $next"
                    if (fromUser) onChange(next)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        root.addView(text)
        root.addView(seek)
    }

    private fun updateConfig(next: MotionCueConfig) {
        config = next
        settings.save(next)
    }

    private fun updatePermissionStatus() {
        if (::permissionStatus.isInitialized) {
            permissionStatus.text = if (Settings.canDrawOverlays(this)) {
                "Overlay permission: enabled"
            } else {
                "Overlay permission: disabled"
            }
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }
}
```

- [ ] **Step 2: Compile check**

Run: `./gradlew :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 3: Commit**

Run:

```bash
git add app/src/main/java/com/local/motioncue/MainActivity.kt
git commit -m "feat: add motion cue control activity"
```

---

### Task 8: README and Device Verification

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: complete app project.
- Produces: build and install instructions for the user's Mate60 Pro+.

- [ ] **Step 1: Write README**

Create `README.md`:

```markdown
# Motion Cue

Personal motion cue overlay for HarmonyOS 4.2 / Mate60 Pro+ using the Android compatibility layer.

## Build

Open this folder in Android Studio and run:

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

With the phone connected and USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If using Huawei tooling instead of adb, install the same APK through DevEco Studio or the system package installer.

## First Run

1. Open Motion Cue.
2. Tap Open overlay permission.
3. Enable drawing over other apps for Motion Cue.
4. Return to Motion Cue.
5. Turn on Enable motion cues.
6. Switch to another app and confirm dots remain on the screen edges.

## Tuning

- Increase Sensitivity if the dots barely move in a car.
- Decrease Sensitivity if the dots react to hand tremor.
- Increase Movement for a stronger visual cue.
- Use white dots on dark apps and black dots on light apps.
```

- [ ] **Step 2: Run available verification**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: PASS if Android tooling and dependencies are available.

- [ ] **Step 3: Install on device when adb is available**

Run: `adb devices`

Expected: Mate60 Pro+ appears as `device`.

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Expected: `Success`.

- [ ] **Step 4: Manual phone verification**

On Mate60 Pro+:

1. Launch Motion Cue.
2. Grant overlay permission.
3. Enable motion cues.
4. Press Home.
5. Open a normal app.
6. Confirm edge dots are visible and taps still pass through.
7. Move the phone left/right and forward/back.
8. Confirm dots move smoothly and return slowly.
9. Stop from the notification.
10. Confirm dots disappear.

- [ ] **Step 5: Commit**

Run:

```bash
git add README.md
git commit -m "docs: add build and device verification notes"
```

---

## Plan Self-Review

- Spec coverage: The plan covers Android/Kotlin APK, package name, foreground service, overlay window, sensors, signal filter, dot renderer, manual always-on behavior, settings, README, unit tests, and device verification.
- Completion scan: The task steps contain concrete files, code, commands, and expected results rather than unresolved markers.
- Type consistency: `MotionCueConfig`, `MotionCueState`, `MotionSignalFilter`, `MotionCueModel`, `MotionCueOverlayView`, `MotionSensorController`, `MotionCueSettings`, and `MotionOverlayService` signatures are defined before or inside the tasks that consume them.
