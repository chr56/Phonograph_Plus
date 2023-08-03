/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import util.phonograph.lastfm.rest.LastFMRestClient
import util.phonograph.lastfm.rest.model.LastFmAlbum
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.text.Html
import android.text.Spanned
import android.text.SpannedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Locale

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

            _album = AlbumLoader.id(context, albumId)

            val songs: List<Song> = album.songs

            while (!isRecyclerViewPrepared) yield() // wait until ready
            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) {
                    callback(album, songs)
                }
            }
        }
    }

    val paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)

    private val lastFMRestClient: LastFMRestClient by lazy { LastFMRestClient(App.instance) }
    var wikiText: Spanned? = null
    var lastFMUrl: String? = null

    fun loadWiki(
        context: Context,
        lang: String? = Locale.getDefault().language,
        resultCallback: ((Spanned?, String?) -> Unit)?
    ) {
        lastFMRestClient.apiService
            .getAlbumInfo(album.title, album.artistName, lang)
            .enqueue(object : Callback<LastFmAlbum?> {
                override fun onResponse(call: Call<LastFmAlbum?>, response: Response<LastFmAlbum?>) {
                    response.body()?.let { lastFmAlbum ->
                        // clear
                        lastFMUrl = null
                        wikiText = null
                        // parse
                        lastFMUrl = lastFmAlbum.album.url
                        wikiText = lastFmAlbum.album.wiki?.content?.trim()?.let { text ->
                            if (text.isNotEmpty()) {
                                Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
                            } else {
                                // If the "lang" parameter is set and no wiki is given, retry with default language
                                if (lang != null) {
                                    loadWiki(context, null, resultCallback)
                                    return
                                } else {
                                    SpannedString(context.getString(R.string.wiki_unavailable))
                                }
                            }
                        }
                    }
                    resultCallback?.invoke(wikiText, lastFMUrl)
                }

                override fun onFailure(call: Call<LastFmAlbum?>, t: Throwable) {
                    ErrorNotification.postErrorNotification(t, "Load ${album.title} Fail")
                }
            })
    }
}
