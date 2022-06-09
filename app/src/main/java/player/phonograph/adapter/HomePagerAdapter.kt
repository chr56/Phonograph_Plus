/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import player.phonograph.R
import player.phonograph.ui.fragments.mainactivity.home.*
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
                PAGERS.ALBUM -> AlbumPage::class.java
                PAGERS.ARTIST -> ArtistPage::class.java
                PAGERS.PLAYLIST -> PlaylistPage::class.java
                PAGERS.GENRE -> GenrePage::class.java
                PAGERS.FOLDER -> FilesPage::class.java
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
            cfg[i] = array.optString(i).also { if (it.isBlank()) throw JSONException("Empty String at index $i") }
        }
        return PageConfig(cfg)
    }

    private const val KEY = "PageCfg"
    /**
     *  TODO Not yet implemented
     */
    val DEFAULT_CONFIG = toJson(PageConfig.DEFAULT_CONFIG)
}
