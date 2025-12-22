/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.style

import com.sothree.slidinguppanel.SlidingUpPanelLayout
import player.phonograph.R
import android.content.res.Resources
import android.view.View
import kotlin.math.max

class QueuePanelHeightAdjuster(resources: Resources) {

    private val minQueuePanelHeight = resources.getDimensionPixelSize(R.dimen.player_queue_panel_height_min)

    private var targetQueuePanelHeight = 0
    private var targetAlbumCoverHeight = 0
    private var requireShrinkAlbumCoverHeight = false

    private var measured = false

    private fun measure(
        basicPlayer: View,
        queuePanel: SlidingUpPanelLayout,
        albumCoverContainer: View,
    ): Boolean {
        val availablePanelHeight = queuePanel.height - basicPlayer.height

        targetQueuePanelHeight = max(availablePanelHeight, minQueuePanelHeight)

        if (measured && queuePanel.panelHeight == targetQueuePanelHeight) return false // no changes, skipped and quickly exit

        requireShrinkAlbumCoverHeight = availablePanelHeight < minQueuePanelHeight

        targetAlbumCoverHeight =
            if (requireShrinkAlbumCoverHeight) {
                albumCoverContainer.height - (minQueuePanelHeight - availablePanelHeight) // shrink AlbumCover
            } else {
                albumCoverContainer.height
            }

        measured = true
        return queuePanel.panelHeight != targetQueuePanelHeight
    }

    fun adjust(
        basicPlayer: View,
        queuePanel: SlidingUpPanelLayout,
        albumCoverContainer: View,
    ) {
        if (measure(basicPlayer, queuePanel, albumCoverContainer)) {
            queuePanel.panelHeight = targetQueuePanelHeight
            if (requireShrinkAlbumCoverHeight && targetAlbumCoverHeight > 0) {
                albumCoverContainer.layoutParams.height = targetAlbumCoverHeight
            }
        }
    }

}