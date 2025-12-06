/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

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


    fun recreate(context: Context): Boolean {
        val oldDelegate: I?
        val newDelegate: I
        synchronized(this) {
            oldDelegate = _delegate
            newDelegate = onCreateDelegate(context)
            _delegate = newDelegate
        }
        (oldDelegate as? AutoCloseable)?.close()
        return oldDelegate != newDelegate
    }

}