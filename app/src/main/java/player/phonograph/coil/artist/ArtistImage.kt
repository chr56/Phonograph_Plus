package player.phonograph.coil.artist

data class ArtistImage(
    val artistName: String,
    val albumCovers: List<AlbumCover>
) {

    override fun toString(): String =
        "ArtistImage{artistName:$artistName,albumCovers:{${albumCovers.fold(""){ acc, albumCover -> "$acc$albumCover," }}}}"

    data class AlbumCover(val id: Long, val year: Int, val filePath: String) {
        override fun toString(): String = "[$id($filePath),year:$year]"
    }
}
