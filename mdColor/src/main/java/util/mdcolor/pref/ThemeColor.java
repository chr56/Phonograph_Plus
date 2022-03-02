/*
 * Copyright (c) 2022 Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid), che_56 (modified)
 */

package util.mdcolor.pref;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.annotation.AttrRes;
import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class ThemeColor implements ThemeColorInterface, ThemeColorPrefKeys {

    @SuppressWarnings("FieldCanBeLocal")
    private final Context mContext;
    private final SharedPreferences.Editor mEditor;

    public static ThemeColor editTheme(@NonNull Context context) {
        return new ThemeColor(context);
    }

    @SuppressLint("CommitPrefEdits")
    private ThemeColor(@NonNull Context context) {
        mContext = context;
        mEditor = mPreferences(context).edit();
    }

    // Static getters

    @CheckResult
    @NonNull
    public static SharedPreferences mPreferences(@NonNull Context context) {
        return context.getSharedPreferences(CONFIG_PREFS_KEY_DEFAULT, Context.MODE_PRIVATE);
    }


    @Override
    public ThemeColor primaryColor(@ColorInt int color) {
        mEditor.putInt(KEY_PRIMARY_COLOR, color);
        return this;
    }

    @Override
    public ThemeColor primaryColorRes(@ColorRes int colorRes) {
        return primaryColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor primaryColorAttr(@AttrRes int colorAttr) {
        return primaryColor(resolveColor(mContext, colorAttr));
    }


    @Override
    public ThemeColor accentColor(@ColorInt int color) {
        mEditor.putInt(KEY_ACCENT_COLOR, color);
        return this;
    }

    @Override
    public ThemeColor accentColorRes(@ColorRes int colorRes) {
        return accentColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor accentColorAttr(@AttrRes int colorAttr) {
        return accentColor(resolveColor(mContext, colorAttr));
    }


    @Override
    public ThemeColor coloredStatusBar(boolean colored) {
        mEditor.putBoolean(KEY_APPLY_PRIMARYDARK_STATUSBAR, colored);
        return this;
    }

    @Override
    public ThemeColor coloredNavigationBar(boolean applyToNavBar) {
        mEditor.putBoolean(KEY_APPLY_PRIMARY_NAVBAR, applyToNavBar);
        return this;
    }

    @CheckResult
    public static boolean isConfigured(Context context) {
        return mPreferences(context).getBoolean(IS_CONFIGURED_KEY, false);
    }

    @SuppressLint("ApplySharedPref")
    public static boolean isConfigured(Context context, @IntRange(from = 0, to = Integer.MAX_VALUE) int version) {
        final SharedPreferences prefs = mPreferences(context);
        final int lastVersion = prefs.getInt(IS_CONFIGURED_VERSION_KEY, -1);
        if (version > lastVersion) {
            prefs.edit().putInt(IS_CONFIGURED_VERSION_KEY, version).commit();
            return false;
        }
        return true;
    }


    // Commit method

    @Override
    public void commit() {
        mEditor.putLong(VALUES_CHANGED, System.currentTimeMillis())
                .putBoolean(IS_CONFIGURED_KEY, true)
                .commit();
    }

    /**
     * <b>Dangerous !</b>, this reset all SharedPreferences!
     */
    public void clearAllPreference() {
        mEditor.clear().commit();
    }


    // Access

    @CheckResult
    @ColorInt
    public static int primaryColor(@NonNull Context context) {
        return mPreferences(context).getInt(KEY_PRIMARY_COLOR, resolveColor(context, androidx.appcompat.R.attr.colorPrimary, Color.parseColor("#455A64")));
    }

    @CheckResult
    @ColorInt
    public static int accentColor(@NonNull Context context) {
        return mPreferences(context).getInt(KEY_ACCENT_COLOR, resolveColor(context, androidx.appcompat.R.attr.colorAccent, Color.parseColor("#263238")));
    }


    @CheckResult
    public static boolean coloredStatusBar(@NonNull Context context) {
        return mPreferences(context).getBoolean(KEY_APPLY_PRIMARYDARK_STATUSBAR, true);
    }

    @CheckResult
    public static boolean coloredNavigationBar(@NonNull Context context) {
        return mPreferences(context).getBoolean(KEY_APPLY_PRIMARY_NAVBAR, false);
    }

    @CheckResult
    @ColorInt
    public static int navigationBarColor(@NonNull Context context) {
        if (!coloredNavigationBar(context)) {
            return Color.BLACK;
        }
        return primaryColor(context);
    }

    @CheckResult
    @ColorInt
    public static int statusBarColor(@NonNull Context context) {
        if (!coloredStatusBar(context)) {
            return Color.BLACK;
        }
        return primaryColorDark(context);
    }

    private static int primaryColorDark(@NonNull Context context) {
        return shiftColor(primaryColor(context), 0.9f);
    }


    @CheckResult
    @ColorInt
    public static int textColorPrimary(@NonNull Context context) {
        return mPreferences(context).getInt(KEY_TEXT_COLOR_PRIMARY, resolveColor(context, android.R.attr.textColorPrimary));
    }

    @CheckResult
    @ColorInt
    public static int textColorSecondary(@NonNull Context context) {
        return mPreferences(context).getInt(KEY_TEXT_COLOR_SECONDARY, resolveColor(context, android.R.attr.textColorSecondary));
    }


    public static void markChanged(@NonNull Context context) {
        new ThemeColor(context).commit();
    }


    // Util

    protected static int resolveColor(Context context, @AttrRes int attr, int fallback) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getColor(0, fallback);
        } finally {
            a.recycle();
        }
    }

    protected static int resolveColor(Context context, @AttrRes int attr) {
        return resolveColor(context, attr, 0);
    }


    @ColorInt
    private static int shiftColor(@ColorInt int color, @FloatRange(from = 0.0f, to = 2.0f) float by) {
        if (by == 1f) return color;
        int alpha = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= by; // value component
        return (alpha << 24) + (0x00ffffff & Color.HSVToColor(hsv));
    }

}
