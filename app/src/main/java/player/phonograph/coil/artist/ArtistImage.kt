package player.phonograph.coil.artist

data class ArtistImage(
    val artistName: String,
    val artistId: Long,
    val songCovers: List<SongCover>,
) {

    override fun toString(): String =
        "ArtistImage{artistName:$artistName($artistId),albumCovers:{${songCovers.fold("") { acc, albumCover -> "$acc$albumCover," }}}}"


    data class SongCover(val id: Long, val year: Int, val filePath: String) {
        override fun toString(): String = "[$id($filePath),year:$year]"
    }
}
