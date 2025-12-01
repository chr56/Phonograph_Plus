/*
 * Copyright (c) 2022~2024 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.basis

import player.phonograph.util.theme.ThemeSettingsDelegate
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
        setTheme(ThemeSettingsDelegate.styleRes())

        setupSystemBars()

        updateTaskDescriptionColor()

        observeTheme()
    }

    private fun observeTheme() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                ThemeSettingsDelegate.styleRes.distinctUntilChanged().drop(1).collect { id ->
                    setTheme(id)
                    requireRecreate()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                ThemeSettingsDelegate.primaryColor.distinctUntilChanged().drop(1).collect { id ->
                    delay(500)
                    requireRecreate()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                ThemeSettingsDelegate.accentColor.distinctUntilChanged().drop(1).collect { id ->
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
