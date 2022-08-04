/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import android.content.Context
import android.util.ArrayMap
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference
import kotlin.jvm.Throws
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import player.phonograph.R
import player.phonograph.ui.fragments.mainactivity.home.*

class HomePagerAdapter(fragment: Fragment, var cfg: PageConfig) : FragmentStateAdapter(fragment) {
    val map: MutableMap<Int, WeakReference<AbsPage>> = ArrayMap(cfg.getSize())

    override fun getItemCount(): Int = cfg.getSize()

    override fun createFragment(position: Int): Fragment =
        cfg.getAsPage(position)
            .also { fragment -> map[position] = WeakReference(fragment) } // registry
}

class PageConfig(var tabMap: MutableMap<Int, String>) : Iterable<String> {

    fun getSize(): Int = tabMap.size

    fun get(index: Int): String {
        return tabMap[index] ?: "EMPTY"
    }

    fun getAsPage(index: Int): AbsPage =
        when (get(index)) {
            PAGERS.SONG -> SongPage()
            PAGERS.ALBUM -> AlbumPage()
            PAGERS.ARTIST -> ArtistPage()
            PAGERS.PLAYLIST -> PlaylistPage()
            PAGERS.GENRE -> GenrePage()
            PAGERS.FOLDER -> FilesPage()
            else -> EmptyPage()
        }

    companion object {
        val DEFAULT_CONFIG = PageConfig(
            HashMap<Int, String>(6).also {
                it[0] = PAGERS.SONG
                it[1] = PAGERS.ALBUM
                it[2] = PAGERS.ARTIST
                it[3] = PAGERS.PLAYLIST
                it[4] = PAGERS.GENRE
                it[5] = PAGERS.FOLDER
            }
        )
    }

    override fun iterator(): Iterator<String> =
        object : Iterator<String> {
            var current = 0
            override fun hasNext(): Boolean = current < tabMap.size
            override fun next(): String = tabMap[current++] ?: PAGERS.EMPTY
        }
}

interface PAGERS {
    companion object {
        const val EMPTY = "EMPTY"
        const val SONG = "SONG"
        const val ALBUM = "ALBUM"
        const val ARTIST = "ARTIST"
        const val PLAYLIST = "PLAYLIST"
        const val GENRE = "GENRE"
        const val FOLDER = "FOLDER"

        fun getDisplayName(pager: String?, context: Context): String {
            return when (pager) {
                SONG -> context.getString(R.string.songs)
                ALBUM -> context.getString(R.string.albums)
                ARTIST -> context.getString(R.string.artists)
                PLAYLIST -> context.getString(R.string.playlists)
                GENRE -> context.getString(R.string.genres)
                FOLDER -> context.getString(R.string.folders)
                EMPTY -> context.getString(R.string.empty)
                else -> "UNKNOWN"
            }
        }
    }
}

object PageConfigUtil {

    @Throws(JSONException::class)
    fun toJson(cfg: PageConfig): JSONObject {
        val array: Array<String> = Array(cfg.getSize()) { i -> cfg.get(i) }
        return JSONObject().put(KEY, JSONArray(array))
    }

    @Throws(JSONException::class)
    fun fromJson(json: JSONObject): PageConfig {
        val array = json.optJSONArray(KEY) ?: throw JSONException("KEY(\"PageCfg\") doesn't exist")

        if (array.length() <= 0) throw JSONException("No Value")

        val cfg = HashMap<Int, String>()
        for (i in 0 until array.length()) {
            cfg[i] = array.optString(i).also {
                if (it.isBlank()) throw JSONException(
                    "Empty String at index $i"
                )
            }
        }
        return PageConfig(cfg)
    }

    private const val KEY = "PageCfg"

    val DEFAULT_CONFIG = toJson(PageConfig.DEFAULT_CONFIG)
}
