/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.components.popup

import player.phonograph.R
import player.phonograph.actions.*
import player.phonograph.model.Song
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.view.View

@Composable
fun SongActionMenuPopupContent(
    song: Song,
    showPlay: Boolean,
    index: Int,
    transitionView: View?,
) {
    val context = LocalContext.current
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .width(IntrinsicSize.Max)
    ) {
        if (showPlay) Item(R.string.action_play) {
            song.actionPlay()
        }
        Item(R.string.action_play_next) {
            song.actionPlayNext()
        }
        if (index >= 0) {
            Item(R.string.action_remove_from_playing_queue) {
                actionRemoveFromQueue(index)
            }
        } else {
            Item(R.string.action_add_to_playing_queue) {
                song.actionEnqueue()
            }
        }
        Item(R.string.action_add_to_playlist) {
            listOf(song).actionAddToPlaylist(context)
        }
        Item(R.string.action_go_to_album) {

            song.actionGotoAlbum(context, transitionView)
        }
        Item(R.string.action_go_to_artist) {

            song.actionGotoArtist(context, transitionView)
        }
        Item(R.string.action_details) {
            fragmentActivity(context) { song.actionGotoDetail(it) }
        }


        var collapsed by remember { mutableStateOf(true) }

        if (collapsed) {
            Item(R.string.more_actions) { collapsed = !collapsed }
        } else {
            Item(R.string.action_share) {
                song.actionShare(context)
            }
            Item(R.string.action_tag_editor) {
                song.actionTagEditor(context)
            }
            Item(R.string.action_set_as_ringtone) {
                song.actionSetAsRingtone(context)
            }
            Item(R.string.action_add_to_black_list) {
                song.actionAddToBlacklist(context)
            }
            Item(R.string.action_delete_from_device) {
                listOf(song).actionDelete(context)
            }
        }

    }
}

@Composable
private fun Item(@StringRes res: Int, onClickAction: () -> Unit) {
    Box(
        Modifier
            .clickable { onClickAction() }
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            stringResource(res),
            style = MaterialTheme.typography.body1,
        )
    }
}