package util.mddesign.util;

import android.content.Context;

import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class ToolbarColorUtil {
    @CheckResult
    @ColorInt
    public static int toolbarContentColor(@NonNull Context context, @ColorInt int toolbarColor) {
        if (ColorUtil.isColorLight(toolbarColor)) {
            return toolbarSubtitleColor(context, toolbarColor);
        }
        return toolbarTitleColor(context, toolbarColor);
    }

    @CheckResult
    @ColorInt
    public static int toolbarSubtitleColor(@NonNull Context context, @ColorInt int toolbarColor) {
        return MaterialColorHelper.getSecondaryTextColor(context, ColorUtil.isColorLight(toolbarColor));
    }

    @CheckResult
    @ColorInt
    public static int toolbarTitleColor(@NonNull Context context, @ColorInt int toolbarColor) {
        return MaterialColorHelper.getPrimaryTextColor(context, ColorUtil.isColorLight(toolbarColor));
    }
}
