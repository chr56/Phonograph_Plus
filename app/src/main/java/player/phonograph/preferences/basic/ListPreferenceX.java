package player.phonograph.preferences.basic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import player.phonograph.R;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ListPreferenceX extends ListPreference {

    public ListPreferenceX(Context context) {
        super(context);
        init(context, null);
    }

    public ListPreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ListPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public ListPreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.x_preference);
        if (getSummary() == null || getSummary().toString().trim().isEmpty())
            setSummary("%s");
    }
}
