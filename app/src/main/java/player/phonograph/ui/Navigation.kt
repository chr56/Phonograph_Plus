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
import androidx.fragment.app.FragmentActivity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun goToArtistDetail(context: Context, artistId: Long) =
    context.startActivity(ArtistDetailActivity.launchIntent(context.applicationContext, artistId))

fun goToArtistDetail(context: Context, artists: List<Artist>) {
    if (artists.isEmpty()) return
    if (artists.size > 1 && context is FragmentActivity) {
        AlertDialog.Builder(context)
            .setTitle(R.string.label_artists)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setSingleChoiceItems(artists.map { it.name }.toTypedArray(), -1) { dialog, selected ->
                goToArtistDetail(context, artists[selected].id)
                dialog.dismiss()
            }
            .show().tintButtons()
    } else {
        goToArtistDetail(context, artists.first().id)
    }
}

private suspend fun findArtists(context: Context, names: Collection<String>): Set<Artist> =
    withContext(Dispatchers.IO) {
        names.flatMap { Artists.searchByName(context, it) }.toSet()
    }

suspend fun goToArtistDetail(context: Context, album: Album) {
    if (!album.artistName.isNullOrEmpty()) {
        val artists: List<Artist> = findArtists(context, listOf(album.artistName)).toList()
        goToArtistDetail(context, artists)
    } else {
        goToArtistDetail(context, album.artistId)
    }
}

suspend fun goToArtistDetail(context: Context, song: Song) {
    val relationship = RelationshipResolver.fromSettings(context).solve(song)
    val artists: List<Artist> = findArtists(context, relationship.artists).toList()
    if (artists.isNotEmpty()) {
        goToArtistDetail(context, artists)
    } else {
        goToArtistDetail(context, song.artistId)
    }
}

fun goToAlbumDetail(context: Context, albumId: Long) =
    context.startActivity(AlbumDetailActivity.launchIntent(context.applicationContext, albumId))

fun goToGenreDetail(context: Context, genre: Genre) =
    context.startActivity(GenreDetailActivity.launchIntent(context.applicationContext, genre))

fun goToPlaylistDetail(context: Context, playlist: Playlist) =
    context.startActivity(PlaylistDetailActivity.launchIntent(context.applicationContext, playlist))

fun navigateToStorageSetting(context: Context) {
    val uri = Uri.fromParts("package", context.packageName, null)
    val intent = Intent().apply {
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