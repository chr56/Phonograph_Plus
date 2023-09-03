/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.lastfm

import coil.map.Mapper
import coil.request.Options
import util.phonograph.tagsources.lastfm.largestUrl

class LastFmImageBundleMapper : Mapper<LastFmImageBundle, String> {
    override fun map(data: LastFmImageBundle, options: Options): String? {
        return data.images.largestUrl(data.preferredSize)
    }
}