package player.phonograph

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import chr_56.MDthemer.core.ThemeColor
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.database.mediastore.MusicDatabase
import player.phonograph.ui.activities.CrashActivity
import java.util.*
import kotlin.system.exitProcess

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class App : Application() {
    lateinit var lyricsService: StatusBarLyric.API.StatusBarLyric
    var isDatabaseChecked: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Exception Handler
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            val intent: Intent = Intent(this, CrashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(KEY_STACK_TRACE, Log.getStackTraceString(exception))
//            intent.action = "$PACKAGE_NAME.CRASH_HANDLER"

            this.startActivity(intent)

            Process.killProcess(Process.myPid())
            exitProcess(1)
        }

        // default theme
        if (!ThemeColor.isConfigured(this, 1)) {
            ThemeColor.editTheme(this)
                .primaryColorRes(R.color.md_blue_A400)
                .accentColorRes(R.color.md_yellow_900)
                .commit()
        }

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            DynamicShortcutManager(this).initDynamicShortcuts()
        }

        // StatusBar Lyrics API
        lyricsService =
            StatusBarLyric.API.StatusBarLyric(
                this,
                AppCompatResources.getDrawable(this, R.drawable.ic_notification),
                PACKAGE_NAME,
                false
            )

        // database
        MusicDatabase.songsDataBase.lastAccessTimestamp = Calendar.getInstance().timeInMillis
    }

    fun nightmode(): Boolean {
        val currentNightMode = (
            resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK
            )
        return when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: App
            private set

        const val PACKAGE_NAME = "player.phonograph" // todo
        const val ACTUAL_PACKAGE_NAME = BuildConfig.APPLICATION_ID
        const val KEY_STACK_TRACE = "stack_trace"
    }
}
