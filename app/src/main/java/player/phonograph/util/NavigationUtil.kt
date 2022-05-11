package player.phonograph.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import player.phonograph.R
import player.phonograph.model.Genre
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.activities.AlbumDetailActivity
import player.phonograph.ui.activities.ArtistDetailActivity
import player.phonograph.ui.activities.GenreDetailActivity
import player.phonograph.ui.activities.PlaylistDetailActivity

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object NavigationUtil {
    @JvmStatic
    fun goToArtist(activity: Activity, artistId: Long) {
        activity.startActivity(
            Intent(activity, ArtistDetailActivity::class.java)
                .apply { putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artistId) }
        )
    }
    @JvmStatic
    fun goToArtist(activity: Activity, artistId: Long, vararg sharedElements: Pair<View, String>) {
        val intent =
            Intent(activity, ArtistDetailActivity::class.java)
                .apply { putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artistId) }
        if (sharedElements.isNotEmpty()) {
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *sharedElements).toBundle())
        } else {
            activity.startActivity(intent)
        }
    }

    @JvmStatic
    fun goToAlbum(activity: Activity, albumId: Long) {
        activity.startActivity(
            Intent(activity, AlbumDetailActivity::class.java)
                .apply { putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, albumId) }
        )
    }

    @JvmStatic
    fun goToAlbum(activity: Activity, albumId: Long, vararg sharedElements: Pair<View, String>) {
        val intent =
            Intent(activity, AlbumDetailActivity::class.java)
                .apply { putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, albumId) }
        if (sharedElements.isNotEmpty()) {
            activity.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *sharedElements).toBundle())
        } else {
            activity.startActivity(intent)
        }
    }

    fun goToGenre(activity: Activity, genre: Genre?, vararg sharedElements: Pair<*, *>?) {
        val intent = Intent(activity, GenreDetailActivity::class.java)
        intent.putExtra(GenreDetailActivity.EXTRA_GENRE, genre)
        activity.startActivity(intent)
    }

    fun goToPlaylist(activity: Activity, playlist: Playlist?, vararg sharedElements: Pair<*, *>?) {
        val intent = Intent(activity, PlaylistDetailActivity::class.java)
        intent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST, playlist)
        activity.startActivity(intent)
    }

    @JvmStatic
    fun openEqualizer(activity: Activity) {
        val sessionId = MusicPlayerRemote.audioSessionId
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(activity, activity.resources.getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show()
        } else {
            try {
                val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                activity.startActivityForResult(effects, 0)
            } catch (notFound: ActivityNotFoundException) {
                Toast.makeText(activity, activity.resources.getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
