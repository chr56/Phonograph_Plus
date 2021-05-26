package com.kabouzeid.gramophone;

import android.app.Application;
import android.os.Build;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.appshortcuts.DynamicShortcutManager;

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
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeStore.editTheme(this)
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
