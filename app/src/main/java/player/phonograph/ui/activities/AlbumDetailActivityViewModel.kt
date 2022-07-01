/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import android.text.Html
import android.text.Spanned
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.*
import kotlinx.coroutines.*
import player.phonograph.App
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import util.phonograph.lastfm.rest.LastFMRestClient
import util.phonograph.lastfm.rest.model.LastFmAlbum

class AlbumDetailActivityViewModel : ViewModel() {

    var isRecyclerViewPrepared: Boolean = false

    var albumId: Long = -1
    private var _album: Album? = null
    val album: Album get() = _album ?: Album()

    fun loadDataSet(
        context: Context,
        callback: (Album, List<Song>) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {

            _album = AlbumLoader.getAlbum(context, albumId)

            val songs: List<Song> = album.songs

            while (!isRecyclerViewPrepared) yield() // wait until ready
            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) {
                    callback(album, songs)
                }
            }
        }
    }

    val paletteColor: MutableLiveData<Int> = MutableLiveData(0)

    private val lastFMRestClient: LastFMRestClient by lazy { LastFMRestClient(App.instance) }
    var wiki: Spanned? = null

    fun loadWiki(
        context: Context,
        lang: String? = Locale.getDefault().language,
        resultCallback: ((Spanned?) -> Unit)?
    ) {
        wiki = null
        lastFMRestClient.apiService
            .getAlbumInfo(album.title, album.artistName, lang)
            .enqueue(object : Callback<LastFmAlbum?> {
                override fun onResponse(call: Call<LastFmAlbum?>, response: Response<LastFmAlbum?>) {
                    response.body()?.let { lastFmAlbum ->
                        if (lastFmAlbum.album != null && lastFmAlbum.album.wiki != null) {
                            val wikiContent = lastFmAlbum.album.wiki.content
                            if (wikiContent != null && wikiContent.trim { it <= ' ' }.isNotEmpty()) {
                                wiki = Html.fromHtml(wikiContent, Html.FROM_HTML_MODE_LEGACY)
                            }
                        }
                    }

                    // If the "lang" parameter is set and no wiki is given, retry with default language
                    if (wiki == null && lang != null) {
                        loadWiki(context, null, resultCallback)
                        return
                    }

                    if (!Setting.isAllowedToDownloadMetadata(context)) {
                        resultCallback?.invoke(wiki)
                    }
                }

                override fun onFailure(call: Call<LastFmAlbum?>, t: Throwable) {
                    ErrorNotification.postErrorNotification(t, "Load ${album.title} Fail")
                }
            })
    }
}
