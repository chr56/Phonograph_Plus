/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.ui

sealed interface PanelAction {
    object Expand : PanelAction
    object Collapse : PanelAction
    object Toggle : PanelAction
}