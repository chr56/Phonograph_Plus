package player.phonograph

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import coil.ImageLoader
import coil.ImageLoaderFactory
import lib.phonograph.localization.ContextLocaleDelegate
import mt.pref.ThemeColor
import mt.pref.internal.ThemeStore
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.coil.createPhonographImageLoader
import player.phonograph.notification.ErrorNotification
import player.phonograph.notification.ErrorNotification.KEY_STACK_TRACE
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.activities.CrashActivity
import kotlin.system.exitProcess

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class App : Application(), ImageLoaderFactory {

    val lyricsService: StatusBarLyric.API.StatusBarLyric by lazy {
        // StatusBar Lyrics API
        StatusBarLyric.API.StatusBarLyric(
            this@App,
            AppCompatResources.getDrawable(this@App, R.drawable.ic_notification),
            PACKAGE_NAME,
            false
        )
    }

    var _queueManager: QueueManager? = null
    val queueManager: QueueManager
        get() {
            if (_queueManager == null) {
                // QueueManager
                _queueManager = QueueManager(this).apply {
                    // restore all
                    postMessage(QueueManager.MSG_STATE_RESTORE_ALL)
                }
            }
            return _queueManager!!
        }

    override fun attachBaseContext(base: Context?) {
        // Localization
        super.attachBaseContext(
            ContextLocaleDelegate.attachBaseContext(base)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Localization
        super.onConfigurationChanged(
            ContextLocaleDelegate.onConfigurationChanged(this, newConfig)
        )
    }

    override fun onCreate() {
        if (BuildConfig.DEBUG) Log.v(
            "Metrics",
            "${System.currentTimeMillis().mod(10000000)} App.onCreate()"
        )
        super.onCreate()
        instance = this

        // Exception Handler
        ErrorNotification.crashActivity = CrashActivity::class.java
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
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeColor.editTheme(this)
                .primaryColorRes(mt.color.R.color.md_blue_A400)
                .accentColorRes(mt.color.R.color.md_yellow_900)
                .commit()
        }

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            DynamicShortcutManager(this).initDynamicShortcuts()
        }
    }

    override fun onTerminate() {
        queueManager.release()
        super.onTerminate()
    }

    companion object {
        @JvmStatic
        lateinit var instance: App
            private set

        const val PACKAGE_NAME = "player.phonograph"
        const val ACTUAL_PACKAGE_NAME = BuildConfig.APPLICATION_ID
    }

    // for coil ImageLoader singleton
    override fun newImageLoader(): ImageLoader = createPhonographImageLoader(this)
}
