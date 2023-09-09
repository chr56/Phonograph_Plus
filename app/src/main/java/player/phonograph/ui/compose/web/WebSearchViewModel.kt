/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.base.Navigator
import util.phonograph.tagsources.lastfm.LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmClientDelegate
import util.phonograph.tagsources.lastfm.LastFmTrack
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzClientDelegate
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzReleaseGroup
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WebSearchViewModel : ViewModel() {

    val navigator = Navigator<Page>(PageHome)

    private var clientDelegateLastFm: LastFmClientDelegate? = null
    fun clientDelegateLastFm(context: Context): LastFmClientDelegate {
        return if (clientDelegateLastFm != null) {
            clientDelegateLastFm!!
        } else {
            LastFmClientDelegate(context, viewModelScope).also { clientDelegateLastFm = it }
        }
    }

    private var clientDelegateMusicBrainz: MusicBrainzClientDelegate? = null
    fun clientDelegateMusicBrainz(context: Context): MusicBrainzClientDelegate {
        return if (clientDelegateMusicBrainz != null) {
            clientDelegateMusicBrainz!!
        } else {
            MusicBrainzClientDelegate(context, viewModelScope).also { clientDelegateMusicBrainz = it }
        }
    }


    var selectorMode: Boolean = false

    fun exit(webSearchActivity: WebSearchActivity) {
        val page = navigator.currentPage.value
        val json = Json { ignoreUnknownKeys = true }
        when (page) {

            is PageDetail.MusicBrainzDetail -> {
                val musicBrainzModel = page.detail.value
                if (musicBrainzModel != null) {
                    val type = when (musicBrainzModel) {
                        is MusicBrainzArtist       -> MUSICBRAINZ_ARTIST
                        is MusicBrainzRecording    -> MUSICBRAINZ_RECORDING
                        is MusicBrainzRelease      -> MUSICBRAINZ_RELEASE
                        is MusicBrainzReleaseGroup -> MUSICBRAINZ_RELEASE_GROUP
                        else                       -> null
                    }
                    webSearchActivity.setResult(
                        RESULT_OK,
                        Intent().apply {
                            putExtra(EXTRA_SELECTOR_RESULT, type)
                            putExtra(EXTRA_DATA, json.encodeToString(musicBrainzModel))
                        }
                    )
                }
                webSearchActivity.finish()
            }

            is PageDetail.LastFmDetail      -> {
                val lastFmModel = page.detail.value
                if (lastFmModel != null) {
                    val type = when (lastFmModel) {
                        is LastFmAlbum  -> LASTFM_ALBUM
                        is LastFmArtist -> LASTFM_ARTIST
                        is LastFmTrack  -> LASTFM_TRACK
                    }
                    webSearchActivity.setResult(
                        RESULT_OK,
                        Intent().apply {
                            putExtra(EXTRA_SELECTOR_RESULT, type)
                            putExtra(EXTRA_DATA, json.encodeToString(lastFmModel))
                        }
                    )
                }
                webSearchActivity.finish()
            }

            else                            -> webSearchActivity.setResult(RESULT_CANCELED)
        }
    }

}
