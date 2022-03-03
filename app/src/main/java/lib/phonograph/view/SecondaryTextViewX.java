/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import util.mdcolor.pref.ThemeColor;


/**
 * @author Aidan Follestad (afollestad)
 */
public class SecondaryTextViewX extends AppCompatTextView {

    public SecondaryTextViewX(Context context) {
        super(context);
        init(context, null);
    }

    public SecondaryTextViewX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SecondaryTextViewX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setTextColor(ThemeColor.textColorSecondary(context));
    }
}
