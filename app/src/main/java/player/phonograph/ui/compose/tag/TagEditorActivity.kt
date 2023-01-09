/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.util.SongDetailUtil.readSong
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle

class TagEditorActivity : ComposeToolbarActivity() {
    private lateinit var model: TagEditorModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val song = parseIntent(this, intent)
        model = TagEditorModel(song, readSong(song))
    }
    @Composable
    override fun SetUpContent() {
        TagEditorActivityContent(model, Color(primaryColor))
    }

    override val title: String get() = getString(R.string.action_tag_editor)

    companion object {
        private fun parseIntent(context: Context, intent: Intent): Song =
            SongLoader.getSong(context, intent.extras?.getLong(SONG_ID) ?: -1)

        const val SONG_ID = "SONG_ID"

        fun launch(context: Context, songId: Long) {
            context.startActivity(
                Intent(context.applicationContext, TagEditorActivity::class.java).apply {
                    putExtra(SONG_ID, songId)
                }
            )
        }
    }
}

class TagEditorModel(val song: Song, val infoModel: SongInfoModel) : ViewModel() {
    val editRequestModel = EditRequestModel()
}

@Composable
fun TagEditorActivityContent(model: TagEditorModel, titleColor: Color) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        InfoTable(
            info = model.infoModel,
            titleColor = titleColor,
            editable = true,
            editRequestModel = model.editRequestModel
        )
    }
}