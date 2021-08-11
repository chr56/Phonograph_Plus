package chr_56.MDthemer.core.activities;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Menu;


import androidx.appcompat.widget.Toolbar;

import chr_56.MDthemer.core.BaseActivity;
import chr_56.MDthemer.core.ThemeColor;
import chr_56.MDthemer.util.ToolbarThemer;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ThemeActivity extends BaseActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Toolbar toolbar = getToolbar();
        ToolbarThemer.setToolbarColorAuto(this,toolbar,menu,getToolbarBackgroundColor(toolbar));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ToolbarThemer.InternalToolbarContentTintUtil.applyOverflowMenuTint(this,getToolbar(), ThemeColor.accentColor(this));
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
        return ToolbarThemer.getSupportActionBarView(getSupportActionBar());
    }

    protected void initView(Context context) {
    }
}