/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.pages

import player.phonograph.ui.fragments.pages.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PageConfig private constructor(private val tabs: MutableList<String>) : Iterable<String> {

    val tabList get() = tabs.toList()

    fun getSize(): Int = tabs.size
    fun get(index: Int): String = tabs[index]

    fun getAsPage(index: Int): AbsPage =
        when (get(index)) {
            Pages.SONG     -> SongPage()
            Pages.ALBUM    -> AlbumPage()
            Pages.ARTIST   -> ArtistPage()
            Pages.PLAYLIST -> NeoPlaylistPage()
            Pages.GENRE    -> GenrePage()
            Pages.FILES    -> FilesPage()
            Pages.FOLDER   -> FlattenFolderPage()
            else           -> EmptyPage()
        }

    companion object {

        fun from(init: List<String>) = PageConfig(init.toMutableList())

        val DEFAULT_CONFIG = PageConfig(
            mutableListOf(
                Pages.SONG,
                Pages.FOLDER,
                Pages.FILES,
                Pages.PLAYLIST,
                Pages.ALBUM,
                Pages.ARTIST,
                Pages.GENRE,
            )
        )
    }

    override fun iterator(): Iterator<String> =
        object : Iterator<String> {
            var current = 0
            override fun hasNext(): Boolean = current < tabs.size
            override fun next(): String = tabs[current++]
        }

    override fun toString(): String = tabs.fold("PagerConfig:") { acc, i -> "$acc,$i" }
}

object PageConfigUtil {

    fun PageConfig.toJson(): JsonObject = JsonObject(
        mapOf(
            KEY to JsonArray(tabList.map { JsonPrimitive(it) })
        )
    )


    fun fromJson(json: JsonObject): PageConfig {
        val array = (json[KEY] as? JsonArray)
            ?: throw IllegalStateException("KEY(\"PageCfg\") doesn't exist")

        if (array.size <= 0) throw IllegalStateException("No Value")

        val data = array.mapNotNull { (it as? JsonPrimitive)?.content }.filter { it.isNotBlank() }

        return PageConfig.from(data)
    }

    private const val KEY = "PageCfg"

}