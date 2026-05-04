/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.lastfm

import coil.map.Mapper
import coil.request.Options
import mms.lastfm.largestUrl

class LastFmImageBundleMapper : Mapper<LastFmImageBundle, String> {
    override fun map(data: LastFmImageBundle, options: Options): String? {
        return data.images.largestUrl(data.preferredSize)
    }
}