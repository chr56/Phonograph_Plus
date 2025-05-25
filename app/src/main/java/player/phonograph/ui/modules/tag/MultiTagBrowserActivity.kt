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
        ComposeActivity(),
        IWebSearchRequester,
        ICreateFileStorageAccessible,
        IOpenFileStorageAccessible {

    private val viewModel: MultiTagBrowserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        registerActivityResultLauncherDelegate(
            openFileStorageAccessDelegate,
            createFileStorageAccessDelegate,
        )
        webSearchTool.register(this)
        val songs = parseIntent(this, intent)
        viewModel.load(this, songs, true)
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback {
            if (viewModel.hasChanges) {
                viewModel.exitWithoutSavingDialogState.show()
            } else {
                finish()
            }
        }
        setContent {
            BatchTagEditor(viewModel, onBackPressedDispatcher, webSearchTool)
        }
    }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()

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
    viewModel: MultiTagBrowserActivityViewModel,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    webSearchTool: WebSearchTool,
) {
    PhonographTheme {
        SystemBarsPadded {
            val scaffoldState = rememberScaffoldState()
            val editable by viewModel.editable.collectAsState()
            Scaffold(
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
                            RequestWebSearchButton(viewModel, webSearchTool)
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
                    )
                }
            ) {
                Box(Modifier.padding(it)) {
                    MultiTagBrowserScreen(viewModel)
                }
            }
        }

    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun RequestWebSearchButton(viewModel: MultiTagBrowserActivityViewModel, webSearchTool: WebSearchTool) {
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
    RequestWebSearchButton(::search, null)
}

