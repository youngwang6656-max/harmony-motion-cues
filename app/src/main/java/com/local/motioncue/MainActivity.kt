package com.local.motioncue

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var settings: MotionCueSettings
    private lateinit var config: MotionCueConfig
    private lateinit var permissionStatus: TextView
    private lateinit var enableSwitch: Switch

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
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        root.addView(TextView(this).apply {
            text = "Motion Cue"
            textSize = 28f
        })

        permissionStatus = TextView(this).apply {
            textSize = 15f
            setPadding(0, 20, 0, 12)
        }
        root.addView(permissionStatus)

        root.addView(Button(this).apply {
            text = "Open overlay permission"
            setOnClickListener { openOverlaySettings() }
        })

        enableSwitch = Switch(this).apply {
            text = "Enable motion cues"
            textSize = 18f
            setPadding(0, 28, 0, 28)
            setOnCheckedChangeListener { button, checked ->
                if (checked) {
                    if (Settings.canDrawOverlays(this@MainActivity)) {
                        MotionOverlayService.start(this@MainActivity)
                    } else {
                        button.isChecked = false
                        openOverlaySettings()
                    }
                } else {
                    MotionOverlayService.stop(this@MainActivity)
                }
            }
        }
        root.addView(enableSwitch)

        addSlider(root, "Sensitivity", 50, 220, (config.sensitivity * 100).toInt()) {
            updateConfig(config.copy(sensitivity = it / 100f))
        }
        addSlider(root, "Dot size", 3, 12, config.dotRadiusDp.toInt()) {
            updateConfig(config.copy(dotRadiusDp = it.toFloat()))
        }
        addSlider(root, "Opacity", 35, 100, (config.opacity * 100).toInt()) {
            updateConfig(config.copy(opacity = it / 100f))
        }
        addSlider(root, "Movement", 16, 72, config.maxDisplacementDp.toInt()) {
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

        setContentView(ScrollView(this).apply { addView(root) })
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
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }
}
