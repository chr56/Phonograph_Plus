/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose.dialogs

import player.phonograph.R
import player.phonograph.model.songCountString
import player.phonograph.service.queue.QueueHolder
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.TimeUtil.timeText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context

@Composable
fun QueueSnapshotsDialogContent(
    context: Context,
    queueManager: QueueManager,
    onDismiss: () -> Unit
) {
    val snapShots = remember {
        queueManager.getQueueSnapShots()
    }
    val onRecoverySnapshot = { snapshot: QueueHolder ->
        queueManager.recoverSnapshot(snapshot, createSnapshot = true, async = true)
        onDismiss()
    }
    Column {
        if (snapShots.isNotEmpty()) {
            LazyColumn(Modifier.padding(16.dp)) {
                for (snapShot in snapShots) {
                    item {
                        Snapshot(context, snapShot) { onRecoverySnapshot(snapShot) }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Text(text = context.getString(R.string.empty), Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Snapshot(context: Context, queueHolder: QueueHolder, onClick: () -> Unit) {
    Card(Modifier
             .clickable { onClick() }
             .fillMaxWidth()
             .padding(4.dp)) {
        Column(
            Modifier.padding(12.dp)
        ) {
            val songCount = queueHolder.playingQueue.size
            Text(text = timeText(queueHolder.snapshotTime / 1000), fontSize = 12.sp)
            Row(Modifier) {
                Text(text = songCountString(context, songCount))
                Text(text = " (${queueHolder.currentSongPosition + 1})")
                Spacer(modifier = Modifier.widthIn(16.dp))
                when (queueHolder.repeatMode) {
                    RepeatMode.REPEAT_QUEUE ->
                        Icon(
                            painter = painterResource(id = R.drawable.ic_repeat_white_24dp),
                            contentDescription = null
                        )
                    RepeatMode.REPEAT_SINGLE_SONG ->
                        Icon(
                            painter = painterResource(id = R.drawable.ic_repeat_one_white_24dp),
                            contentDescription = null
                        )
                    else -> {}
                }
                when (queueHolder.shuffleMode) {
                    ShuffleMode.SHUFFLE -> {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shuffle_white_24dp),
                            contentDescription = context.getString(R.string.pref_title_remember_shuffle)
                        )
                    }
                    else                -> {}
                }
            }
        }
    }
}