package player.phonograph.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Keep;
import androidx.preference.DialogPreference;

import player.phonograph.R;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Keep
public class NowPlayingScreenPreferenceX extends DialogPreference {
    @Keep
    public NowPlayingScreenPreferenceX(Context context) {
        super(context);
        init();
    }

    @Keep
    public NowPlayingScreenPreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Keep
    public NowPlayingScreenPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Keep
    public NowPlayingScreenPreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private void init() {
        setLayoutResource(R.layout.x_preference);
    }
}