/*
 * Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.auxiliary

import player.phonograph.mechanism.migrate.MigrationManager
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.ui.modules.main.MainActivity
import android.app.Activity
import android.content.Intent
import android.os.Bundle

class LauncherActivity : Activity() {

    private fun gotoMainActivity() {
        startActivity(MainActivity.launchingIntent(this))
        finish()
    }

    private fun gotoIntro() {
        startActivity(Intent(this, PhonographIntroActivity::class.java))
        finish()
    }

    private fun gotoMigration() {
        startActivity(Intent(this, MigrationActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!PrerequisiteSetting.instance(this).introShown) {
            gotoIntro()
        } else {
            if (MigrationManager.shouldMigration(this)) {
                gotoMigration()
            } else {
                gotoMainActivity()
            }
        }
    }
}