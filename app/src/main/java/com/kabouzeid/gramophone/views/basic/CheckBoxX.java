package com.kabouzeid.gramophone.views.basic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatCheckBox;

import com.kabouzeid.appthemehelper.ATH;
import com.kabouzeid.appthemehelper.ThemeStore;

/**
 * @author Aidan Follestad (afollestad)
 */
public class CheckBoxX extends AppCompatCheckBox {

    public CheckBoxX(Context context) {
        super(context);
        init(context, null);
    }

    public CheckBoxX(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CheckBoxX(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        ATH.setTint(this, ThemeStore.accentColor(context));
    }
}
