package com.kabouzeid.appthemehelper.common;


import androidx.appcompat.widget.Toolbar;

import com.kabouzeid.appthemehelper.util.ToolbarContentTintHelper;

public class ATHActionBarActivity extends ATHToolbarActivity {

    @Override
    protected Toolbar getATHToolbar() {
        return ToolbarContentTintHelper.getSupportActionBarView(getSupportActionBar());
    }
}
