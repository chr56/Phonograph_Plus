/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.module


import androidx.preference.PreferenceManager
import player.phonograph.BaseApp

@Suppress("ObjectPropertyName")
object IgnoreMediaStorePreference {
    private var _ignoreMediaStore = init()
    var ignoreMediaStore: Boolean
        get() = _ignoreMediaStore
        set(newValue) {
            _ignoreMediaStore = newValue
        }

    private fun init(): Boolean =
        PreferenceManager.getDefaultSharedPreferences(BaseApp.instance)
            .getBoolean(IGNORE_MEDIA_STORE_ARTWORK, false)

    const val IGNORE_MEDIA_STORE_ARTWORK = "ignore_media_store_artwork"
}