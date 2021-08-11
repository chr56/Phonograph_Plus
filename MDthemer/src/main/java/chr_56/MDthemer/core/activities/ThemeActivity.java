package chr_56.MDthemer.core.activities;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Menu;


import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.WindowDecorActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.ToolbarWidgetWrapper;

import java.lang.reflect.Field;

import chr_56.MDthemer.color.MaterialColor;
import chr_56.MDthemer.core.BaseActivity;
import chr_56.MDthemer.core.ThemeColor;
import chr_56.MDthemer.util.MenuTinter;
import chr_56.MDthemer.util.ToolbarTinter;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ThemeActivity extends BaseActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Toolbar toolbar = getToolbar();
        MenuTinter.setMenuColor(this,toolbar,menu,MaterialColor.White._1000.getAsColor());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuTinter.applyOverflowMenuTint(this,getToolbar(), ThemeColor.accentColor(this));
        return super.onPrepareOptionsMenu(menu);
    }

    public static int getToolbarBackgroundColor(Toolbar toolbar) {
        if (toolbar != null) {
            if (toolbar.getBackground() instanceof ColorDrawable) {
                return ((ColorDrawable) toolbar.getBackground()).getColor();
            }
        }
        return 0;
    }
    @Override
    protected Toolbar getToolbar() {
        return getSupportActionBarView(getSupportActionBar());
    }
    protected static Toolbar getSupportActionBarView(@Nullable ActionBar ab) {
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

    protected void initView(Context context) {
    }
}