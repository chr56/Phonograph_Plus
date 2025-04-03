/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph

import coil.ImageLoader
import coil.ImageLoaderFactory
import lib.phonograph.localization.ContextLocaleDelegate
import lib.phonograph.misc.Reboot
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.coil.createPhonographImageLoader
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.moduleViewModels
import player.phonograph.ui.modules.auxiliary.CrashActivity
import player.phonograph.util.concurrent.postDelayedOnceHandlerCallback
import player.phonograph.util.debug
import player.phonograph.util.logMetrics
import player.phonograph.util.startCrashActivity
import player.phonograph.util.theme.ThemeCacheUpdateDelegate
import player.phonograph.util.theme.changeGlobalNightMode
import player.phonograph.util.theme.checkNightMode
import androidx.appcompat.app.AppCompatDelegate
import android.app.Application
import android.content.Context
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
            if (!CrashActivity.isCrashProcess(this)) {
                startCrashActivity(this, exception, CrashActivity::class.java)
            } else {
                Log.e("Phonograph", "Recursively crash!", exception)
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }

        if (CrashActivity.isCrashProcess(this)) return

        // night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        startKoin {
            androidLogger(if (DEBUG) Level.DEBUG else Level.WARNING)
            androidContext(this@App)

            modules(moduleStatus, moduleLoaders, moduleViewModels)
        }

        // Color
        ThemeCacheUpdateDelegate.start(this)
    }

    override fun onTerminate() {
        GlobalContext.get().get<QueueManager>().release()
        super.onTerminate()
    }

    // for coil ImageLoader singleton
    override fun newImageLoader(): ImageLoader = createPhonographImageLoader(this)
}
