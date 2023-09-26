package player.phonograph.util

import player.phonograph.R
import player.phonograph.model.Genre
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.activities.AlbumDetailActivity
import player.phonograph.ui.activities.ArtistDetailActivity
import player.phonograph.ui.activities.GenreDetailActivity
import player.phonograph.ui.modules.playlist.PlaylistDetailActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.view.View
import android.widget.Toast

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object NavigationUtil {

    fun goToArtist(context: Context, artistId: Long) =
        context.startActivity(ArtistDetailActivity.launchIntent(context.applicationContext, artistId))

    fun goToArtist(context: Context, artistId: Long, vararg sharedElements: Pair<View, String>) {
        val intent = ArtistDetailActivity.launchIntent(context.applicationContext, artistId)
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
                activity.resources.getString(R.string.no_audio_ID),
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
                    activity.resources.getString(R.string.no_equalizer),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
