/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

interface QueryParameter {
    fun check(): Boolean
    fun toAction(): WebSearchAction
}

data class MusicbrainzQueryParameter(var target: MusicBrainzAction.Target, var query: String) : QueryParameter {
    override fun check(): Boolean = query.isNotEmpty()
    override fun toAction(): MusicBrainzAction.Search = MusicBrainzAction.Search(target, query)
}

data class LastFmQueryParameter(
    val target: LastFmAction.Target,
    val albumQuery: String?,
    val artistQuery: String?,
    val trackQuery: String?,
) : QueryParameter {
    override fun check(): Boolean = when (target) {
        LastFmAction.Target.Track  -> trackQuery != null
        LastFmAction.Target.Artist -> artistQuery != null
        LastFmAction.Target.Album  -> albumQuery != null
    }

    override fun toAction(): LastFmAction.Search {
        return LastFmAction.Search(target, albumQuery.orEmpty(), artistQuery.orEmpty(), trackQuery.orEmpty())
    }
}
