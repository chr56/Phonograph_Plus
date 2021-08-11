package chr_56.MDthemer.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;

import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.WindowDecorActionBar;
import androidx.appcompat.view.menu.BaseMenuPresenter;
import androidx.appcompat.view.menu.ListMenuItemView;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.ShowableListMenu;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.ToolbarWidgetWrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;

import chr_56.MDthemer.R;
import chr_56.MDthemer.core.ThemeColor;

/**
 * @author Karim Abou Zeid (kabouzeid), chr_56 [modify]
 */
public class ToolbarThemer {
    @SuppressWarnings("unchecked")
    public static void setToolbarColor(@NonNull Context context, @NonNull Toolbar toolbar,@Nullable Menu menu,
                                       final @ColorInt int toolbarColor,
                                       final @ColorInt int titleTextColor,
                                       final @ColorInt int subtitleTextColor) {
        if (toolbar != null) {
            //Text
            toolbar.setTitleTextColor(titleTextColor);
            toolbar.setSubtitleTextColor(subtitleTextColor);
            //Icon
            if (toolbar.getNavigationIcon() != null) {
                // Tint the toolbar navigation icon (e.g. back, drawer, etc.)
                toolbar.setNavigationIcon(TintHelper.createTintedDrawable(toolbar.getNavigationIcon(), toolbarColor));
            }
        }
    }
    public static void setToolbarColorAuto(@NonNull Context context, Toolbar toolbar,Menu menu,
                                           final @ColorInt int toolbarColor){
        setToolbarColor(context, toolbar,menu,
                toolbarContentColor(context, toolbarColor),
                toolbarTitleColor(context, toolbarColor),
                toolbarSubtitleColor(context, toolbarColor));
    }

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
    @Nullable
    public static Toolbar getSupportActionBarView(@Nullable ActionBar ab) {
        if (ab == null || !(ab instanceof WindowDecorActionBar)) return null;
        try {
            WindowDecorActionBar decorAb = (WindowDecorActionBar) ab;
            Field field = WindowDecorActionBar.class.getDeclaredField("mDecorToolbar");
            field.setAccessible(true);
            ToolbarWidgetWrapper wrapper = (ToolbarWidgetWrapper) field.get(decorAb);
            field = ToolbarWidgetWrapper.class.getDeclaredField("mToolbar");
            field.setAccessible(true);
            return (Toolbar) field.get(wrapper);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to retrieve Toolbar from AppCompat support ActionBar: " + t.getMessage(), t);
        }
    }
}

