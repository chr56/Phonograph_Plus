package com.kabouzeid.phonograph.preferences.basic;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import androidx.preference.Preference;

import com.kabouzeid.phonograph.R;


/**
 * @author Aidan Follestad (afollestad)
 */
public class PreferenceX extends Preference {

    public PreferenceX(Context context) {
        super(context);
        init(context, null);
    }

    public PreferenceX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PreferenceX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.x_preference);
    }
}