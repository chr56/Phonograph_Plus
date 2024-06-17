/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.mechanism.setting.HomeTabConfig.PageConfigUtil.toJson
import player.phonograph.model.pages.Pages
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.pages.AbsPage
import player.phonograph.ui.fragments.pages.AlbumPage
import player.phonograph.ui.fragments.pages.ArtistPage
import player.phonograph.ui.fragments.pages.EmptyPage
import player.phonograph.ui.fragments.pages.FilesPage
import player.phonograph.ui.fragments.pages.FlattenFolderPage
import player.phonograph.ui.fragments.pages.GenrePage
import player.phonograph.ui.fragments.pages.Playlist2Page
import player.phonograph.ui.fragments.pages.SongPage
import player.phonograph.util.reportError
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object HomeTabConfig {
    private val parser = Json { ignoreUnknownKeys = true; isLenient = true }

    private var cachedPageConfig: PageConfig? = null
    private var cachedRawPageConfig: String? = null

    var homeTabConfig: PageConfig
        @Synchronized get() {
            val rawString = Setting(App.instance)[Keys.homeTabConfigJsonString].data

            // Fetch Cache
            val cached: PageConfig? = cachedPageConfig
            if (rawString == cachedRawPageConfig && cached != null) {
                return cached
            }

            // Then Parse
            val config: PageConfig = parseHomeTabConfig(rawString)
            cachedPageConfig = config
            cachedRawPageConfig = rawString

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
            val str = parser.encodeToString(json)
            synchronized(this) {
                Setting(App.instance)[Keys.homeTabConfigJsonString].data = str
            }
        }

    fun parseHomeTabConfig(raw: String): PageConfig {
        val config: PageConfig? = PageConfigUtil.from(raw)
        return if (config == null) {
            resetHomeTabConfig()
            PageConfig.DEFAULT_CONFIG
        } else {
            config
        }
    }

    /**
     * add a new [page] at the end of setting
     */
    fun append(page: String) {
        val list = homeTabConfig.tabs.toMutableList()
        list.add(page)
        homeTabConfig = PageConfig.from(list)
    }

    fun resetHomeTabConfig() {
        Setting(App.instance)[Keys.homeTabConfigJsonString].data =
            Json.encodeToString(PageConfig.DEFAULT_CONFIG.toJson())
    }

    object PageConfigUtil {

        fun PageConfig.toJson(): JsonObject = JsonObject(
            mapOf(KEY to JsonArray(tabs.map { JsonPrimitive(it) }))
        )

        /**
         * Parse from raw json
         * @return null if failed
         */
        fun from(raw: String): PageConfig? {
            if (raw.isEmpty()) return null

            val config: PageConfig? = try {
                val json: JsonElement = parser.parseToJsonElement(raw)
                fromJson(json as JsonObject)
            } catch (e: Exception) {
                reportError(e, "Preference", "Fail to parse home tab config string $raw")
                null
            }

            // valid
            // TODO

            return config
        }

        /**
         * Parse from [JsonObject]
         */
        @Throws(IllegalStateException::class)
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

class PageConfig private constructor(pages: List<String>) : Iterable<String> {

    private val _tabs: MutableList<String> = pages.toMutableList()
    val tabs: List<String> get() = _tabs.toList()

    /**
     * total count of tabs
     */
    val size: Int get() = _tabs.size

    /**
     * get page identifier by [index]
     */
    operator fun get(index: Int): String = _tabs[index]

    /**
     * create instance of AbsPage by [index]
     */
    fun initiate(index: Int): AbsPage =
        when (get(index)) {
            Pages.SONG     -> SongPage()
            Pages.ALBUM    -> AlbumPage()
            Pages.ARTIST   -> ArtistPage()
            Pages.PLAYLIST -> Playlist2Page()
            Pages.GENRE    -> GenrePage()
            Pages.FILES    -> FilesPage()
            Pages.FOLDER   -> FlattenFolderPage()
            else           -> EmptyPage()
        }

    companion object {

        fun from(tabs: List<String>) = PageConfig(tabs)

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
            override fun hasNext(): Boolean = current < _tabs.size
            override fun next(): String = _tabs[current++]
        }

    override fun toString(): String = _tabs.fold("PagerConfig:") { acc, i -> "$acc, $i" }
}