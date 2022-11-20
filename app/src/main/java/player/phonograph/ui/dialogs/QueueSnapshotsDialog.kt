/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.App
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.QueueSnapshotsDialog
import androidx.compose.runtime.Composable

class QueueSnapshotsDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        QueueSnapshotsDialog(requireContext(), App.instance.queueManager, ::dismiss)
    }
}