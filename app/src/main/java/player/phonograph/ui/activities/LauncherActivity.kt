/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import lib.phonograph.activity.ToolbarActivity
import mt.util.color.invertColor
import mt.util.color.primaryDisabledTextColor
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.intro.AppIntroActivity
import player.phonograph.util.permissions.NonGrantedPermission
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.View

class LauncherActivity : ToolbarActivity() {

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private lateinit var mainContent: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Setting.instance.introShown) {
            setContentView(generateView())
            showIntro()
        } else {
            if (SDK_INT >= N_MR1) {
                DynamicShortcutManager(this).updateDynamicShortcuts()
            }
            startMainActivity()
        }
    }

    private fun generateView(): View {
        return CoordinatorLayout(this).apply {
            setBackgroundColor(invertColor(primaryDisabledTextColor()))
        }.also { mainContent = it }
    }

    private fun showIntro() {
        Setting.instance.introShown = true
        startActivityForResult(Intent(this, AppIntroActivity::class.java), APP_INTRO_REQUEST)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_INTRO_REQUEST) {
            val permissions =
                if (SDK_INT >= TIRAMISU)
                    arrayOf(POST_NOTIFICATIONS, READ_MEDIA_AUDIO)
                else arrayOf(READ_EXTERNAL_STORAGE)
            askForPermission(permissions)
        }
    }

    private fun askForPermission(permissions: Array<String>) {
        requestPermissionImpl(permissions) { map ->
            val allGranted = map.values.reduce { acc, b -> if (!acc) false else b }
            if (!allGranted) {
                val denied = map.filter { !it.value }.map { it.key }
                notifyPermissionDeniedUser(denied.map { NonGrantedPermission(it) }) {
                    askForPermission(denied.toTypedArray())
                    startMainActivity()
                }
            } else {
                startMainActivity()
            }
        }
    }

    override val snackBarContainer: View get() = mainContent

    companion object {
        private const val APP_INTRO_REQUEST = 100
    }

}