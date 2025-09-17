/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import android.content.Context

abstract class Delegated<I> {
    @Volatile
    private var _delegate: I? = null
    protected fun delegate(context: Context): I =
        _delegate ?: synchronized(this) {
            _delegate ?: run {
                val delegate: I = onCreateDelegate(context)
                _delegate = delegate
                delegate
            }
        }

    protected abstract fun onCreateDelegate(context: Context): I

}