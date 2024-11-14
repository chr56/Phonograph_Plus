/*
 * Copyright (c) 2022~2024 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.basis

import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.accentColorFlow
import player.phonograph.util.theme.primaryColorFlow
import player.phonograph.util.theme.setupSystemBars
import player.phonograph.util.theme.updateTaskDescriptionColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * An abstract class providing material activity (no toolbar)
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ThemeActivity : MultiLanguageActivity() {
    private var createTime: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        createTime = System.currentTimeMillis()

        // theme
        setTheme(ThemeSetting.themeStyle(this))

        setupSystemBars()

        updateTaskDescriptionColor()

        observeTheme()
    }

    private fun observeTheme() {
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                Setting(this@ThemeActivity)[Keys.theme].flow.distinctUntilChanged().drop(1).collect {
                    setTheme(ThemeSetting.themeStyle(this@ThemeActivity))
                    requireRecreate()
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                primaryColorFlow(this@ThemeActivity).distinctUntilChanged().drop(1).collect {
                    delay(500)
                    requireRecreate()
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                accentColorFlow(this@ThemeActivity).distinctUntilChanged().drop(1).collect {
                    delay(500)
                    requireRecreate()
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            recreateEffect.collect {
                lifecycle.withResumed {
                    Handler(Looper.getMainLooper()).post { recreate() }
                }
            }
        }
    }

    private val recreateEffect: MutableSharedFlow<Unit> = MutableSharedFlow()
    protected suspend fun requireRecreate() {
        recreateEffect.emit(Unit)
    }

    //
    // SnackBar holder
    //
    protected open val snackBarContainer: View get() = window.decorView

}
