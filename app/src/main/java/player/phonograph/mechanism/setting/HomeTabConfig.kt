/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.mechanism.setting.HomeTabConfig.PageConfigUtil.fromJson
import player.phonograph.mechanism.setting.HomeTabConfig.PageConfigUtil.toJson
import player.phonograph.model.pages.Pages
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.pages.AbsPage
import player.phonograph.ui.fragments.pages.AlbumPage
import player.phonograph.ui.fragments.pages.ArtistPage
import player.phonograph.ui.fragments.pages.EmptyPage
import player.phonograph.ui.fragments.pages.FilesPage
import player.phonograph.ui.fragments.pages.FlattenFolderPage
import player.phonograph.ui.fragments.pages.GenrePage
import player.phonograph.ui.fragments.pages.PlaylistPage
import player.phonograph.ui.fragments.pages.SongPage
import player.phonograph.util.reportError
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object HomeTabConfig {
    private val parser = Json { ignoreUnknownKeys = true;isLenient = true }

    private var cachedPageConfig: PageConfig? = null
    private var cachedRawPageConfig: String? = null

    var homeTabConfig: PageConfig
        @Synchronized get() {
            val rawString = Setting.instance.homeTabConfigJsonString

            if (rawString.isEmpty()) {
                resetHomeTabConfig()
                return PageConfig.DEFAULT_CONFIG
            }


            val cached = cachedPageConfig
            if (rawString == cachedRawPageConfig && cached != null) {
                return cached // return the cached instead
            }


            val config: PageConfig = try {
                val json = parser.parseToJsonElement(rawString)
                fromJson(json as JsonObject)
            } catch (e: Exception) {
                reportError(e, "Preference", "Fail to parse home tab config string $rawString")
                // return default
                resetHomeTabConfig()
                PageConfig.DEFAULT_CONFIG
            }


            // update cache
            cachedPageConfig = config
            cachedRawPageConfig = rawString


            // valid // TODO
            return config
        }
        set(value) {
            val json =
                try {
                    value.toJson()
                } catch (e: Exception) {
                    Log.e("Preference", "Save home tab config failed, use default. \n${e.message}")
                    // return default
                    PageConfig.DEFAULT_CONFIG.toJson()
                }
            val str =
                parser.encodeToString(json)
            synchronized(this) {
                Setting.instance.homeTabConfigJsonString = str
            }
        }

    /**
     * add a new [page] at the end of setting
     */
    fun append(page: String) {
        val list = homeTabConfig.tabList.toMutableList()
        list.add(page)
        homeTabConfig = PageConfig.from(list)
    }

    fun resetHomeTabConfig() {
        Setting.instance.homeTabConfigJsonString =
            Json.encodeToString(PageConfig.DEFAULT_CONFIG.toJson())
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
}

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

    override fun toString(): String = tabs.fold("PagerConfig:") { acc, i -> "$acc,$i" }
}