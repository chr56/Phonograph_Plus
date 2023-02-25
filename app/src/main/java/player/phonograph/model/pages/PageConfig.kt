/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.pages

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import player.phonograph.ui.fragments.pages.*
import kotlin.jvm.Throws

class PageConfig private constructor(private val tabs: MutableList<String>) : Iterable<String> {

    val tabList get() = tabs.toList()

    fun getSize(): Int = tabs.size
    fun get(index: Int): String = tabs[index]

    fun getAsPage(index: Int): AbsPage =
        when (get(index)) {
            Pages.SONG     -> SongPage()
            Pages.ALBUM    -> AlbumPage()
            Pages.ARTIST   -> ArtistPage()
            Pages.PLAYLIST -> PlaylistPage()
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
}

object PageConfigUtil {

    @Throws(JSONException::class)
    fun PageConfig.toJson(): JSONObject {
        val array: Array<String> = Array(this.getSize()) { i -> this.get(i) }
        return JSONObject().put(KEY, JSONArray(array))
    }

    @Throws(JSONException::class)
    fun JSONObject.fromJson(): PageConfig {
        val array = this.optJSONArray(KEY) ?: throw JSONException("KEY(\"PageCfg\") doesn't exist")

        if (array.length() <= 0) throw JSONException("No Value")

        val cfg = ArrayList<String>()
        for (i in 0 until array.length()) {
            cfg.add(array.optString(i).also {
                if (it.isBlank()) throw JSONException(
                    "Empty String at index $i"
                )
            })
        }
        return PageConfig.from(cfg)
    }

    private const val KEY = "PageCfg"

    val DEFAULT_CONFIG = PageConfig.DEFAULT_CONFIG.toJson()
}