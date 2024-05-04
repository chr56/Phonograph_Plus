/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.activityresultcontract.CreateFileStorageAccessTool
import lib.activityresultcontract.ICreateFileStorageAccess
import lib.activityresultcontract.IOpenFileStorageAccess
import lib.activityresultcontract.OpenFileStorageAccessTool
import mms.Source
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.compose.ComposeThemeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.modules.web.IWebSearchRequester
import player.phonograph.ui.modules.web.WebSearchLauncher
import player.phonograph.ui.modules.web.WebSearchTool
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.runBlocking

class MultiTagBrowserActivity :
        ComposeThemeActivity(),
        IWebSearchRequester,
        ICreateFileStorageAccess,
        IOpenFileStorageAccess {

    private val viewModel: MultiTagBrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        createFileStorageAccessTool.register(this)
        openFileStorageAccessTool.register(this)
        webSearchTool.register(this)
        val songs = parseIntent(this, intent)
        viewModel.updateSong(this, songs)
        super.onCreate(savedInstanceState)

        setContent {
            BatchTagEditor(viewModel, onBackPressedDispatcher, webSearchTool)
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

        private const val PATHS = "PATHS"
        private fun parseIntent(context: Context, intent: Intent): List<Song> {
            val paths = intent.extras?.getStringArrayList(PATHS) ?: return emptyList()
            return paths.mapNotNull { runBlocking { Songs.path(context, it) } }
        }

        fun launch(context: Context, paths: ArrayList<String>) {
            context.startActivity(
                Intent(context, MultiTagBrowserActivity::class.java).apply {
                    putStringArrayListExtra(PATHS, paths)
                }
            )
        }
    }
}


@Composable
private fun BatchTagEditor(
    viewModel: MultiTagBrowserViewModel,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    webSearchTool: WebSearchTool,
) {
    PhonographTheme {
        val scaffoldState = rememberScaffoldState()
        val editable by viewModel.editable.collectAsState()
        Scaffold(
            Modifier.statusBarsPadding(),
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(if (editable) R.string.action_tag_editor else R.string.label_details)
                        )
                    },
                    navigationIcon = {
                        Box(Modifier.padding(16.dp)) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack, null,
                                Modifier.clickable {
                                    onBackPressedDispatcher.onBackPressed()
                                }
                            )
                        }
                    },
                    actions = {
                        RequestWebSearch(viewModel, webSearchTool)
                        if (editable) {
                            IconButton(onClick = { viewModel.saveConfirmationDialogState.show() }) {
                                Icon(painterResource(id = R.drawable.ic_save_white_24dp), stringResource(R.string.save))
                            }
                        } else {
                            IconButton(onClick = { viewModel.updateEditable(true) }) {
                                Icon(painterResource(id = R.drawable.ic_edit_white_24dp), stringResource(R.string.edit))
                            }
                        }
                    }
                )
            }
        ) {
            Box(Modifier.padding(it)) {
                MultiTagBrowserScreen(viewModel)
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun RequestWebSearch(viewModel: MultiTagBrowserViewModel, webSearchTool: WebSearchTool) {
    val context = LocalContext.current
    fun search(source: Source) {
        val intent = when (source) {
            Source.LastFm -> WebSearchLauncher.searchLastFmSong(context, null)
            Source.MusicBrainz -> WebSearchLauncher.searchMusicBrainzSong(context, null)
        }
        webSearchTool.launch(intent) {
            // Log.v("TagEditor", it.toString()) //todo
        }
    }
    RequestWebSearch(webSearchTool, ::search, null)
}

