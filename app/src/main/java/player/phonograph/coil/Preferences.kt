/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import player.phonograph.settings.Setting

object IgnoreMediaStorePreference {
    var ignoreMediaStore: Boolean = Setting.instance.ignoreMediaStoreArtwork

    // todo
    fun refresh() {
        ignoreMediaStore = Setting.instance.ignoreMediaStoreArtwork
    }
}
