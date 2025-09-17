package player.phonograph.util

import player.phonograph.R
import player.phonograph.model.Genre
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Artists
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.album.AlbumDetailActivity
import player.phonograph.ui.modules.artist.ArtistDetailActivity
import player.phonograph.ui.modules.genre.GenreDetailActivity
import player.phonograph.ui.modules.playlist.PlaylistDetailActivity
import player.phonograph.util.text.splitMultiTag
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

    suspend fun goToArtist(context: Context, artistName: String, sharedElements: Array<Pair<View, String>>? = null) {
        val artists = withContext(Dispatchers.IO) {
            splitMultiTag(artistName).flatMap { Artists.searchByName(context, it) }.toSet().toList()
        }
        when (artists.size) {
            0    -> Toast.makeText(context, R.string.msg_empty, Toast.LENGTH_SHORT).show()
            1    -> goToArtist(context, artists.first().id, sharedElements)
            else -> {
                if (context is FragmentActivity) {
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
        }
    }

    fun goToArtist(context: Context, artistId: Long, sharedElements: Array<Pair<View, String>>? = null) {
        val intent = ArtistDetailActivity.launchIntent(context.applicationContext, artistId)
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
        context.startActivity(AlbumDetailActivity.launchIntent(context.applicationContext, albumId))

    fun goToAlbum(context: Context, albumId: Long, vararg sharedElements: Pair<View, String>) {
        val intent = AlbumDetailActivity.launchIntent(context.applicationContext, albumId)
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
        context.startActivity(GenreDetailActivity.launchIntent(context, genre))

    @Suppress("UNUSED_PARAMETER")
    fun goToPlaylist(context: Context, playlist: Playlist, vararg sharedElements: Pair<*, *>?) =
        context.startActivity(PlaylistDetailActivity.launchIntent(context, playlist))

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
