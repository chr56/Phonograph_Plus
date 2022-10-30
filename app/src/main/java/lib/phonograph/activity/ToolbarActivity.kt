/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import mt.tint.viewtint.applyOverflowMenuTint
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.WindowDecorActionBar
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.ToolbarWidgetWrapper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem

/**
 * An abstract class providing material activity with toolbar
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ToolbarActivity() : PermissionActivity() {

    //
    // Toolbar & Actionbar
    //
    protected var supportToolbar: Toolbar? = null
        private set(value) {
            field = value
            super.setSupportActionBar(value)
        }
        get() {
            return getSupportActionBarView(supportActionBar) ?: field
        }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        this.supportToolbar = toolbar
        this.supportToolbar?.setOnMenuItemClickListener(this::onBackClick)
        this.supportToolbar?.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun onBackClick(item: MenuItem): Boolean =
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            true
        } else {
            false
        }

    protected open fun getSupportActionBarView(ab: ActionBar?): Toolbar? {
        return if (ab == null || ab !is WindowDecorActionBar) null else try {
            var field = WindowDecorActionBar::class.java.getDeclaredField("mDecorToolbar")
            field.isAccessible = true
            val wrapper = field[ab] as ToolbarWidgetWrapper
            field = ToolbarWidgetWrapper::class.java.getDeclaredField("mToolbar")
            field.isAccessible = true
            field[wrapper] as Toolbar
        } catch (t: Throwable) {
            throw RuntimeException(
                "Failed to retrieve Toolbar from AppCompat support ActionBar: ${t.message}", t)
        }
    }

    //
    // Menu (Tint)
    //
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        supportToolbar?.let { applyOverflowMenuTint(this, it, accentColor) }
        return super.onPrepareOptionsMenu(menu)
    }

    //
    // Physics Keys
    //

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            showOverflowMenu()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    protected open fun showOverflowMenu() {}
}
