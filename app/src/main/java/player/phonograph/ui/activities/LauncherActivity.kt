/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.settings.Setting
import androidx.activity.ComponentActivity
import android.content.Intent
import android.os.Bundle

class LauncherActivity : ComponentActivity() {

    private fun gotoMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun gotoIntro() {
        startActivity(Intent(this, PhonographIntroActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PrerequisiteSetting.instance(this).introShown) {
            gotoIntro()
        } else {
            gotoMainActivity()
        }
    }
}