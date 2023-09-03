/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.lastfm

import coil.key.Keyer
import coil.request.Options

class LastFmImageBundleKeyer : Keyer<LastFmImageBundle> {
    override fun key(data: LastFmImageBundle, options: Options): String = "${data.owner}@${data.preferredSize?.name}"
}