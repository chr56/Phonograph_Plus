/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.database.mediastore

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import player.phonograph.helper.SortOrder

class MusicDatabaseDebugDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val items = listOf<String>(
            "GetAllSong", // 1
            "GetAllAlbum", // 2
            "GetAllAlbumWithSong", // 3
        )

        val resultDialog = MaterialDialog(requireActivity())
            .title(text = "Result")
            .positiveButton { }
            .negativeButton { resultDialog -> resultDialog.dismiss() }

        val debugDialog = MaterialDialog(requireActivity())
            .title(text = "Music DB Debug Dialog")
            .listItemsSingleChoice(items = items) { materialDialog: MaterialDialog, index: Int, charSequence: CharSequence ->
                when (index) {
                    0 -> {
                        val buffer = StringBuffer()
                        MusicDatabase.songsDataBase.SongDao().getAllSongs(SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT).forEach { song ->
                            buffer.append(song).append("\n\n")
                        }
                        resultDialog.message(text = buffer.toString()).show()
                    }
                    1 -> {
                        val buffer = StringBuffer()
                        MusicDatabase.songsDataBase.AlbumDao().getAllAlbums(SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT).forEach { album ->
                            buffer.append(album).append("\n\n")
                        }
                        resultDialog.message(text = buffer.toString()).show()
                    }
                    2 -> {
                        val buffer = StringBuffer()
                        MusicDatabase.songsDataBase.AlbumDao().getAllAlbumsWithSongs(SortOrder.AlbumSortOrder.ALBUM_YEAR_REVERT).forEach { albumWithSong ->
                            buffer.append(albumWithSong.album).append(":\n")
                            albumWithSong.songs.forEach {
                                buffer.append("${it.id}-${it.title}").append("\n")
                            }
                            buffer.append("\n\n")
                        }
                        resultDialog.message(text = buffer.toString()).show()
                    }
//                    _ -> {}

                    else -> {}
                }
            }
            .positiveButton(android.R.string.ok)
            .negativeButton { debugDialog -> debugDialog.dismiss() }

        return debugDialog
    }
}
