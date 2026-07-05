/*
 *  Copyright (c) 2022~2026 chr_56
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
import player.phonograph.ui.modules.album.AlbumDetailActivity
import player.phonograph.ui.modules.artist.ArtistDetailActivity
import player.phonograph.ui.modules.genre.GenreDetailActivity
import player.phonograph.ui.modules.playlist.PlaylistDetailActivity
import player.phonograph.ui.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun goToArtistDetail(context: Context, album: Album, sharedElements: Array<Pair<View, String>>? = null) {
    if (!album.artistName.isNullOrEmpty()) {
        val artists: List<Artist> = findArtists(context, listOf(album.artistName)).toList()
        goToArtistDetail(context, artists, sharedElements)
    } else {
        goToArtistDetail(context, album.artistId, sharedElements)
    }
}

suspend fun goToArtistDetail(context: Context, song: Song, sharedElements: Array<Pair<View, String>>? = null) {
    val relationship = RelationshipResolver.fromSettings(context).solve(song)
    val artists: List<Artist> = findArtists(context, relationship.artists).toList()
    if (artists.isNotEmpty()) {
        goToArtistDetail(context, artists, sharedElements)
    } else {
        goToArtistDetail(context, song.artistId, sharedElements)
    }
}

private suspend fun findArtists(context: Context, names: Collection<String>): Set<Artist> =
    withContext(Dispatchers.IO) {
        names.flatMap { Artists.searchByName(context, it) }.toSet()
    }

fun goToArtistDetail(context: Context, artists: List<Artist>, sharedElements: Array<Pair<View, String>>? = null) {
    if (artists.isEmpty()) return
    if (artists.size > 1 && context is FragmentActivity) {
        AlertDialog.Builder(context)
            .setTitle(R.string.label_artists)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setSingleChoiceItems(artists.map { it.name }.toTypedArray(), -1) { dialog, selected ->
                goToArtistDetail(context, artists[selected].id, sharedElements)
                dialog.dismiss()
            }
            .show().tintButtons()
    } else {
        goToArtistDetail(context, artists.first().id, sharedElements)
    }
}

fun goToArtistDetail(context: Context, artistId: Long, sharedElements: Array<Pair<View, String>>? = null) {
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

fun goToAlbumDetail(context: Context, albumId: Long, sharedElements: Array<Pair<View, String>>? = null) {
    val intent = AlbumDetailActivity.launchIntent(context.applicationContext, albumId)
    if (context is Activity && !sharedElements.isNullOrEmpty()) {
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

fun goToGenreDetail(context: Context, genre: Genre, sharedElements: Array<Pair<View, String>>? = null) =
    context.startActivity(GenreDetailActivity.launchIntent(context, genre))

fun goToPlaylistDetail(context: Context, playlist: Playlist, sharedElements: Array<Pair<View, String>>? = null) =
    context.startActivity(PlaylistDetailActivity.launchIntent(context, playlist))

fun navigateToStorageSetting(context: Context) {
    val uri = Uri.fromParts("package", context.packageName, null)
    val intent = Intent()
    intent.apply {
        if (SDK_INT >= VERSION_CODES.R) {
            action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            data = uri
        } else {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = uri
        }
    }
    try {
        context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "${e.message?.take(48)}", Toast.LENGTH_SHORT).show()
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
    }
}

fun navigateToAppDetailSetting(context: Context) {
    context.startActivity(
        Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
        }
    )
}