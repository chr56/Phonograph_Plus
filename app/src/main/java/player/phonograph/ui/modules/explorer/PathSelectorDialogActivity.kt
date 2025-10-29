/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.R
import player.phonograph.settings.ThemeSetting
import player.phonograph.ui.basis.DialogActivity
import player.phonograph.util.permissions.navigateToStorageSetting
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.ButtonBarLayout
import androidx.core.util.valueIterator
import androidx.core.view.setMargins
import androidx.fragment.app.commit
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.Space

class PathSelectorDialogActivity : DialogActivity() {

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
            button(0, getString(R.string.action_grant_permission), accentColor) {
                navigateToStorageSetting(this@PathSelectorDialogActivity)
            }
            space(1)
            button(2, getString(R.string.action_select), accentColor) {
                onSelect()
            }
        }

        val rootContainer = FrameLayout(this).apply {
            addView(
                contentPanel.panel, 0, FrameLayout.LayoutParams(
                    MATCH_PARENT, WRAP_CONTENT, Gravity.TOP
                )
            )
            addView(buttonPanel.panel, 1, FrameLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM
            ).apply { setMargins(8) })
            supportFragmentManager.commit {
                replace(R.id.container, explorerFragment, "FilesChooserExplorer")
            }
        }
        return rootContainer
    }

    private fun onSelect() {
        setResult(
            RESULT_CODE_SUCCESS,
            Intent().apply {
                putExtra(EXTRA_KEY_PATH, model.currentPath.value.path)
            }
        )
        finish()
    }

    private val accentColor by lazy { ThemeSetting.accentColor(this) }

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

    fun contentPanel(context: Context, constructor: FrameLayout.() -> Unit): ContentPanel {
        val contentPanel = FrameLayout(context).apply(constructor)
        return ContentPanel(contentPanel)
    }

    class ContentPanel(val panel: FrameLayout)

    private fun buttonPanel(context: Context, constructor: ButtonPanel.Builder.() -> Unit): ButtonPanel =
        ButtonPanel.Builder(context).apply(constructor).build()

    @SuppressLint("RestrictedApi")
    class ButtonPanel(val panel: ButtonBarLayout, val buttons: SparseArray<View> = SparseArray(3)) {
        class Builder(private val context: Context) {

            private val buttons: SparseArray<View> = SparseArray(2)
            var orientation: Int = LinearLayout.HORIZONTAL

            fun button(index: Int, buttonText: String, color: Int, onClick: (View) -> Unit): Builder {
                buttons.put(index, createButton(context, buttonText, color, onClick))
                return this
            }

            fun space(index: Int): Builder {
                buttons.put(index, Space(context))
                return this
            }

            fun build(): ButtonPanel {
                val panel = ButtonBarLayout(context, null)
                val buttonLayoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 0f)
                val spaceLayoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1f)

                panel.orientation = this.orientation

                //todo
                for (button in buttons.valueIterator()) {
                    panel.addView(
                        button,
                        if (button is Space) spaceLayoutParams else buttonLayoutParams
                    )
                }

                return ButtonPanel(panel, buttons)
            }

            private fun createButton(
                context: Context,
                buttonText: String,
                color: Int,
                onClick: (View) -> Unit,
            ): Button {
                return AppCompatButton(context).apply {
                    text = buttonText
                    textSize = 14f
                    isSingleLine = true
                    gravity = Gravity.CENTER
                    setPadding(12, 0, 12, 0)
                    minWidth = 64
                    background = ColorDrawable(Color.TRANSPARENT)
                    setTextColor(color)
                    setOnClickListener { onClick(it) }
                }
            }
        }
    }
}

private const val TAG = "PathSelector"

private const val RESULT_CODE_SUCCESS = 0
private const val RESULT_CODE_CANCELED = -1

private const val EXTRA_KEY_PATH = "path"
