/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import coil.ImageLoader
import coil.ImageLoaderFactory
import lib.phonograph.localization.ContextLocaleDelegate
import lib.phonograph.misc.Reboot
import mt.pref.ThemeColor
import mt.pref.internal.ThemeStore
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.coil.createPhonographImageLoader
import player.phonograph.notification.ErrorNotification
import player.phonograph.notification.ErrorNotification.KEY_STACK_TRACE
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.activities.CrashActivity
import player.phonograph.ui.moduleViewModels
import player.phonograph.util.debug
import player.phonograph.util.logMetrics
import player.phonograph.util.postDelayedOnceHandlerCallback
import player.phonograph.util.theme.changeGlobalNightMode
import player.phonograph.util.theme.checkNightMode
import androidx.appcompat.app.AppCompatDelegate
import player.phonograph.util.text.currentTimestamp
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
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

    override fun attachBaseContext(base: Context?) {
        // Localization
        super.attachBaseContext(
            ContextLocaleDelegate.attachBaseContext(base)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Night Mode
        checkNightMode(newConfig) { present, nightMode ->
            postDelayedOnceHandlerCallback(Handler(Looper.getMainLooper()), 550, 536870912) {
                changeGlobalNightMode(present, nightMode)
            }
        }
        // Localization
        super.onConfigurationChanged(
            ContextLocaleDelegate.onConfigurationChanged(this, newConfig)
        )
    }

    override fun onCreate() {
        if (Reboot.isRebootingProcess(this)) return
        debug { logMetrics("App.onCreate()") }
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
        // night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        startKoin {
            androidLogger(if (DEBUG) Level.DEBUG else Level.WARNING)
            androidContext(this@App)

            modules(moduleStatus, moduleLoaders, moduleViewModels)
        }

        // database
        MusicDatabase.Metadata.lastAccessTimestamp = currentTimestamp() / 1000
    }

    override fun onTerminate() {
        GlobalContext.get().get<QueueManager>().release()
        super.onTerminate()
        MusicDatabase.songsDataBase.close()
    }

    // for coil ImageLoader singleton
    override fun newImageLoader(): ImageLoader = createPhonographImageLoader(this)
}
