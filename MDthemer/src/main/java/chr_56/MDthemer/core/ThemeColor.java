package chr_56.MDthemer.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.AttrRes;
import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import chr_56.MDthemer.R;
import chr_56.MDthemer.util.ColorUtil;
import chr_56.MDthemer.util.Util;

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid), che_56[modified]
 */
public final class ThemeColor implements ThemeColorPrefKeys, ThemeColorInterface {

    private final Context mContext;
    private final SharedPreferences.Editor mEditor;

    public static ThemeColor editTheme(@NonNull Context context) {
        return new ThemeColor(context);
    }

    @SuppressLint("CommitPrefEdits")
    private ThemeColor(@NonNull Context context) {
        mContext = context;
        mEditor = prefs(context).edit();
    }


    @Override
    public ThemeColor primaryColor(@ColorInt int color) {
        mEditor.putInt(KEY_PRIMARY_COLOR, color);
        if (autoGeneratePrimaryDark(mContext))
            primaryColorDark(ColorUtil.darkenColor(color));
        return this;
    }

    @Override
    public ThemeColor primaryColorRes(@ColorRes int colorRes) {
        return primaryColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor primaryColorAttr(@AttrRes int colorAttr) {
        return primaryColor(Util.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeColor primaryColorDark(@ColorInt int color) {
        mEditor.putInt(KEY_PRIMARY_COLOR_DARK, color);
        return this;
    }

    @Override
    public ThemeColor primaryColorDarkRes(@ColorRes int colorRes) {
        return primaryColorDark(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor primaryColorDarkAttr(@AttrRes int colorAttr) {
        return primaryColorDark(Util.resolveColor(mContext, colorAttr));
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
        return accentColor(Util.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeColor statusBarColor(@ColorInt int color) {
        mEditor.putInt(KEY_STATUS_BAR_COLOR, color);
        return this;
    }

    @Override
    public ThemeColor statusBarColorRes(@ColorRes int colorRes) {
        return statusBarColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor statusBarColorAttr(@AttrRes int colorAttr) {
        return statusBarColor(Util.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeColor navigationBarColor(@ColorInt int color) {
        mEditor.putInt(KEY_NAVIGATION_BAR_COLOR, color);
        return this;
    }

    @Override
    public ThemeColor navigationBarColorRes(@ColorRes int colorRes) {
        return navigationBarColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor navigationBarColorAttr(@AttrRes int colorAttr) {
        return navigationBarColor(Util.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeColor textColorPrimary(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_PRIMARY, color);
        return this;
    }

    @Override
    public ThemeColor textColorPrimaryRes(@ColorRes int colorRes) {
        return textColorPrimary(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor textColorPrimaryAttr(@AttrRes int colorAttr) {
        return textColorPrimary(Util.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeColor textColorPrimaryInverse(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_PRIMARY_INVERSE, color);
        return this;
    }

    @Override
    public ThemeColor textColorPrimaryInverseRes(@ColorRes int colorRes) {
        return textColorPrimaryInverse(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor textColorPrimaryInverseAttr(@AttrRes int colorAttr) {
        return textColorPrimaryInverse(Util.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeColor textColorSecondary(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_SECONDARY, color);
        return this;
    }

    @Override
    public ThemeColor textColorSecondaryRes(@ColorRes int colorRes) {
        return textColorSecondary(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor textColorSecondaryAttr(@AttrRes int colorAttr) {
        return textColorSecondary(Util.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeColor textColorSecondaryInverse(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_SECONDARY_INVERSE, color);
        return this;
    }

    @Override
    public ThemeColor textColorSecondaryInverseRes(@ColorRes int colorRes) {
        return textColorSecondaryInverse(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeColor textColorSecondaryInverseAttr(@AttrRes int colorAttr) {
        return textColorSecondaryInverse(Util.resolveColor(mContext, colorAttr));
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

    @Override
    public ThemeColor autoGeneratePrimaryDark(boolean autoGenerate) {
        mEditor.putBoolean(KEY_AUTO_GENERATE_PRIMARYDARK, autoGenerate);
        return this;
    }

    // Commit method

    @SuppressWarnings("unchecked")
    @Override
    public void commit() {
        mEditor.putLong(VALUES_CHANGED, System.currentTimeMillis())
                .putBoolean(IS_CONFIGURED_KEY, true)
                .commit();
    }

    // Static getters

    @CheckResult
    @NonNull
    public static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(CONFIG_PREFS_KEY_DEFAULT, Context.MODE_PRIVATE);
    }

    public static void markChanged(@NonNull Context context) {
        new ThemeColor(context).commit();
    }

    @CheckResult
    @ColorInt
    public static int primaryColor(@NonNull Context context) {
        return prefs(context).getInt(KEY_PRIMARY_COLOR, Util.resolveColor(context, R.attr.colorPrimary, Color.parseColor("#455A64")));
    }

    @CheckResult
    @ColorInt
    public static int primaryColorDark(@NonNull Context context) {
        return prefs(context).getInt(KEY_PRIMARY_COLOR_DARK, Util.resolveColor(context, R.attr.colorPrimaryDark, Color.parseColor("#37474F")));
    }

    @CheckResult
    @ColorInt
    public static int accentColor(@NonNull Context context) {
        return prefs(context).getInt(KEY_ACCENT_COLOR, Util.resolveColor(context, R.attr.colorAccent, Color.parseColor("#263238")));
    }

    @CheckResult
    @ColorInt
    public static int statusBarColor(@NonNull Context context) {
        if (!coloredStatusBar(context)) {
            return Color.BLACK;
        }
        return prefs(context).getInt(KEY_STATUS_BAR_COLOR, primaryColorDark(context));
    }

    @CheckResult
    @ColorInt
    public static int navigationBarColor(@NonNull Context context) {
        if (!coloredNavigationBar(context)) {
            return Color.BLACK;
        }
        return prefs(context).getInt(KEY_NAVIGATION_BAR_COLOR, primaryColor(context));
    }

    @CheckResult
    @ColorInt
    public static int textColorPrimary(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_PRIMARY, Util.resolveColor(context, android.R.attr.textColorPrimary));
    }

    @CheckResult
    @ColorInt
    public static int textColorPrimaryInverse(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_PRIMARY_INVERSE, Util.resolveColor(context, android.R.attr.textColorPrimaryInverse));
    }

    @CheckResult
    @ColorInt
    public static int textColorSecondary(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_SECONDARY, Util.resolveColor(context, android.R.attr.textColorSecondary));
    }

    @CheckResult
    @ColorInt
    public static int textColorSecondaryInverse(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_SECONDARY_INVERSE, Util.resolveColor(context, android.R.attr.textColorSecondaryInverse));
    }

    @CheckResult
    public static boolean coloredStatusBar(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_APPLY_PRIMARYDARK_STATUSBAR, true);
    }

    @CheckResult
    public static boolean coloredNavigationBar(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_APPLY_PRIMARY_NAVBAR, false);
    }

    @CheckResult
    public static boolean autoGeneratePrimaryDark(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_AUTO_GENERATE_PRIMARYDARK, true);
    }

    @CheckResult
    public static boolean isConfigured(Context context) {
        return prefs(context).getBoolean(IS_CONFIGURED_KEY, false);
    }

    @SuppressLint("CommitPrefEdits")
    public static boolean isConfigured(Context context, @IntRange(from = 0, to = Integer.MAX_VALUE) int version) {
        final SharedPreferences prefs = prefs(context);
        final int lastVersion = prefs.getInt(IS_CONFIGURED_VERSION_KEY, -1);
        if (version > lastVersion) {
            prefs.edit().putInt(IS_CONFIGURED_VERSION_KEY, version).commit();
            return false;
        }
        return true;
    }
}