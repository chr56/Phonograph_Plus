/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import mms.AbsClientDelegate.ExceptionHandler
import mms.lastfm.LastFmAlbum
import mms.lastfm.LastFmArtist
import mms.lastfm.LastFmClientDelegate
import mms.lastfm.LastFmTrack
import mms.musicbrainz.MusicBrainzArtist
import mms.musicbrainz.MusicBrainzClientDelegate
import mms.musicbrainz.MusicBrainzRecording
import mms.musicbrainz.MusicBrainzRelease
import mms.musicbrainz.MusicBrainzReleaseGroup
import player.phonograph.USER_AGENT
import player.phonograph.ui.compose.Navigator
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import kotlinx.serialization.json.Json

class WebSearchViewModel : ViewModel() {

    val navigator = Navigator<Page>(PageHome)

    private val errorReporter = object : ExceptionHandler {
        override fun reportError(e: Throwable, tag: String, message: String) =
            player.phonograph.foundation.reportError(e, tag, message)

        override fun warning(tag: String, message: String) =
            player.phonograph.foundation.warning(tag, message)

    }

    private var clientDelegateLastFm: LastFmClientDelegate? = null
    fun clientDelegateLastFm(context: Context): LastFmClientDelegate {
        return if (clientDelegateLastFm != null) {
            clientDelegateLastFm!!
        } else {
            val delegate = LastFmClientDelegate(context, USER_AGENT, errorReporter, viewModelScope)
            clientDelegateLastFm = delegate
            delegate
        }
    }

    private var clientDelegateMusicBrainz: MusicBrainzClientDelegate? = null
    fun clientDelegateMusicBrainz(context: Context): MusicBrainzClientDelegate {
        return if (clientDelegateMusicBrainz != null) {
            clientDelegateMusicBrainz!!
        } else {
            val delegate = MusicBrainzClientDelegate(context, USER_AGENT, errorReporter, viewModelScope)
            clientDelegateMusicBrainz = delegate
            delegate
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
