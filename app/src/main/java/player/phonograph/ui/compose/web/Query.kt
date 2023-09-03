/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import kotlinx.coroutines.flow.MutableStateFlow

class Query {

    enum class Target {
        Artist,
        Release,
        Track,
        ;
    }

    enum class Source {
        LastFm,
        ;
    }


    val target: MutableStateFlow<Target> = MutableStateFlow(Target.Release)

    val source: MutableStateFlow<Source> = MutableStateFlow(Source.LastFm)

    val releaseQuery: MutableStateFlow<String?> = MutableStateFlow(null)
    val artistQuery: MutableStateFlow<String?> = MutableStateFlow(null)
    val trackQuery: MutableStateFlow<String?> = MutableStateFlow(null)

    fun action(): QueryAction {
        return when (target.value) {
            Target.Artist -> QueryAction.Artist(artistQuery.value.orEmpty())
            Target.Release -> QueryAction.Release(releaseQuery.value.orEmpty())
            Target.Track -> QueryAction.Track(trackQuery.value.orEmpty(), artistQuery.value.orEmpty())
        }
    }

    sealed class QueryAction {
        data class Artist(val name: String) : QueryAction()
        data class Release(val name: String) : QueryAction()
        data class Track(val name: String, val artist: String?) : QueryAction()
    }
}