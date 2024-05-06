/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import org.koin.android.ext.android.inject
import player.phonograph.R
import player.phonograph.model.songCountString
import player.phonograph.service.queue.QueueHolder
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.util.text.timeText
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context

class QueueSnapshotsDialog : ComposeViewDialogFragment() {

    private val queueManager: QueueManager by inject()

    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    positiveButton(
                        res = android.R.string.ok,
                        textStyle = accentColoredButtonStyle()
                    ) { dismiss() }
                }
            ) {
                title(res = R.string.playing_queue_history)
                customView {
                    QueueSnapshotsDialogContent(
                        requireContext(),
                        queueManager,
                        ::dismiss
                    )
                }
            }
        }
    }
}


@Composable
private fun QueueSnapshotsDialogContent(
    context: Context,
    queueManager: QueueManager,
    onDismiss: () -> Unit,
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
    Card(
        Modifier
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