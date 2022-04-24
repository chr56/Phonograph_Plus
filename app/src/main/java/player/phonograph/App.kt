package player.phonograph

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import kotlin.system.exitProcess
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.ui.activities.CrashActivity
import util.mdcolor.pref.ThemeColor

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class App : Application() {

    lateinit var lyricsService: StatusBarLyric.API.StatusBarLyric

    override fun onCreate() {
        if (BuildConfig.DEBUG) Log.v("Metrics", "${System.currentTimeMillis().mod(10000000)} App.onCreate()")
        super.onCreate()
        instance = this

        // Exception Handler
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            this.startActivity(
                Intent(this, CrashActivity::class.java)
                    .apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(KEY_STACK_TRACE, Log.getStackTraceString(exception))
                    }
            )
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }

        // default theme
        if (!ThemeColor.isConfigured(this, 1)) {
            ThemeColor.editTheme(this)
                .primaryColorRes(util.mdcolor.R.color.md_blue_A400)
                .accentColorRes(util.mdcolor.R.color.md_yellow_900)
                .commit()
        }

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            DynamicShortcutManager(this).initDynamicShortcuts()
        }

        // StatusBar Lyrics API
        lyricsService =
            StatusBarLyric.API.StatusBarLyric(
                this@App,
                AppCompatResources.getDrawable(this@App, R.drawable.ic_notification),
                PACKAGE_NAME,
                false
            )
    }

    val nightMode: Boolean
        get() = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }

    companion object {
        @JvmStatic
        lateinit var instance: App private set

        const val PACKAGE_NAME = "player.phonograph"
        const val ACTUAL_PACKAGE_NAME = BuildConfig.APPLICATION_ID
    }
}
