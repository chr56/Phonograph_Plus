/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.ui

enum class AppShortcutType(val id: String) {
    LastAddedShortcut("$ID_PREFIX.last_added"),
    ShuffleAllShortcut("$ID_PREFIX.shuffle_all"),
    TopTracksShortcut("$ID_PREFIX.top_tracks"),
    ;

    companion object {
        fun from(id: String): AppShortcutType? {
            return when (id) {
                LastAddedShortcut.id  -> LastAddedShortcut
                ShuffleAllShortcut.id -> ShuffleAllShortcut
                TopTracksShortcut.id  -> TopTracksShortcut
                else                  -> null
            }
        }
    }
}

private const val ID_PREFIX = "player.phonograph.appshortcuts.id"