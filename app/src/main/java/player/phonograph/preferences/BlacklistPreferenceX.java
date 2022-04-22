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
public class BlacklistPreferenceX extends DialogPreference {
    @Keep
    public BlacklistPreferenceX(Context context) {
        super(context);
        init();
    }

    @Keep
    public BlacklistPreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Keep
    public BlacklistPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Keep
    public BlacklistPreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private void init(){
        setLayoutResource(R.layout.x_preference);
    }
}