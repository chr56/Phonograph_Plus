package com.kabouzeid.phonograph.preferences.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.kabouzeid.phonograph.R;
import com.kabouzeid.phonograph.views.basic.BorderCircleView;

/**
 * @author Aidan Follestad (afollestad)
 */
public class ColorPreferenceX extends Preference {

    private View mView;
    private int color;
    private int border;

    public ColorPreferenceX(Context context) {
        this(context, null, 0);
        init(context, null);
    }

    public ColorPreferenceX(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init(context, attrs);
    }

    public ColorPreferenceX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);

    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.x_preference);
        setWidgetLayoutResource(R.layout.x_preference_color);
        setPersistent(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mView = holder.itemView;
        invalidateColor();
    }

    public void setColor(int color, int border) {
        this.color = color;
        this.border = border;
        invalidateColor();
    }

    private void invalidateColor() {
        if (mView != null) {
            BorderCircleView circle = (BorderCircleView) mView.findViewById(R.id.circle);
            if (this.color != 0) {
                circle.setVisibility(View.VISIBLE);
                circle.setBackgroundColor(color);
                circle.setBorderColor(border);
            } else {
                circle.setVisibility(View.GONE);
            }
        }
    }
}