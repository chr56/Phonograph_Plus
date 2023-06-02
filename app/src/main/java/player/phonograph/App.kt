package player.phonograph

import coil.ImageLoader
import coil.ImageLoaderFactory
import lib.phonograph.localization.ContextLocaleDelegate
import lib.phonograph.localization.Localization
import lib.phonograph.misc.Reboot
import mt.pref.ThemeColor
import mt.pref.internal.ThemeStore
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.coil.createPhonographImageLoader
import player.phonograph.mechanism.event.setupEventReceiver
import player.phonograph.notification.ErrorNotification
import player.phonograph.notification.ErrorNotification.KEY_STACK_TRACE
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.activities.CrashActivity
import player.phonograph.util.theme.applyMonet
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class App : Application(), ImageLoaderFactory {

    companion object {
        @JvmStatic
        lateinit var instance: App
            private set
    }

    private var _queueManager: QueueManager? = null
    val queueManager: QueueManager
        get() {
            if (_queueManager == null) {
                // QueueManager
                _queueManager = QueueManager(this).apply {
                    // restore all
                    post(QueueManager.MSG_STATE_RESTORE_ALL)
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
        // Theme
        applyMonet(this)
    }

    override fun onCreate() {
        if (Reboot.isRebootingProcess(this)) return

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

        if (CrashActivity.isCrashProcess(this)) return

        // default theme
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeColor.editTheme(this)
                .primaryColorRes(mt.color.R.color.md_blue_A400)
                .accentColorRes(mt.color.R.color.md_yellow_900)
                .commit()
        }

        applyMonet(this)

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            DynamicShortcutManager(this).initDynamicShortcuts()
        }

        // Sync Locales
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Localization.syncSystemLocale(this)
        }

        // state listener
        setupEventReceiver(this)
    }

    override fun onTerminate() {
        queueManager.release()
        super.onTerminate()
    }

    // for coil ImageLoader singleton
    override fun newImageLoader(): ImageLoader = createPhonographImageLoader(this)
}
