package com.kabouzeid.phonograph.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.kabouzeid.phonograph.R;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class NowPlayingScreenPreferenceX extends DialogPreference {
    public NowPlayingScreenPreferenceX(Context context) {
        super(context);
        init();
    }

    public NowPlayingScreenPreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NowPlayingScreenPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public NowPlayingScreenPreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private void init() {
        setLayoutResource(R.layout.x_preference);
    }
}