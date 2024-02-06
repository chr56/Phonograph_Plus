/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import mms.lastfm.LastFmAction
import mms.musicbrainz.MusicBrainzAction
import mms.musicbrainz.MusicBrainzModel
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.ui.modules.web.WebSearchActionConst.LASTFM_SEARCH
import player.phonograph.ui.modules.web.WebSearchActionConst.MUSICBRAINZ_SEARCH
import player.phonograph.ui.modules.web.WebSearchActionConst.MUSICBRAINZ_VIEW
import player.phonograph.util.parcelableExtra
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun luceneQuery(song: Song): String = buildString {
    append(""""${song.title}"""")
    if (!song.artistName.isNullOrEmpty()) append(""" AND artist:"${song.artistName}"""")
    if (!song.albumName.isNullOrEmpty()) append(""" AND release:"${song.albumName}"""")
}

private fun luceneQuery(album: Album): String = buildString {
    append(""""${album.title}"""")
    if (!album.artistName.isNullOrEmpty()) append(""" AND artist:"${album.artistName}"""")
}

const val EXTRA_ACTION_TYPE = "ACTION"
const val EXTRA_DATA = "DATA"
const val EXTRA_SELECTOR_MODE = "SELECTOR"

const val EXTRA_SELECTOR_RESULT = "SELECTOR_RESULT"
const val MUSICBRAINZ_ARTIST = "MusicBrainzArtist"
const val MUSICBRAINZ_RECORDING = "MusicBrainzRecording"
const val MUSICBRAINZ_RELEASE = "MusicBrainzRelease"
const val MUSICBRAINZ_RELEASE_GROUP = "MusicBrainzReleaseGroup"
const val LASTFM_ALBUM = "LastFmAlbum"
const val LASTFM_ARTIST = "LastFmArtist"
const val LASTFM_TRACK = "LastFmTrack"


/**
 * [WebSearchActivity] launching intent factory
 */
object WebSearchLauncher {

    /**
     * default launch intent
     */
    fun launchIntent(context: Context): Intent =
        Intent(context, WebSearchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }

    fun searchLastFmAlbum(context: Context, item: Album?): Intent =
        launchIntent(context).apply {
            putExtra(EXTRA_ACTION_TYPE, LASTFM_SEARCH)
            putExtra(
                EXTRA_DATA,
                LastFmAction.Search(
                    LastFmAction.Target.Album,
                    album = item?.title.orEmpty(),
                    artist = item?.artistName.orEmpty()
                )
            )
        }

    fun searchLastFmArtist(context: Context, item: Artist?): Intent =
        launchIntent(context).apply {
            putExtra(EXTRA_ACTION_TYPE, LASTFM_SEARCH)
            putExtra(
                EXTRA_DATA,
                LastFmAction.Search(
                    LastFmAction.Target.Artist,
                    artist = item?.name.orEmpty()
                )
            )
        }

    fun searchLastFmSong(context: Context, item: Song?): Intent =
        launchIntent(context).apply {
            putExtra(EXTRA_ACTION_TYPE, LASTFM_SEARCH)
            putExtra(
                EXTRA_DATA,
                LastFmAction.Search(
                    LastFmAction.Target.Track,
                    album = item?.albumName.orEmpty(),
                    artist = item?.artistName.orEmpty(),
                    track = item?.title.orEmpty()
                )
            )
        }

    fun searchMusicBrainzAlbum(context: Context, item: Album?): Intent =
        launchIntent(context).apply {
            if (item != null) {
                putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_SEARCH)
                putExtra(
                    EXTRA_DATA,
                    MusicBrainzAction.Search(MusicBrainzAction.Target.ReleaseGroup, luceneQuery(item))
                )
            }
        }

    fun searchMusicBrainzArtist(context: Context, item: Artist?): Intent =
        launchIntent(context).apply {
            if (item != null) {
                putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_SEARCH)
                putExtra(
                    EXTRA_DATA,
                    MusicBrainzAction.Search(MusicBrainzAction.Target.Artist, item.name)
                )
            }
        }

    fun searchMusicBrainzSong(context: Context, item: Song?): Intent =
        launchIntent(context).apply {
            if (item != null) {
                putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_SEARCH)
                putExtra(
                    EXTRA_DATA,
                    MusicBrainzAction.Search(MusicBrainzAction.Target.Recording, luceneQuery(item))
                )
            }
        }

    fun viewIntentMusicBrainzReleaseGroup(context: Context, mbid: String): Intent =
        launchIntent(context).apply {
            putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
            putExtra(
                EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.ReleaseGroup, mbid)
            )
        }

    fun viewIntentMusicBrainzRelease(context: Context, mbid: String): Intent =
        launchIntent(context).apply {
            putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
            putExtra(
                EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.Release, mbid)
            )
        }

    fun viewIntentMusicBrainzArtist(context: Context, mbid: String): Intent =
        launchIntent(context).apply {
            putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
            putExtra(
                EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.Artist, mbid)
            )
        }

    fun viewIntentMusicBrainzRecording(context: Context, mbid: String): Intent =
        launchIntent(context).apply {
            putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
            putExtra(
                EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.Recording, mbid)
            )
        }

    fun Intent.selectable(): Intent {
        putExtra(EXTRA_SELECTOR_MODE, true)
        return this
    }

}

object WebSearchActionConst {
    const val MUSICBRAINZ_SEARCH = "musicbrainz_search"
    const val MUSICBRAINZ_VIEW = "musicbrainz_view"
    const val LASTFM_SEARCH = "lastfm_search"
    const val LASTFM_VIEW_ARTIST = "lastfm_view_artist"
    const val LASTFM_VIEW_ALBUM = "lastfm_view_album"
    const val LASTFM_VIEW_TRACK = "lastfm_view_track"
}

internal fun executeCommand(
    activity: WebSearchActivity,
    intent: Intent,
) {

    val viewModel = activity.viewModel
    val navigator = viewModel.navigator

    when (intent.getStringExtra(EXTRA_ACTION_TYPE)) {

        MUSICBRAINZ_VIEW   ->
            intent.parcelableExtra<MusicBrainzAction.View>(EXTRA_DATA)?.also { action ->
                val clientDelegate = viewModel.clientDelegateMusicBrainz(activity)
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val result = clientDelegate.request(activity, action).await()
                    val page = PageDetail.MusicBrainzDetail(result as? MusicBrainzModel)
                    navigator.navigateTo(page)
                }
            }

        MUSICBRAINZ_SEARCH ->
            intent.parcelableExtra<MusicBrainzAction.Search>(EXTRA_DATA)?.also { action ->
                val page = PageSearch.MusicBrainzSearch(action.target, action.query)
                navigator.navigateTo(page)
            }

        LASTFM_SEARCH      ->
            intent.parcelableExtra<LastFmAction.Search>(EXTRA_DATA).also {
                val page = PageSearch.LastFmSearch(
                    albumQuery = it?.album,
                    artistQuery = it?.artist,
                    trackQuery = it?.track,
                    target = it?.target ?: LastFmAction.Target.Album
                )
                navigator.navigateTo(page)
            }
    }
}