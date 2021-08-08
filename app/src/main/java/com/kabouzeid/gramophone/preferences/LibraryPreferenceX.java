package com.kabouzeid.gramophone.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.kabouzeid.gramophone.R;

public class LibraryPreferenceX extends DialogPreference {
    public LibraryPreferenceX(Context context) {
        super(context);
        init();
    }

    public LibraryPreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LibraryPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public LibraryPreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private void init() {
        setLayoutResource(R.layout.x_preference);
    }
}
