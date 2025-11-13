/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.R
import player.phonograph.ui.basis.ComposeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.compose.components.AdvancedDialogFrame
import player.phonograph.ui.compose.components.LimitedDialog
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.compose.AndroidFragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class PathSelectorDialogActivity : ComposeActivity() {

    private val model: FileExplorerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CODE_CANCELED)
        setContent {
            PhonographTheme {
                LimitedDialog(onDismiss = ::finish) {
                    BoxWithConstraints {
                        AdvancedDialogFrame(
                            modifier = Modifier.height(this.maxHeight * 0.833f),
                            title = stringResource(R.string.label_path_selector),
                            onDismissRequest = { finish() },
                            actions = listOf(
                                ActionItem(
                                    imageVector = Icons.Default.Check,
                                    textRes = R.string.action_select,
                                    onClick = ::onSelect,
                                )
                            ),
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                AndroidFragment<FilesChooserExplorerFragment>(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun onSelect() {
        val path = model.currentPath.value.path
        setResult(
            RESULT_CODE_SUCCESS,
            Intent().apply {
                putExtra(EXTRA_KEY_PATH, path)
            }
        )
        finish()
    }

    class PathSelectorActivityResultContract : ActivityResultContract<String?, String?>() {
        override fun createIntent(context: Context, input: String?): Intent =
            Intent(context, PathSelectorDialogActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? =
            if (resultCode == RESULT_CODE_SUCCESS && intent != null) {
                intent.getStringExtra(EXTRA_KEY_PATH)
            } else {
                Log.w(TAG, "Not selected")
                null
            }
    }

    companion object {
        private const val TAG = "PathSelector"

        private const val RESULT_CODE_SUCCESS = 0
        private const val RESULT_CODE_CANCELED = -1

        private const val EXTRA_KEY_PATH = "path"

    }
}
