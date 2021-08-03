package com.kabouzeid.appthemehelper;

import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid)
 */
public abstract class ATHActivity extends AppCompatActivity {

    private long updateTime = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateTime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ATH.didThemeValuesChange(this, updateTime)) {
            onThemeChanged();
        }
    }

    public void onThemeChanged() {
        postRecreate();
    }

    public void postRecreate() {
        // hack to prevent java.lang.RuntimeException: Performing pause of activity that is not resumed
        // makes sure recreate() is called right after and not in onResume()
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                recreate();
            }
        });
    }
}