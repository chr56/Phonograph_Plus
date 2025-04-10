/*
 *  Copyright (c) 2022~2024 chr_56
 */

@file:Suppress("ConvertObjectToDataObject")

package player.phonograph.ui.modules.playlist

import player.phonograph.model.Song
import player.phonograph.model.ui.UIMode

sealed interface PlaylistAction

object Fetch : PlaylistAction
class Refresh(@JvmField val fetch: Boolean) : PlaylistAction
class Search(@JvmField val keyword: String) : PlaylistAction
class UpdateMode(val mode: UIMode) : PlaylistAction

sealed interface EditAction : PlaylistAction {
    class Move(@JvmField val from: Int, @JvmField val to: Int) : EditAction
    class Delete(val song: Song, @JvmField val position: Int) : EditAction
}
