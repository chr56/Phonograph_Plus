package player.phonograph.model.lyrics

const val LRC: Int = 2
const val TXT: Int = 1
const val DEFAULT_TITLE = "Lyrics"


internal fun Array<*>?.castToStringMutableList(): MutableList<String> {
    return this?.let { it.toMutableList() as MutableList<String> } ?: ArrayList<String>() as MutableList<String>
}
