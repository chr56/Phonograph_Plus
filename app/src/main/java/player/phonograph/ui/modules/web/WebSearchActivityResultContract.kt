/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import lib.phonograph.misc.ActivityResultContractTool
import player.phonograph.ui.modules.web.WebSearchLauncher.selectable
import util.phonograph.tagsources.lastfm.LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmTrack
import util.phonograph.tagsources.musicbrainz.MusicBrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzReleaseGroup
import androidx.activity.result.contract.ActivityResultContract
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import kotlinx.serialization.json.Json

class WebSearchActivityResultContract : ActivityResultContract<Intent, Any?>() {

    override fun createIntent(context: Context, input: Intent): Intent {
        return input.selectable()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Any? {
        if (resultCode == RESULT_OK && intent != null) {
            val type = intent.getStringExtra(EXTRA_SELECTOR_RESULT)
            val data = intent.getStringExtra(EXTRA_DATA)
            if (type != null && data != null) {
                val json = Json { ignoreUnknownKeys = true }
                return when (type) {
                    MUSICBRAINZ_ARTIST        -> json.decodeFromString<MusicBrainzArtist>(data)
                    MUSICBRAINZ_RECORDING     -> json.decodeFromString<MusicBrainzRecording>(data)
                    MUSICBRAINZ_RELEASE       -> json.decodeFromString<MusicBrainzRelease>(data)
                    MUSICBRAINZ_RELEASE_GROUP -> json.decodeFromString<MusicBrainzReleaseGroup>(data)
                    LASTFM_ALBUM              -> json.decodeFromString<LastFmAlbum>(data)
                    LASTFM_ARTIST             -> json.decodeFromString<LastFmArtist>(data)
                    LASTFM_TRACK              -> json.decodeFromString<LastFmTrack>(data)
                    else                      -> null
                }
            }
        }
        return null
    }
}

class WebSearchTool : ActivityResultContractTool<Intent, Any?>() {
    override fun key(): String = "WebSearch"
    override fun contract(): ActivityResultContract<Intent, Any?> = WebSearchActivityResultContract()
}

interface IWebSearchRequester {
    val webSearchTool: WebSearchTool
}