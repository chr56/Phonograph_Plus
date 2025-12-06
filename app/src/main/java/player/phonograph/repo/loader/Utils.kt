/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import android.content.Context


fun replaceFavoriteSongDelegate(context: Context) {
    FavoriteSongs.recreate(context)
}
