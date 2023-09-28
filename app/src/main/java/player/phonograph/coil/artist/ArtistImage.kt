package player.phonograph.coil.artist

import player.phonograph.coil.audiofile.AudioFile

data class ArtistImage(
    val name: String,
    val id: Long,
    val files: List<AudioFile>,
) {
    override fun toString(): String =
        "ArtistImage(name=$name, id=$id, files=${files.joinToString(prefix = "[", postfix = "]") { it.path }})"
}
