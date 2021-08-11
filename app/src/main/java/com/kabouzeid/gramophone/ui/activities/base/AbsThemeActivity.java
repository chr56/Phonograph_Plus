package com.kabouzeid.gramophone.ui.activities.base;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorInt;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.util.PreferenceUtil;
import com.kabouzeid.gramophone.util.Util;

import chr_56.MDthemer.core.ThemeColor;
import chr_56.MDthemer.core.Themer;
import chr_56.MDthemer.core.activities.ThemeActivity;
import chr_56.MDthemer.util.ColorUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public abstract class AbsThemeActivity extends ThemeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(PreferenceUtil.getInstance(this).getGeneralTheme());
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        //MaterialDialogsUtil.updateMaterialDialogsThemeSingleton(this);
    }

    protected void setDrawUnderStatusbar() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            Util.setAllowDrawUnderStatusBar(getWindow());
//        else //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) //TODO
//            Util.setStatusBarTranslucent(getWindow());
    }

    /**
     * This will set the color of the view with the id "status_bar" on KitKat and Lollipop.
     * On Lollipop if no such view is found it will set the statusbar color using the native method.
     *
     * @param color the new statusbar color (will be shifted down on Lollipop and above)
     */
    public void setStatusbarColor(int color) { //TODO
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final View statusBar = getWindow().getDecorView().getRootView().findViewById(R.id.status_bar);
            if (statusBar != null) {
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    statusBar.setBackgroundColor(ColorUtil.darkenColor(color));
                //} else {
                //    statusBar.setBackgroundColor(color);
                //}
            } else /*if (Build.VERSION.SDK_INT >= 21)*/ {
                getWindow().setStatusBarColor(ColorUtil.darkenColor(color));
            }
        setLightStatusbarAuto(color);
        //}
    }

    public void setStatusbarColorAuto() {
        // we don't want to use statusbar color because we are doing the color darkening on our own to support KitKat
        setStatusbarColor(ThemeColor.primaryColor(this));
    }

    public void setTaskDescriptionColor(@ColorInt int color) {
        Themer.setTaskDescriptionColor(this, color);
    }

    public void setTaskDescriptionColorAuto() {
        setTaskDescriptionColor(ThemeColor.primaryColor(this));
    }

    public void setNavigationbarColor(int color) {
        if (ThemeColor.coloredNavigationBar(this)) {
            Themer.setNavigationbarColor(this, color);
        } else {
            Themer.setNavigationbarColor(this, Color.BLACK);
        }
    }

    public void setNavigationbarColorAuto() {
        setNavigationbarColor(ThemeColor.navigationBarColor(this));
    }

    public void setLightStatusbar(boolean enabled) {
        Themer.setLightStatusbar(this, enabled);
    }

    public void setLightStatusbarAuto(int bgColor) {
        setLightStatusbar(ColorUtil.isColorLight(bgColor));
    }

//    static final class MaterialDialogsUtil {
//
//        public static void updateMaterialDialogsThemeSingleton(Context context) {
//            final ThemeSingleton md = ThemeSingleton.get();
//            md.titleColor = ThemeColor.textColorPrimary(context);
//            md.contentColor = ThemeColor.textColorSecondary(context);
//            md.itemColor = md.titleColor;
//            md.widgetColor = ThemeColor.accentColor(context);
//            md.linkColor = ColorStateList.valueOf(md.widgetColor);
//            md.positiveColor = ColorStateList.valueOf(md.widgetColor);
//            md.neutralColor = ColorStateList.valueOf(md.widgetColor);
//            md.negativeColor = ColorStateList.valueOf(md.widgetColor);
//            md.darkTheme = chr_56.MDthemer.util.Util.isWindowBackgroundDark(context);//todo
//        }
//
//        private MaterialDialogsUtil() {
//        }
//    }
}
