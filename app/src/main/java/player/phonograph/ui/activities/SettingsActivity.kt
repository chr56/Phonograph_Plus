package player.phonograph.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.Menu.NONE
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.App
import player.phonograph.R
import player.phonograph.provider.DatabaseManger
import player.phonograph.ui.fragments.SettingsFragment
import player.phonograph.util.Util
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

class SettingsActivity : ToolbarActivity() {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { m ->
            m.add(NONE, R.id.action_export_data, 0, "Export...").also {
                it.setShowAsAction(SHOW_AS_ACTION_NEVER)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_export_data -> {
                CoroutineScope(Dispatchers.IO).launch {
                    DatabaseManger(App.instance).exportDatabases()
                    Util.coroutineToast(App.instance, R.string.success)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
