package player.phonograph.coil.album

import player.phonograph.coil.audiofile.AudioFile

data class AlbumImage(
    val name: String,
    val id: Long,
    val files: List<AudioFile>,
) {

    override fun toString(): String =
        "AlbumImage(name=$name, id=$id, files=${files.joinToString(prefix = "[", postfix = "]") { it.path }})"
}