package com.kabouzeid.gramophone

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import android.util.Log
import chr_56.MDthemer.core.ThemeColor
import com.kabouzeid.gramophone.appshortcuts.DynamicShortcutManager
import com.kabouzeid.gramophone.util.PreferenceUtil
import kotlin.system.exitProcess

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // Exception Handler
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            val intent: Intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK  or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(KEY_STACK_TRACE, Log.getStackTraceString(exception))
            intent.action = "$PACKAGE_NAME.CRASH_HANDLER"

            this.startActivity(intent)

            Process.killProcess(Process.myPid())
            exitProcess(1);
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

        //
        themeRes = PreferenceUtil.getInstance(this).generalTheme
    }
    private var themeRes: Int = 0

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

        const val PACKAGE_NAME = "com.kabouzeid.gramophone" // todo
        const val KEY_STACK_TRACE = "stack_trace"
    }
}
