/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.musicbrainz

import util.phonograph.tagsources.QueryParameter

data class MusicbrainzQueryParameter(var target: MusicBrainzAction.Target, var query: String) : QueryParameter {
    override fun check(): Boolean = query.isNotEmpty()
    override fun toAction(): MusicBrainzAction.Search = MusicBrainzAction.Search(target, query)
}