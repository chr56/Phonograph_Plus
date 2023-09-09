/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.base.Navigator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context

class WebSearchViewModel : ViewModel() {

    val navigator = Navigator<Page>(PageHome)

    private var clientDelegateLastFm: LastFmClientDelegate? = null
    fun clientDelegateLastFm(context: Context): LastFmClientDelegate {
        return if (clientDelegateLastFm != null) {
            clientDelegateLastFm!!
        } else {
            LastFmClientDelegate(context, viewModelScope).also { clientDelegateLastFm = it }
        }
    }

    private var clientDelegateMusicBrainz: MusicBrainzClientDelegate? = null
    fun clientDelegateMusicBrainz(context: Context): MusicBrainzClientDelegate {
        return if (clientDelegateMusicBrainz != null) {
            clientDelegateMusicBrainz!!
        } else {
            MusicBrainzClientDelegate(context, viewModelScope).also { clientDelegateMusicBrainz = it }
        }
    }

}
