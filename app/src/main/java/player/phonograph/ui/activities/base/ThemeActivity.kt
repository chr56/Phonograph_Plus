package player.phonograph.ui.activities.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.WindowDecorActionBar
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.ToolbarWidgetWrapper
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer
import util.mddesign.util.ColorUtil
import util.mddesign.util.MenuTinter
import player.phonograph.R
import player.phonograph.util.PreferenceUtil
import player.phonograph.util.Util

// todo remove Platform check

/**
 * An abstract class dealing theme
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ThemeActivity : AppCompatActivity() {
    private var createTime: Long = -1
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createTime = System.currentTimeMillis()
        setTheme(PreferenceUtil.getInstance(this).generalTheme)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    override fun onResume() {
        super.onResume()
        if (Themer.didThemeValuesChange(this, createTime)) {
            postRecreate()
        }
    }

    protected fun postRecreate() {
        // hack to prevent java.lang.RuntimeException: Performing pause of activity that is not resumed
        // makes sure recreate() is called right after and not in onResume()
        Handler().post { recreate() }
    }

    //
    // User Interface
    //
    protected open fun setDrawUnderStatusbar() {
        Util.setAllowDrawUnderStatusBar(window)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Util.setAllowDrawUnderStatusBar(window)
//        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Util.setStatusBarTranslucent(window)
    }

    //
    // Toolbar & Actionbar
    // 
    override fun setSupportActionBar(toolbar: Toolbar?) {
        this.toolbar = toolbar
        super.setSupportActionBar(toolbar)
    }
    protected fun getToolbar(): Toolbar? {
        return getSupportActionBarView(supportActionBar)
    }
    fun getToolbarBackgroundColor(toolbar: Toolbar?): Int {
        toolbar?.let {
            if (toolbar.background is ColorDrawable) return (toolbar.background as ColorDrawable).color
        }
        return 0
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
                "Failed to retrieve Toolbar from AppCompat support ActionBar: " + t.message,
                t
            )
        }
    }

    //
    // Status Bar
    //
    /**
     * This will set the color of the view with the id "status_bar" on KitKat and Lollipop.
     * On Lollipop if no such view is found it will set the statusbar color using the native method.
     *
     * @param color the new statusbar color (will be shifted down on Lollipop and above)
     */
    open fun setStatusbarColor(color: Int) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val statusBar = window.decorView.rootView.findViewById<View>(R.id.status_bar)
        if (statusBar != null) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            statusBar.setBackgroundColor(ColorUtil.darkenColor(color))
//                } else {
//                    statusBar.setBackgroundColor(color)
//                }
        } else /* if (Build.VERSION.SDK_INT >= 21) */ {
            window.statusBarColor = ColorUtil.darkenColor(color)
        }
        setLightStatusbarAuto(color)
//        }
    }
    open fun setStatusbarColorAuto() {
        // we don't want to use statusbar color because we are doing the color darkening on our own to support KitKat
        setStatusbarColor(ThemeColor.primaryColor(this))
    }
    open fun setLightStatusbar(enabled: Boolean) {
        Themer.setLightStatusbar(this, enabled)
    }

    open fun setLightStatusbarAuto(bgColor: Int) {
        setLightStatusbar(ColorUtil.isColorLight(bgColor))
    }

    //
    // NavigationBar
    //
    open fun setNavigationbarColor(color: Int) {
        if (ThemeColor.coloredNavigationBar(this)) {
            Themer.setNavigationbarColor(this, color)
        } else {
            Themer.setNavigationbarColor(this, Color.BLACK)
        }
    }
    open fun setNavigationbarColorAuto() {
        setNavigationbarColor(ThemeColor.navigationBarColor(this))
    }

    //
    // Task Description
    //
    open fun setTaskDescriptionColor(@ColorInt color: Int) {
        Themer.setTaskDescriptionColor(this, color)
    }
    open fun setTaskDescriptionColorAuto() {
        setTaskDescriptionColor(ThemeColor.primaryColor(this))
    }

    //
    // Menu (Tint)
    //
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        MenuTinter.setMenuColor(this, getToolbar(), menu!!, MaterialColor.White._1000.asColor) //todo
        return super.onCreateOptionsMenu(menu)
    }
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        MenuTinter.applyOverflowMenuTint(this, getToolbar(), ThemeColor.accentColor(this))
        return super.onPrepareOptionsMenu(menu)
    }
}
