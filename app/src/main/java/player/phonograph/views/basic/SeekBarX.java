package player.phonograph.views.basic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatSeekBar;

import chr_56.MDthemer.core.ThemeColor;
import chr_56.MDthemer.core.Themer;


/**
 * @author Aidan Follestad (afollestad)
 */
public class SeekBarX extends AppCompatSeekBar {

    public SeekBarX(Context context) {
        super(context);
        init(context, null);
    }

    public SeekBarX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SeekBarX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public SeekBarX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        init(context, attrs);
//    }

    private void init(Context context, AttributeSet attrs) {
        Themer.setTint(this, ThemeColor.accentColor(context));
    }
}
