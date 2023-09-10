/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenFileStorageAccessTool
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.ui.compose.base.ComposeThemeActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.ui.compose.web.IWebSearchRequester
import player.phonograph.ui.compose.web.WebSearchTool
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.flow.combine

class TagEditorActivity :
        ComposeThemeActivity(),
        IWebSearchRequester,
        ICreateFileStorageAccess,
        IOpenFileStorageAccess {

    private val viewModel: TagEditorActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        webSearchTool.register(lifecycle, activityResultRegistry)
        val song = parseIntent(this, intent)
        viewModel.updateSong(this, song)
        super.onCreate(savedInstanceState)

        val initialColor = primaryColor.value
        val color = combine(viewModel.color, primaryColor) { songColor, themeColor ->
            songColor ?: themeColor
        }
        setContent {
            val highlightColor by color.collectAsState(initialColor)
            TagEditor(viewModel, highlightColor, onBackPressedDispatcher)
        }
        onBackPressedDispatcher.addCallback {
            if (viewModel.pendingEditRequests.isNotEmpty()) {
                viewModel.exitWithoutSavingDialogState.show()
            } else {
                finish()
            }
        }
    }

    override val openFileStorageAccessTool: OpenFileStorageAccessTool = OpenFileStorageAccessTool()
    override val createFileStorageAccessTool: CreateFileStorageAccessTool = CreateFileStorageAccessTool()
    override val webSearchTool: WebSearchTool = WebSearchTool()

    companion object {

        private const val PATH = "PATH"
        private fun parseIntent(context: Context, intent: Intent): Song =
            SongLoader.path(context, intent.extras?.getString(PATH).orEmpty())

        fun launch(context: Context, path: String) {
            context.startActivity(
                Intent(context, TagEditorActivity::class.java).apply {
                    putExtra(PATH, path)
                }
            )
        }
    }
}

@Composable
private fun TagEditor(
    viewModel: TagEditorActivityViewModel,
    highlightColor: Color,
    onBackPressedDispatcher: OnBackPressedDispatcher,
) {
    PhonographTheme(highlightColor) {
        val scaffoldState = rememberScaffoldState()
        Scaffold(
            Modifier.statusBarsPadding(),
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.action_tag_editor)) },
                    navigationIcon = {
                        Box(Modifier.padding(16.dp)) {
                            Icon(
                                Icons.Default.ArrowBack, null,
                                Modifier.clickable {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.saveConfirmationDialogState.show() }) {
                            Icon(painterResource(id = R.drawable.ic_save_white_24dp), null)
                        }
                    }
                )
            }
        ) {
            Box(Modifier.padding(it)) {
                TagBrowserScreen(viewModel)
            }
        }
    }
}