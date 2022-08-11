/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import android.util.ArrayMap
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference
import kotlin.jvm.Throws
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import player.phonograph.model.pages.Pages
import player.phonograph.ui.fragments.home.*

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
            Pages.SONG -> SongPage()
            Pages.ALBUM -> AlbumPage()
            Pages.ARTIST -> ArtistPage()
            Pages.PLAYLIST -> PlaylistPage()
            Pages.GENRE -> GenrePage()
            Pages.FOLDER -> FilesPage()
            else -> EmptyPage()
        }

    companion object {
        val DEFAULT_CONFIG = PageConfig(
            HashMap<Int, String>(6).also {
                it[0] = Pages.SONG
                it[1] = Pages.ALBUM
                it[2] = Pages.ARTIST
                it[3] = Pages.PLAYLIST
                it[4] = Pages.GENRE
                it[5] = Pages.FOLDER
            }
        )
    }

    override fun iterator(): Iterator<String> =
        object : Iterator<String> {
            var current = 0
            override fun hasNext(): Boolean = current < tabMap.size
            override fun next(): String = tabMap[current++] ?: Pages.EMPTY
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

    val DEFAULT_CONFIG = PageConfig.DEFAULT_CONFIG.toJson()
}
