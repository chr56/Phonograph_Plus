/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.module

import player.phonograph.BaseApp

object IgnoreMediaStorePreference {
    private val IGNORE_MEDIA_STORE_ARTWORK = "ignore_media_store_artwork"

    private val impl: BooleanIsolatePreference =
        BooleanIsolatePreference(IGNORE_MEDIA_STORE_ARTWORK, false, BaseApp.instance)

    var ignoreMediaStore: Boolean
        get() = impl.read()
        set(newValue) {
            impl.write(newValue)
        }
}