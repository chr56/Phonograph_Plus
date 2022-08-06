package player.phonograph.coil.artist

data class ArtistImage(
    val artistName: String,
    val albumCovers: List<AlbumCover>
) {

    override fun toString(): String =
        "ArtistImage{artistName:$artistName,albumCovers:{${albumCovers.fold(""){ acc, albumCover -> "$acc$albumCover," }}}}"

    class AlbumCover(var year: Int, var filePath: String) {
        override fun toString(): String {
            return "[$filePath,$year]"
        }
    }
}
