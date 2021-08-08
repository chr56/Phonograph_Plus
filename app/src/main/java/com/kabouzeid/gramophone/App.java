package com.kabouzeid.gramophone;

import android.app.Application;
import android.os.Build;

import com.kabouzeid.gramophone.appshortcuts.DynamicShortcutManager;

import chr_56.MDthemer.core.ThemeColor;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends Application {

    private static App app;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        // default theme
        if (!ThemeColor.isConfigured(this, 1)) {
            ThemeColor.editTheme(this)
                    .primaryColorRes(R.color.md_light_blue_500)
                    .accentColorRes(R.color.md_yellow_800)
                    .commit();
        }

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).initDynamicShortcuts();
        }

    }

    public static App getInstance() {
        return app;
    }

}
