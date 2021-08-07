package chr_56.MDthemer;


import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid)
 */
interface ThemeColorInterface {

    // Primary colors

    ThemeColor primaryColor(@ColorInt int color);

    ThemeColor primaryColorRes(@ColorRes int colorRes);

    ThemeColor primaryColorAttr(@AttrRes int colorAttr);

    ThemeColor autoGeneratePrimaryDark(boolean autoGenerate);

    ThemeColor primaryColorDark(@ColorInt int color);

    ThemeColor primaryColorDarkRes(@ColorRes int colorRes);

    ThemeColor primaryColorDarkAttr(@AttrRes int colorAttr);

    // Accent colors

    ThemeColor accentColor(@ColorInt int color);

    ThemeColor accentColorRes(@ColorRes int colorRes);

    ThemeColor accentColorAttr(@AttrRes int colorAttr);

    // Status bar color

    ThemeColor statusBarColor(@ColorInt int color);

    ThemeColor statusBarColorRes(@ColorRes int colorRes);

    ThemeColor statusBarColorAttr(@AttrRes int colorAttr);

    // Navigation bar color

    ThemeColor navigationBarColor(@ColorInt int color);

    ThemeColor navigationBarColorRes(@ColorRes int colorRes);

    ThemeColor navigationBarColorAttr(@AttrRes int colorAttr);

    // Primary text color

    ThemeColor textColorPrimary(@ColorInt int color);

    ThemeColor textColorPrimaryRes(@ColorRes int colorRes);

    ThemeColor textColorPrimaryAttr(@AttrRes int colorAttr);

    ThemeColor textColorPrimaryInverse(@ColorInt int color);

    ThemeColor textColorPrimaryInverseRes(@ColorRes int colorRes);

    ThemeColor textColorPrimaryInverseAttr(@AttrRes int colorAttr);

    // Secondary text color

    ThemeColor textColorSecondary(@ColorInt int color);

    ThemeColor textColorSecondaryRes(@ColorRes int colorRes);

    ThemeColor textColorSecondaryAttr(@AttrRes int colorAttr);

    ThemeColor textColorSecondaryInverse(@ColorInt int color);

    ThemeColor textColorSecondaryInverseRes(@ColorRes int colorRes);

    ThemeColor textColorSecondaryInverseAttr(@AttrRes int colorAttr);

    // Toggle configurations

    ThemeColor coloredStatusBar(boolean colored);

    ThemeColor coloredNavigationBar(boolean applyToNavBar);

    // Commit/apply

    void commit();
}
