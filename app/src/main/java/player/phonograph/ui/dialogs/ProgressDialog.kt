/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.ui.compose.components.Progress
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow

class ProgressDialog : DialogFragment() {

    private lateinit var title: String
    lateinit var currentTextState: MutableStateFlow<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString(KEY_TITLE) ?: getString(R.string.process)
        currentTextState = MutableStateFlow(getString(R.string.process))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(setUpView(requireContext()))
            .setCancelable(false)
            .create()
        return dialog
    }

    private fun setUpView(context: Context) =
        ComposeView(context).apply {
            setContent {
                PhonographTheme {
                    Progress(currentTextState)
                }
            }
        }

    companion object {
        private const val KEY_TITLE = "title"
        fun newInstance(title: String) = ProgressDialog().apply {
            arguments = Bundle().apply {
                putString(KEY_TITLE, title)
            }
        }
    }
}