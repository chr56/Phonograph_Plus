/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import android.content.Context

private const val MAX_TRY = 3

fun replaceFavoriteSongDelegate(context: Context) {
    var maxTry = MAX_TRY
    while (maxTry > 0) {
        maxTry--
        if (FavoriteSongs.recreate(context)) {
            break
        }
    }
}
