/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenFileStorageAccessDelegate
import mms.Source
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.basis.ComposeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.SystemBarsPadded
import player.phonograph.ui.modules.tag.components.RequestWebSearchButton
import player.phonograph.ui.modules.tag.util.importWebSearchResult
import player.phonograph.ui.modules.web.IWebSearchRequester
import player.phonograph.ui.modules.web.LastFmDialog
import player.phonograph.ui.modules.web.WebSearchLauncher
import player.phonograph.ui.modules.web.WebSearchTool
import player.phonograph.util.debug
import player.phonograph.util.observe
import player.phonograph.util.theme.updateSystemBarsColor
import util.theme.color.darkenColor
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TagBrowserActivity :
        ComposeActivity(),
        IWebSearchRequester,
        ICreateFileStorageAccessible,
        IOpenFileStorageAccessible {

    private val viewModel: TagBrowserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        registerActivityResultLauncherDelegate(
            openFileStorageAccessDelegate,
            createFileStorageAccessDelegate,
            webSearchTool,
        )
        val song = parseIntent(this, intent)
        if (song != null) {
            viewModel.load(this, song, true)
        }
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback {
            if (viewModel.hasChanges) {
                viewModel.exitWithoutSavingDialogState.show()
            } else {
                finish()
            }
        }

        setContent {
            val highlightColorState: State<Color?> = viewModel.state.map { it?.color }.collectAsState(null)
            PhonographTheme(highlightColorState) {
                SystemBarsPadded {
                    TagBrowserActivityMainContent(viewModel, webSearchTool, onBackPressedDispatcher)
                }
            }
        }

        observe(viewModel.state) { state ->
            val color = state?.color
            if (color != null) updateSystemBarsColor(darkenColor(color.toArgb()), Color.Transparent.toArgb())
        }
    }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val webSearchTool: WebSearchTool = WebSearchTool()

    companion object {

        private const val PATH = "PATH"
        private fun parseIntent(context: Context, intent: Intent): Song? {
            val path = intent.extras?.getString(PATH) ?: return null
            return runBlocking { Songs.path(context, path) }
        }

        fun launch(context: Context, path: String) {
            context.startActivity(
                Intent(context, TagBrowserActivity::class.java).apply {
                    putExtra(PATH, path)
                }
            )
        }
    }
}

@Composable
private fun TagBrowserActivityMainContent(
    viewModel: TagBrowserActivityViewModel,
    webSearchTool: WebSearchTool,
    onBackPressedDispatcher: OnBackPressedDispatcher,
) {
    Scaffold(
        scaffoldState = rememberScaffoldState(),
        topBar = {
            TopAppBar(
                title = {
                    val editable by viewModel.editable.collectAsState()
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
                actions = { OptionMenu(viewModel, webSearchTool) }
            )
        }
    ) {
        Box(Modifier.padding(it)) {
            TagBrowserScreen(viewModel)
        }
    }
}

@Composable
private fun OptionMenu(viewModel: TagBrowserActivityViewModel, webSearchTool: WebSearchTool) {
    RequestWebSearchButton(viewModel, webSearchTool)
    val editable by viewModel.editable.collectAsState()
    if (editable) {
        IconButton(onClick = { viewModel.saveConfirmationDialogState.show() }) {
            Icon(
                painterResource(id = R.drawable.ic_save_white_24dp),
                stringResource(R.string.action_save)
            )
        }
    } else {
        IconButton(onClick = { viewModel.enterEditMode() }) {
            Icon(
                painterResource(id = R.drawable.ic_edit_white_24dp),
                stringResource(R.string.action_edit)
            )
        }
    }
}

@Composable
private fun RequestWebSearchButton(viewModel: TagBrowserActivityViewModel, webSearchTool: WebSearchTool) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    fun search(source: Source) {
        val song = state?.song ?: return
        val intent = when (source) {
            Source.LastFm -> WebSearchLauncher.searchLastFmSong(context, song)
            Source.MusicBrainz -> WebSearchLauncher.searchMusicBrainzSong(context, song)
        }
        webSearchTool.launch(intent) {
            if (it != null) {
                viewModel.viewModelScope.launch(Dispatchers.Default) {
                    debug { Log.v("TagEditor", it.toString()) }
                    importWebSearchResult(viewModel, it)
                }
            }
        }
    }

    fun onShowWikiDialog() {
        val song = state?.song ?: return
        val fragmentManager = (context as? FragmentActivity)?.supportFragmentManager
        if (fragmentManager != null) {
            LastFmDialog.from(song).show(fragmentManager, "WEB_SEARCH_DIALOG")
        }
    }
    RequestWebSearchButton(::search, ::onShowWikiDialog)
}

