/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import player.phonograph.model.metadata.EditAction

/**
 *  pair of [EditAction] and [String] oldValue
 */
typealias Change = Pair<EditAction, String?>

class MetadataChanges(val changes: List<Change>) : List<Change> by changes