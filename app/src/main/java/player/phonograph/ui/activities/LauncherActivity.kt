/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import lib.phonograph.activity.ToolbarActivity
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.settings.Setting
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Bundle
import android.view.View

class LauncherActivity : ToolbarActivity() {

    private fun gotoMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun gotoIntro() {
        startActivity(Intent(this, PhonographIntroActivity::class.java))
        finish()
    }

    private lateinit var mainContent: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Setting.instance.introShown) {
            gotoIntro()
        } else {
            if (SDK_INT >= N_MR1) {
                DynamicShortcutManager(this).updateDynamicShortcuts()
            }
            gotoMainActivity()
        }
    }
}