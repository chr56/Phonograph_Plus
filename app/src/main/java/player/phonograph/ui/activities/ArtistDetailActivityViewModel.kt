/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import mt.pref.ThemeColor
import mt.pref.ThemeColor.accentColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import player.phonograph.settings.Setting
import player.phonograph.util.reportError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import util.phonograph.lastfm.rest.LastFMRestClient
import util.phonograph.lastfm.rest.model.LastFmArtist
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.widget.ImageView
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ArtistDetailActivityViewModel(var artistId: Long) : ViewModel() {

    private val _artist: MutableStateFlow<Artist?> = MutableStateFlow(null)
    val artist get() = _artist.asStateFlow()

    private val _albums: MutableStateFlow<List<Album>?> = MutableStateFlow(null)
    val albums get() = _albums.asStateFlow()

    private val _songs: MutableStateFlow<List<Song>?> = MutableStateFlow(null)
    val songs get() = _songs.asStateFlow()

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
    val paletteColor get() = _paletteColor.asStateFlow()



    fun load(context: Context) {
        viewModelScope.launch(SupervisorJob()) {
            val artist = ArtistLoader.id(context, artistId)
            _artist.emit(artist)
            _albums.emit(artist.albums)
            _songs.emit(artist.songs)
        }
    }

    fun loadArtistImage(context: Context, artist: Artist, imageView: ImageView) {
        val defaultColor = ThemeColor.primaryColor(context)
        loadImage(context)
            .from(artist)
            .into(
                PaletteTargetBuilder(defaultColor)
                    .onResourceReady { result, color ->
                        imageView.setImageDrawable(result)
                        _paletteColor.tryEmit(color)
                    }
                    .onFail {
                        imageView.setImageResource(R.drawable.default_album_art)
                        _paletteColor.tryEmit(defaultColor)
                    }
                    .build()
            )
            .enqueue()
    }

    private val lastFMRestClient: LastFMRestClient by lazy { LastFMRestClient(App.instance) }
    var biographyDialog: MaterialDialog? = null

    var biography: Spanned? = null
    var lastFmUrl: String? = null
    fun loadBiography(
        context: Context,
        artist: Artist,
        lang: String? = Locale.getDefault().language
    ) {
        biography = null
        lastFMRestClient.apiService
            .getArtistInfo(artist.name, lang, null)
            .enqueue(object : Callback<LastFmArtist?> {

                override fun onResponse(
                    call: Call<LastFmArtist?>,
                    response: Response<LastFmArtist?>
                ) {
                    response.body()?.let { lastFmArtist ->
                        lastFmUrl = lastFmArtist.artist.url
                        val bioContent = lastFmArtist.artist.bio?.content
                        if (bioContent != null && bioContent.trim { it <= ' ' }.isNotEmpty()) {
                            biography = Html.fromHtml(bioContent, Html.FROM_HTML_MODE_LEGACY)
                        }
                    }
                    // If the "lang" parameter is set and no biography is given, retry with default language
                    if (biography == null && lang != null) {
                        loadBiography(context, artist, null)
                        return
                    }
                    if (!Setting.instance.isAllowedToDownloadMetadata(context)) {
                        with(biographyDialog!!) {
                            if (biography != null) {
                                message(text = biography)
                            } else {
                                message(R.string.biography_unavailable)
                            }
                            negativeButton(text = "Last.FM") {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(lastFmUrl)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<LastFmArtist?>, t: Throwable) {
                    biography = null
                    reportError(t, "LoadBiography", "Load ${artist.name} Fail")
                }

            })
    }

    fun showBiography(context: Context, artist: Artist): Boolean {
        if (biographyDialog == null) {
            biographyDialog = MaterialDialog(context)
                .title(null, artist.name)
                .positiveButton(android.R.string.ok, null, null)
                .apply {
                    getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor(context))
                    getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor(context))
                }
        }
        if (Setting.instance.isAllowedToDownloadMetadata(context)) { // wiki should've been already downloaded
            biographyDialog!!.show {
                if (biography != null) {
                    message(text = biography)
                } else {
                    message(R.string.biography_unavailable)
                }
                negativeButton(text = "Last.FM") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(lastFmUrl)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            }
        } else { // force download
            biographyDialog!!.show()
            loadBiography(context, artist)
        }
        return true
    }

}
