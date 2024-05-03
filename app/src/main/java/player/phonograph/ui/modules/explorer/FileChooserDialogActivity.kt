/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.ui.components.viewcreater.buttonPanel
import player.phonograph.ui.components.viewcreater.contentPanel
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.fragment.app.commit
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout

class FileChooserDialogActivity : AppCompatActivity() {


    private val model: FilesChooserViewModel by viewModels()

    private lateinit var explorerFragment: FilesChooserExplorerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        explorerFragment = FilesChooserExplorerFragment()
        setContentView(dialogView())
        setResult(RESULT_CODE_CANCELED)
    }

    private fun dialogView(): ViewGroup {
        val contentPanel = contentPanel(this) {
            id = R.id.container
            setPadding(0, 0, 0, 24 + 128)
        }

        val buttonPanel = buttonPanel(this) {
            button(0, getString(R.string.grant_permission), accentColor) {
                navigateToStorageSetting(this@FileChooserDialogActivity)
            }
            space(1)
            button(2, getString(R.string.action_select), accentColor) {
                onSelect()
            }
        }

        val rootContainer = FrameLayout(this).apply {
            addView(
                contentPanel.panel, 0, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
                )
            )
            addView(buttonPanel.panel, 1, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            ).apply { setMargins(8) })
            supportFragmentManager.commit {
                replace(R.id.container, explorerFragment, "FilesChooserExplorer")
            }
            setBackgroundColor(Color.WHITE)
        }
        return rootContainer
    }

    private fun onSelect() {
        setResult(
            RESULT_CODE_SUCCESS,
            Intent().apply {
                putExtra(EXTRA_KEY_PATH, model.currentLocation.value.absolutePath)
            }
        )
        finish()
    }

    private val accentColor by lazy { ThemeColor.accentColor(this) }

    class FileChooserActivityResultContract : ActivityResultContract<String?, String?>() {
        override fun createIntent(context: Context, input: String?): Intent =
            Intent(context, FileChooserDialogActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? =
            if (resultCode == RESULT_CODE_SUCCESS && intent != null) {
                intent.getStringExtra(EXTRA_KEY_PATH)
            } else {
                Log.w(TAG, "Not selected")
                null
            }
    }
}

private const val TAG = "FileChooser"

private const val RESULT_CODE_SUCCESS = 0
private const val RESULT_CODE_CANCELED = -1

private const val EXTRA_KEY_PATH = "path"
