package player.phonograph.ui.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import player.phonograph.R
import player.phonograph.ui.activities.base.AbsBaseActivity
import player.phonograph.ui.fragments.SettingsFragment
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

class SettingsActivity : AbsBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        Themer.setActivityToolbarColorAuto(this, toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SettingsFragment()).commit()
        } else {
            val frag =
                supportFragmentManager.findFragmentById(R.id.content_frame) as SettingsFragment?
            frag?.invalidateSettings()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
