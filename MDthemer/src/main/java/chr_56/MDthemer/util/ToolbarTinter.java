package chr_56.MDthemer.util;

import android.content.Context;
import android.view.Menu;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import static chr_56.MDthemer.util.ToolbarColorUtil.*;

/**
 * @author Karim Abou Zeid (kabouzeid), chr_56 [modify]
 */
public class ToolbarTinter {
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


}

