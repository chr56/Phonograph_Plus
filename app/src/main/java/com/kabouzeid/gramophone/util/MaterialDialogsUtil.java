package com.kabouzeid.gramophone.util;

import android.content.Context;
import android.content.res.ColorStateList;

import com.afollestad.materialdialogs.internal.ThemeSingleton;

import chr_56.MDthemer.core.ThemeColor;
import chr_56.MDthemer.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public final class MaterialDialogsUtil {

    public static void updateMaterialDialogsThemeSingleton(Context context) {
        final ThemeSingleton md = ThemeSingleton.get();
        md.titleColor = ThemeColor.textColorPrimary(context);
        md.contentColor = ThemeColor.textColorSecondary(context);
        md.itemColor = md.titleColor;
        md.widgetColor = ThemeColor.accentColor(context);
        md.linkColor = ColorStateList.valueOf(md.widgetColor);
        md.positiveColor = ColorStateList.valueOf(md.widgetColor);
        md.neutralColor = ColorStateList.valueOf(md.widgetColor);
        md.negativeColor = ColorStateList.valueOf(md.widgetColor);
        md.darkTheme = chr_56.MDthemer.util.Util.isWindowBackgroundDark(context);//todo
    }

    private MaterialDialogsUtil() {
    }
}
