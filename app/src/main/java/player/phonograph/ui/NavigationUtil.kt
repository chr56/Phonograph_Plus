/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui

import player.phonograph.R
 import player.phonograph.mechanism.metadata.RelationshipResolver
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Artists
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.album.AlbumDetailActivity
import player.phonograph.ui.modules.artist.ArtistDetailActivity
import player.phonograph.ui.modules.genre.GenreDetailActivity
import player.phonograph.ui.modules.playlist.PlaylistDetailActivity
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object NavigationUtil {

    suspend fun goToArtist(context: Context, album: Album, sharedElements: Array<Pair<View, String>>? = null) {
        if (!album.artistName.isNullOrEmpty()) {
            val artists = findArtists(context, listOf(album.artistName)).toList()
            goToArtist(context, artists, sharedElements)
        } else {
            goToArtist(context, album.artistId, sharedElements)
        }
    }

    suspend fun goToArtist(context: Context, song: Song, sharedElements: Array<Pair<View, String>>? = null) {
        val relationship = RelationshipResolver.fromSettings(context).solve(song)
        val artists: List<Artist> = findArtists(context, relationship.artists).toList()
        if (artists.isNotEmpty()) {
            goToArtist(context, artists, sharedElements)
        } else {
            goToArtist(context, song.artistId, sharedElements)
        }
    }

    private suspend fun findArtists(context: Context, names: Collection<String>): Set<Artist> =
        withContext(Dispatchers.IO) {
            names.flatMap { Artists.searchByName(context, it) }.toSet()
        }

    fun goToArtist(context: Context, artists: List<Artist>, sharedElements: Array<Pair<View, String>>? = null) {
        if (artists.isEmpty()) Toast.makeText(context, R.string.msg_empty, Toast.LENGTH_SHORT).show()
        if (artists.size > 1 && context is FragmentActivity) {
            AlertDialog.Builder(context)
                .setTitle(R.string.label_artists)
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setSingleChoiceItems(artists.map { it.name }.toTypedArray(), -1) { dialog, selected ->
                    goToArtist(context, artists[selected].id, sharedElements)
                    dialog.dismiss()
                }
                .show().tintButtons()
        } else {
            goToArtist(context, artists.first().id, sharedElements)
        }
    }

    fun goToArtist(context: Context, artistId: Long, sharedElements: Array<Pair<View, String>>? = null) {
        val intent = ArtistDetailActivity.Companion.launchIntent(context.applicationContext, artistId)
        if (!sharedElements.isNullOrEmpty() && context is Activity) {
            context.startActivity(
                intent,
                ActivityOptionsCompat
                    .makeSceneTransitionAnimation(context, *sharedElements)
                    .toBundle()
            )
        } else {
            context.startActivity(intent)
        }
    }

    fun goToAlbum(context: Context, albumId: Long) =
        context.startActivity(AlbumDetailActivity.Companion.launchIntent(context.applicationContext, albumId))

    fun goToAlbum(context: Context, albumId: Long, vararg sharedElements: Pair<View, String>) {
        val intent = AlbumDetailActivity.Companion.launchIntent(context.applicationContext, albumId)
        if (sharedElements.isNotEmpty() && context is Activity) {
            context.startActivity(
                intent,
                ActivityOptionsCompat
                    .makeSceneTransitionAnimation(context, *sharedElements)
                    .toBundle()
            )
        } else {
            context.startActivity(intent)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun goToGenre(context: Context, genre: Genre, vararg sharedElements: Pair<*, *>?) =
        context.startActivity(GenreDetailActivity.Companion.launchIntent(context, genre))

    @Suppress("UNUSED_PARAMETER")
    fun goToPlaylist(context: Context, playlist: Playlist, vararg sharedElements: Pair<*, *>?) =
        context.startActivity(PlaylistDetailActivity.Companion.launchIntent(context, playlist))

    fun openEqualizer(activity: Activity) {
        val sessionId = MusicPlayerRemote.audioSessionId
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(
                activity,
                activity.resources.getString(R.string.err_no_audio_ID),
                Toast.LENGTH_LONG
            ).show()
        } else {
            try {
                activity.startActivityForResult(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }, 0
                )
            } catch (notFound: ActivityNotFoundException) {
                Toast.makeText(
                    activity,
                    activity.resources.getString(R.string.err_no_equalizer),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}