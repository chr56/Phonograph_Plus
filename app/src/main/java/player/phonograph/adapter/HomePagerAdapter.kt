/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import player.phonograph.ui.fragments.mainactivity.library.new_ui.EmptyPage
import player.phonograph.ui.fragments.mainactivity.library.new_ui.SongPage
import java.lang.ref.WeakReference
import kotlin.jvm.Throws

class HomePagerAdapter(fragment: Fragment, var cfg: PageConfig) : FragmentStateAdapter(fragment) {

    private val parentFragmentReference: WeakReference<Fragment> = WeakReference(fragment)

    override fun getItemCount(): Int {
        return cfg.getSize()
    }

    override fun createFragment(position: Int): Fragment {
        val fragmentClass =
            when (cfg.get(position)) {
                PAGERS.SONG -> SongPage::class.java
                else -> EmptyPage::class.java
            }
        var fragment: Fragment? = null

        try {
            fragment = fragmentClass.getConstructor().newInstance()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return fragment ?: EmptyPage()
    }
}

class PageConfig(var tabMap: MutableMap<Int, String>) {

    fun getSize(): Int = tabMap.size

    fun get(index: Int): String {
        return tabMap[index] ?: "EMPTY"
    }

    companion object {
        /**
         *  TODO Not yet implemented
         */
        val DEFAULT_CONFIG = PageConfig(
            HashMap<Int, String>(5).also {
                it[0] = PAGERS.SONG
                it[1] = PAGERS.ALBUM
                it[2] = PAGERS.ARTIST
                it[3] = PAGERS.PLAYLIST
                it[4] = PAGERS.GENRE
            }
        )
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
            cfg[i] = array.optString(i).also { if (it.isNotBlank()) throw JSONException("Empty String at index $i") }
        }
        return PageConfig(cfg)
    }

    private const val KEY = "PageCfg"
    /**
     *  TODO Not yet implemented
     */
    val DEFAULT_CONFIG = toJson(PageConfig.DEFAULT_CONFIG)
}
