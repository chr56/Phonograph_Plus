/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.menu


sealed interface ActionEntry {

    val semantic: ActionSemantic
    val action: suspend () -> Unit

    data class ToolbarEntry(
        override val semantic: ActionSemantic,
        override val action: suspend () -> Unit,
        val visibility: Int = ALWAYS,
        val dangerous: Boolean = false,
    ) : ActionEntry {
        companion object {
            const val NEVER = 0
            const val ALWAYS = 1
            const val IF_ROOM = 8
        }
    }

    data class GroupedEntries(
        override val semantic: ActionSemantic,
        val entries: List<ActionEntry>,
        override val action: suspend () -> Unit = {},
    ) : ActionEntry
}