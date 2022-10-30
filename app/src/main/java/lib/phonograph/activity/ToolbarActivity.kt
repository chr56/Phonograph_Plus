/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import mt.tint.viewtint.applyOverflowMenuTint
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.WindowDecorActionBar
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.ToolbarWidgetWrapper
import android.util.Log
import android.view.Menu

/**
 * An abstract class providing material activity with toolbar
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ToolbarActivity : PermissionActivity() {

    //
    // Toolbar & Actionbar
    //

    val supportToolbar: Toolbar?
        get() = customToolbar ?: reflectSupportActionBarView(supportActionBar)

    private var customToolbar: Toolbar? = null

    override fun setSupportActionBar(toolbar: Toolbar?) {
        this.customToolbar = toolbar
        super.setSupportActionBar(toolbar)
        setupToolbarBehavior()
    }

    protected open fun setupToolbarBehavior() {
        supportToolbar?.setOnMenuItemClickListener {
            if (it.itemId == android.R.id.home) {
                navigateUp()
                true
            } else {
                false
            }
        }
        supportToolbar?.setNavigationOnClickListener { navigateUp() }
    }

    protected open fun navigateUp() {
        if (!isTaskRoot) {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    //
    // Menu (Tint)
    //
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        supportToolbar?.let { applyOverflowMenuTint(this, it, accentColor) }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun reflectSupportActionBarView(ab: ActionBar?): Toolbar? {
        return when {
            ab == null                  -> null
            ab !is WindowDecorActionBar -> null
            reflectWindowDecorActionBar -> reflectWindowDecorActionBar(ab)
            else                        -> null
        }
    }

    /**
     * try to reflect WindowDecorActionBar to get default toolbar
     */
    protected open val reflectWindowDecorActionBar = false

    companion object {
        private fun reflectWindowDecorActionBar(ab: ActionBar?): Toolbar? {
            return try {
                var field = WindowDecorActionBar::class.java.getDeclaredField("mDecorToolbar")
                field.isAccessible = true
                val wrapper = field[ab] as ToolbarWidgetWrapper
                field = ToolbarWidgetWrapper::class.java.getDeclaredField("mToolbar")
                field.isAccessible = true
                field[wrapper] as Toolbar
            } catch (e: Throwable) {
                val tag = "ReflectWindowDecor"
                Log.e(tag, "reflectWindowDecorActionBar", e)
                Log.e(tag, "Failed to retrieve Toolbar from AppCompat support ActionBar")
                null
            }
        }
    }
}
