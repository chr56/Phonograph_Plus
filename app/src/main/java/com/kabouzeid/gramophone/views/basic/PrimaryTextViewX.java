package com.kabouzeid.gramophone.views.basic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.kabouzeid.appthemehelper.ThemeStore;

/**
 * @author Aidan Follestad (afollestad)
 */
public class PrimaryTextViewX extends AppCompatTextView {

    public PrimaryTextViewX(Context context) {
        super(context);
        init(context, null);
    }

    public PrimaryTextViewX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PrimaryTextViewX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setTextColor(ThemeStore.textColorPrimary(context));
    }
}
