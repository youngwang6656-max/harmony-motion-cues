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
            text = getString(R.string.main_title)
            textSize = 28f
        })

        permissionStatus = TextView(this).apply {
            textSize = 15f
            setPadding(0, 20, 0, 12)
        }
        root.addView(permissionStatus)

        root.addView(Button(this).apply {
            text = getString(R.string.open_overlay_permission)
            setOnClickListener { openOverlaySettings() }
        })

        enableSwitch = Switch(this).apply {
            text = getString(R.string.enable_motion_cues)
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

        addSlider(root, getString(R.string.sensitivity), 50, 220, (config.sensitivity * 100).toInt()) {
            updateConfig(config.copy(sensitivity = it / 100f))
        }
        addSlider(root, getString(R.string.dot_size), 3, 12, config.dotRadiusDp.toInt()) {
            updateConfig(config.copy(dotRadiusDp = it.toFloat()))
        }
        addSlider(root, getString(R.string.opacity), 35, 100, (config.opacity * 100).toInt()) {
            updateConfig(config.copy(opacity = it / 100f))
        }
        addSlider(root, getString(R.string.movement), 16, 72, config.maxDisplacementDp.toInt()) {
            updateConfig(config.copy(maxDisplacementDp = it.toFloat()))
        }

        root.addView(Button(this).apply {
            text = getString(R.string.use_auto_dots)
            setOnClickListener { updateConfig(config.copy(colorMode = CueColorMode.AUTO)) }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.use_white_dots)
            setOnClickListener { updateConfig(config.copy(colorMode = CueColorMode.WHITE)) }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.use_black_dots)
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
                getString(R.string.overlay_permission_enabled)
            } else {
                getString(R.string.overlay_permission_disabled)
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
