package player.phonograph.views.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.SwitchCompat;

import util.mdcolor.pref.ThemeColor;
import util.mddesign.core.Themer;


/**
 * @author Aidan Follestad (afollestad)
 */
public class SwitchX extends SwitchCompat {

    public SwitchX(Context context) {
        super(context);
        init(context, null);
    }

    public SwitchX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SwitchX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        Themer.setTint(this, ThemeColor.accentColor(context));
    }

    @Override
    public boolean isShown() {
        return getParent() != null && getVisibility() == View.VISIBLE;
    }
}
