package player.phonograph.ui.dialogs

import lib.phonograph.dialog.alertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.MusicServiceMsgConst
import player.phonograph.R
import player.phonograph.provider.PathFilterStore
import player.phonograph.settings.Setting
import player.phonograph.ui.components.viewcreater.*
import java.io.File

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PathFilterDialog : DialogFragment() {

    private val mode get() = Setting.instance.pathFilterExcludeMode

    private lateinit var titlePanel: TitlePanel
    private lateinit var contentPanel: ContentPanel
    private lateinit var buttonPanel: ButtonPanel

    private lateinit var controlPanel: ButtonPanel

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PathAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return buildDialog()
    }

    private fun buildDialog(): View {
        val context = requireContext()
        val accentColor = ThemeColor.accentColor(context)

        titlePanel = titlePanel(context)

        contentPanel = contentPanel(context, this::setUpContent)

        buttonPanel = buttonPanel(context) {
            button(0, getString(R.string.swith_mode), accentColor) {
                val inv = !Setting.instance.pathFilterExcludeMode
                Setting.instance.pathFilterExcludeMode = inv
                loadPaths()
                App.instance.sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))
            }
            space(1)
            button(2, getString(android.R.string.ok), accentColor) {
                dismiss()
            }
        }
        return buildDialogView(context, titlePanel, contentPanel, buttonPanel)
    }

    private fun setUpContent(root: FrameLayout) {
        val context = requireContext()
        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
        controlPanel = buttonPanel(context) {
            button(0, getString(R.string.add_action), context.primaryTextColor()) {
                PathFilterFolderChooserDialog().show(parentFragmentManager, "FOLDER_CHOOSER")
                dismiss()
            }
            space(1)
            button(2, getString(R.string.clear_action), context.primaryTextColor()) {
                clearAll(mode)
            }
        }
        root.addView(controlPanel.panel,
                     LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP)
        )
        root.addView(recyclerView,
                     LayoutParams(LayoutParams.WRAP_CONTENT,
                                  LayoutParams.MATCH_PARENT,
                                  Gravity.BOTTOM).apply { setMargins(0, 128, 0, 0) }
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadPaths()
    }


    private fun loadPaths(): List<String> {
        titlePanel.titleView.text = title(mode)
        val paths = with(PathFilterStore.getInstance(requireContext())) {
            if (mode) blacklistPaths else whitelistPaths
        }
        adapter = PathAdapter(requireContext(), paths) { index: Int, _ -> deletePath(index, mode) }
        recyclerView.adapter = adapter
        return paths
    }

    private fun deletePath(index: Int, mode: Boolean) {
        val context = requireContext()
        val path = with(PathFilterStore.getInstance(context)) {
            if (mode) blacklistPaths else whitelistPaths
        }[index]
        alertDialog(context) {
            title(R.string.delete_action)
            message("${title(mode)}\n${path}")
            positiveButton(android.R.string.ok) {
                with(PathFilterStore.getInstance(context)) {
                    if (mode) removeBlacklistPath(File(path)) else removeWhitelistPath(File(path))
                }
                loadPaths()
            }
            neutralButton(android.R.string.cancel) {
                it.dismiss()
            }
        }.show()
    }

    private fun clearAll(mode: Boolean) {
        alertDialog(requireContext()) {
            title(R.string.clear_action)
            message(
                if (mode) R.string.excluded_paths else R.string.included_paths
            )
            positiveButton(R.string.clear_action) {
                with(PathFilterStore.getInstance(requireContext())) {
                    if (mode) clearBlacklist() else clearWhitelist()
                }
                loadPaths()
            }
            neutralButton(android.R.string.cancel)
        }.show()
    }

    class PathAdapter(
        val context: Context,
        val paths: List<String>,
        val onClick: (Int, View) -> Unit,
    ) :
            RecyclerView.Adapter<PathAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(initView(context))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.display(paths[position]) { onClick(position, it) }
        }

        override fun getItemCount(): Int = paths.size

        class ViewHolder(val binding: Binding) : RecyclerView.ViewHolder(binding.root) {
            fun display(text: String, onclick: (View) -> Unit) {
                binding.textView.text = text
                binding.root.setOnClickListener(onclick)
            }
        }

        private fun initView(context: Context): Binding {
            val textView = TextView(context).apply {
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(24, 6, 24, 6)
            }
            val root = FrameLayout(context).apply {
                val layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        .apply { setMargins(24, 32, 24, 32) }
                addView(textView, layoutParams)
            }
            return Binding(root, textView)
        }

        class Binding(val root: View, val textView: TextView)
    }

    fun title(mode: Boolean) =
        if (mode) getString(R.string.excluded_paths) else getString(R.string.included_paths)

    override fun onStart() {
        requireDialog().window!!.attributes =
            requireDialog().window!!.let { window ->
                window.attributes.apply {
                    width = (requireActivity().window.decorView.width * 0.90).toInt()
                }
            }
        super.onStart()
    }
}
