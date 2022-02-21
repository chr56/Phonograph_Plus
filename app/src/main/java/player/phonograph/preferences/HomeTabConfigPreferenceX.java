package player.phonograph.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import player.phonograph.R;

public class HomeTabConfigPreferenceX extends DialogPreference {
    public HomeTabConfigPreferenceX(Context context) {
        super(context);
        init();
    }

    public HomeTabConfigPreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomeTabConfigPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public HomeTabConfigPreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private void init() {
        setLayoutResource(R.layout.x_preference);
    }
}
