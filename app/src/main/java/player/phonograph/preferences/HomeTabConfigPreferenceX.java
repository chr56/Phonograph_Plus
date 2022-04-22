package player.phonograph.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Keep;
import androidx.preference.DialogPreference;

import player.phonograph.R;

@Keep
public class HomeTabConfigPreferenceX extends DialogPreference {
    @Keep
    public HomeTabConfigPreferenceX(Context context) {
        super(context);
        init();
    }

    @Keep
    public HomeTabConfigPreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Keep
    public HomeTabConfigPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Keep
    public HomeTabConfigPreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private void init() {
        setLayoutResource(R.layout.x_preference);
    }
}
