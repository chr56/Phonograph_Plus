/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.App
import player.phonograph.mechanism.migrate.migrate
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.util.currentVersionCode
import android.app.Activity
import android.content.Intent
import android.os.Bundle

class LauncherActivity : Activity() {

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
            checkMigrate()
            gotoMainActivity()
        }
    }

    private fun checkMigrate() {
        val currentVersion = currentVersionCode(this)
        val previousVersion = PrerequisiteSetting.instance(this).previousVersion
        migrate(App.instance, previousVersion, currentVersion)
    }
}