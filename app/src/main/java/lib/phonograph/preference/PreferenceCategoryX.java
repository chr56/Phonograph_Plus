/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import player.phonograph.R;

import util.mdcolor.pref.ThemeColor;

public class PreferenceCategoryX extends PreferenceCategory {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PreferenceCategoryX(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public PreferenceCategoryX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public PreferenceCategoryX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PreferenceCategoryX(Context context) {
        super(context);
        init(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView mTitle = (TextView) holder.itemView;
        mTitle.setTextColor(ThemeColor.accentColor(getContext()));
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.x_preference_category);
    }
}
