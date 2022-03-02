package util.mddesign.util;

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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.BaseMenuPresenter;
import androidx.appcompat.view.menu.ListMenuItemView;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.ShowableListMenu;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;

import java.lang.reflect.Field;
import java.util.ArrayList;

import chr_56.MDthemer.R;
import util.mdcolor.MaterialColor;
import util.mdcolor.pref.ThemeColor;

/**
 * @author Karim Abou Zeid (kabouzeid), chr_56 [modify]
 */
public class MenuTinter {
    /**
     * tint the menu
     * @param context context of toolbar's container
     * @param toolbar menu's container
     * @param menu the menu to tint
     * @param menuWidgetColor menu icon color
     */
    public static void setMenuColor(@NonNull Context context,Toolbar toolbar,@NonNull Menu menu,
                                       final @ColorInt int menuWidgetColor) {
        tintMenu(toolbar, menu, menuWidgetColor);
        applyOverflowMenuTint(context, toolbar, menuWidgetColor);

        if (context instanceof Activity) {
            setOverflowButtonColor((Activity) context, menuWidgetColor);
        }

        try {
            // Tint immediate overflow menu items
            final Field menuField = Toolbar.class.getDeclaredField("mMenuBuilderCallback");
            menuField.setAccessible(true);
            final Field presenterField = Toolbar.class.getDeclaredField("mActionMenuPresenterCallback");
            presenterField.setAccessible(true);
            final Field menuViewField = Toolbar.class.getDeclaredField("mMenuView");
            menuViewField.setAccessible(true);

            final MenuPresenter.Callback currentPresenterCb = (MenuPresenter.Callback) presenterField.get(toolbar);
            if (!(currentPresenterCb instanceof _MenuPresenterCallback)) {
                final _MenuPresenterCallback newPresenterCb = new _MenuPresenterCallback(context, menuWidgetColor, currentPresenterCb, toolbar);
                final MenuBuilder.Callback currentMenuCb = (MenuBuilder.Callback) menuField.get(toolbar);
                toolbar.setMenuCallbacks(newPresenterCb, currentMenuCb);
                ActionMenuView menuView = (ActionMenuView) menuViewField.get(toolbar);
                if (menuView != null)
                    menuView.setMenuCallbacks(newPresenterCb, currentMenuCb);
            }

            // OnMenuItemClickListener to tint submenu items
            final Field menuItemClickListener = Toolbar.class.getDeclaredField("mOnMenuItemClickListener");
            menuItemClickListener.setAccessible(true);
            Toolbar.OnMenuItemClickListener currentClickListener = (Toolbar.OnMenuItemClickListener) menuItemClickListener.get(toolbar);
            if (!(currentClickListener instanceof _OnMenuItemClickListener)) {
                final _OnMenuItemClickListener newClickListener = new _OnMenuItemClickListener(context, menuWidgetColor, currentClickListener, toolbar);
                toolbar.setOnMenuItemClickListener(newClickListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * tint the menu, menu icon color is current accent color
     * @param context context of toolbar's container
     * @param toolbar menu's container
     * @param menu the menu to tint
     */
    public static void setMenuColor_Accent(@NonNull Context context, Toolbar toolbar, Menu menu) {
        setMenuColor(context, toolbar, menu, ThemeColor.accentColor(context));
    }
    /**
     * tint the menu, menu icon color is just white
     * @param context context of toolbar's container
     * @param toolbar menu's container
     * @param menu the menu to tint
     */
    public static void setMenuColor_White(@NonNull Context context, Toolbar toolbar, Menu menu) {
        setMenuColor(context, toolbar, menu, MaterialColor.White._1000.getAsColor());
    }
    /**
     * tint the menu, menu icon color is just black
     * @param context context of toolbar's container
     * @param toolbar menu's container
     * @param menu the menu to tint
     */
    public static void setMenuColor_Black(@NonNull Context context, Toolbar toolbar, Menu menu) {
        setMenuColor(context, toolbar, menu, MaterialColor.Black._1000.getAsColor());
    }

    public static void handleOnPrepareOptionsMenu(Activity activity, Toolbar toolbar,
                                                  int widgetColor) {
        applyOverflowMenuTint(activity, toolbar, widgetColor);
    }
    public static void handleOnPrepareOptionsMenu(Activity activity, Toolbar toolbar) {
        applyOverflowMenuTint(activity, toolbar, ThemeColor.accentColor(activity));
    }



    private static void tintMenu(@NonNull Toolbar toolbar, @Nullable Menu menu, final @ColorInt int color) {
        try {
            final Field field = Toolbar.class.getDeclaredField("mCollapseIcon");
            field.setAccessible(true);
            Drawable collapseIcon = (Drawable) field.get(toolbar);
            if (collapseIcon != null) {
                field.set(toolbar, TintHelper.createTintedDrawable(collapseIcon, color));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (menu != null && menu.size() > 0) {
            for (int i = 0; i < menu.size(); i++) {
                final MenuItem item = menu.getItem(i);
                if (item.getIcon() != null) {
                    item.setIcon(TintHelper.createTintedDrawable(item.getIcon(), color));
                }
                // Search view theming
                if (item.getActionView() != null && (item.getActionView() instanceof android.widget.SearchView || item.getActionView() instanceof androidx.appcompat.widget.SearchView)) {
                    SearchViewTintUtil.setSearchViewContentColor(item.getActionView(), color);
                }
            }
        }
    }

    public static void applyOverflowMenuTint(final @NonNull Context context, final Toolbar toolbar, final @ColorInt int color) {
        if (toolbar == null) return;
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Field f1 = Toolbar.class.getDeclaredField("mMenuView");
                    f1.setAccessible(true);
                    ActionMenuView actionMenuView = (ActionMenuView) f1.get(toolbar);
                    Field f2 = ActionMenuView.class.getDeclaredField("mPresenter");
                    f2.setAccessible(true);

                    // Actually ActionMenuPresenter
                    BaseMenuPresenter presenter = (BaseMenuPresenter) f2.get(actionMenuView);
                    Field f3 = presenter.getClass().getDeclaredField("mOverflowPopup");
                    f3.setAccessible(true);
                    MenuPopupHelper overflowMenuPopupHelper = (MenuPopupHelper) f3.get(presenter);
                    setTintForMenuPopupHelper(context, overflowMenuPopupHelper, color);

                    Field f4 = presenter.getClass().getDeclaredField("mActionButtonPopup");
                    f4.setAccessible(true);
                    MenuPopupHelper subMenuPopupHelper = (MenuPopupHelper) f4.get(presenter);
                    setTintForMenuPopupHelper(context, subMenuPopupHelper, color);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void setTintForMenuPopupHelper(final @NonNull Context context, @Nullable MenuPopupHelper menuPopupHelper, final @ColorInt int color) {
        try {
            if (menuPopupHelper != null) {
                final ListView listView = ((ShowableListMenu) menuPopupHelper.getPopup()).getListView();
                listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        try {
                            Field checkboxField = ListMenuItemView.class.getDeclaredField("mCheckBox");
                            checkboxField.setAccessible(true);
                            Field radioButtonField = ListMenuItemView.class.getDeclaredField("mRadioButton");
                            radioButtonField.setAccessible(true);

                            final boolean isDark = !ColorUtil.isColorLight(Util.resolveColor(context, android.R.attr.windowBackground));

                            for (int i = 0; i < listView.getChildCount(); i++) {
                                View v = listView.getChildAt(i);
                                if (!(v instanceof ListMenuItemView)) continue;
                                ListMenuItemView iv = (ListMenuItemView) v;

                                CheckBox check = (CheckBox) checkboxField.get(iv);
                                if (check != null) {
                                    TintHelper.setTint(check, color, isDark);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                        check.setBackground(null);
                                }

                                RadioButton radioButton = (RadioButton) radioButtonField.get(iv);
                                if (radioButton != null) {
                                    TintHelper.setTint(radioButton, color, isDark);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                        radioButton.setBackground(null);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        /*} else {
                            //noinspection deprecation
                            listView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }*/
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setOverflowButtonColor(@NonNull Activity activity, final @ColorInt int color) {
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<>();
                decorView.findViewsWithText(outViews, overflowDescription,
                        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) return;
                final AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
                overflow.setImageDrawable(TintHelper.createTintedDrawable(overflow.getDrawable(), color));
                ViewUtil.removeOnGlobalLayoutListener(decorView, this);
            }
        });
    }

    private static final class SearchViewTintUtil {
        private static void tintImageView(Object target, Field field, final @ColorInt int color) throws Exception {
            field.setAccessible(true);
            final ImageView imageView = (ImageView) field.get(target);
            if (imageView.getDrawable() != null)
                imageView.setImageDrawable(TintHelper.createTintedDrawable(imageView.getDrawable(), color));
        }

        public static void setSearchViewContentColor(View searchView, final @ColorInt int color) {
            if (searchView == null) return;
            final Class<?> cls = searchView.getClass();
            try {
                final Field mSearchSrcTextViewField = cls.getDeclaredField("mSearchSrcTextView");
                mSearchSrcTextViewField.setAccessible(true);
                final EditText mSearchSrcTextView = (EditText) mSearchSrcTextViewField.get(searchView);
                mSearchSrcTextView.setTextColor(color);
                mSearchSrcTextView.setHintTextColor(ColorUtil.adjustAlpha(color, 0.5f));
                TintHelper.setCursorTint(mSearchSrcTextView, color);

                Field field = cls.getDeclaredField("mSearchButton");
                tintImageView(searchView, field, color);
                field = cls.getDeclaredField("mGoButton");
                tintImageView(searchView, field, color);
                field = cls.getDeclaredField("mCloseButton");
                tintImageView(searchView, field, color);
                field = cls.getDeclaredField("mVoiceButton");
                tintImageView(searchView, field, color);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private SearchViewTintUtil() {
        }
    }

    private static class _MenuPresenterCallback implements MenuPresenter.Callback {

        public _MenuPresenterCallback(Context context, final @ColorInt int color, MenuPresenter.Callback parentCb, Toolbar toolbar) {
            mContext = context;
            mColor = color;
            mParentCb = parentCb;
            mToolbar = toolbar;
        }

        private Context mContext;
        private int mColor;
        private MenuPresenter.Callback mParentCb;
        private Toolbar mToolbar;

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            if (mParentCb != null)
                mParentCb.onCloseMenu(menu, allMenusAreClosing);
        }

        @Override
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            applyOverflowMenuTint(mContext, mToolbar, mColor);
            return mParentCb != null && mParentCb.onOpenSubMenu(subMenu);
        }
    }
    private static class _OnMenuItemClickListener implements Toolbar.OnMenuItemClickListener {

        private Context mContext;
        private int mColor;
        private Toolbar.OnMenuItemClickListener mParentListener;
        private Toolbar mToolbar;

        public _OnMenuItemClickListener(Context context, final @ColorInt int color, Toolbar.OnMenuItemClickListener parentCb, Toolbar toolbar) {
            mContext = context;
            mColor = color;
            mParentListener = parentCb;
            mToolbar = toolbar;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            applyOverflowMenuTint(mContext, mToolbar, mColor);
            return mParentListener != null && mParentListener.onMenuItemClick(item);
        }
    }


}
