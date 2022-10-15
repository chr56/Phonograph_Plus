/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.dialogs

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.model.version.Version
import player.phonograph.model.version.VersionCatalog
import player.phonograph.settings.Setting
import player.phonograph.ui.components.viewcreater.ContentPanel
import player.phonograph.ui.components.viewcreater.buildDialogView
import player.phonograph.ui.components.viewcreater.buttonPanel
import player.phonograph.ui.components.viewcreater.contentPanel
import player.phonograph.ui.components.viewcreater.titlePanel
import player.phonograph.util.TimeUtil.dateText
import player.phonograph.util.UpdateUtil.canAccessGitHub

class UpgradeDialog : DialogFragment() {

    private lateinit var versionCatalog: VersionCatalog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        versionCatalog = requireArguments().getParcelable(VERSION_CATALOG) ?: throw IllegalStateException("VersionCatalog non-exist")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = buildDialog()

    private lateinit var contentPanel: ContentPanel
    private lateinit var container: LinearLayout
    private fun buildDialog(): View {
        val context = requireContext()
        val titlePanel = titlePanel(context).apply {
            titleView.text = getString(R.string.new_version)
        }
        val buttonPanel = buttonPanel(context) {
            button(0, getString(R.string.ignore_once), accentColor) { actionIgnore() }
            space(1)
            button(2, getString(R.string.more_actions), accentColor) { actionMore() }
        }
        contentPanel = contentPanel(context) {
            container = LinearLayout(context).apply {
                orientation = VERTICAL
            }
            addView(container, MATCH_PARENT, WRAP_CONTENT)
        }
        return buildDialogView(context, titlePanel, contentPanel, buttonPanel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        container.apply {
            val head = TextView(context).apply {
                setPadding(32)
                text = dateText(versionCatalog.currentLatestChannelVersionBy { it.versionCode }.date)
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            addView(head, MATCH_PARENT, WRAP_CONTENT)
            for (version in versionCatalog.versions) {
                val card = CardView(context).apply {
                    setContentPadding(32, 24, 32, 24)
                    addView(LinearLayout(context).apply {
                        orientation = VERTICAL
                        val title = TextView(context).apply {
                            text = with(version) {
                                "$versionName ${dateText(date)} ($channel)"
                            }
                            textSize = 16f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        }
                        val log = TextView(context).apply {
                            textSize = 15f
                            text = version.releaseNote.parsed(resources)
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        }
                        addView(title, MATCH_PARENT, WRAP_CONTENT)
                        addView(log, MATCH_PARENT, WRAP_CONTENT)
                    }, MATCH_PARENT, WRAP_CONTENT)
                    setOnClickListener {
                        actionSelectVersion(version)
                    }
                }
                addView(card,
                    FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(36) })
            }
        }
    }

    private fun actionIgnore() {
        dismiss()
        Setting.instance.ignoreUpgradeDate = versionCatalog.currentLatestChannelVersionBy { it.date }.date
        Toast.makeText(activity, R.string.upgrade_ignored, Toast.LENGTH_SHORT).show()
    }

    private fun actionMore() {
        val uris = arrayListOf("https://github.com/chr56/Phonograph_Plus/releases")
        val text = arrayListOf(getString(R.string.git_hub) + "(Release Page)")
        if (canAccessGitHub) {
            uris.add("https://t.me/Phonograph_Plus")
            text.add(getString(R.string.tg_channel))
        }
        buildSingleChoiceAlertDialog(getString(R.string.download), text) { _, which ->
            if (which in uris.indices) requireContext().startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(uris[which])
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }.show()
    }


    private fun actionSelectVersion(version: Version) {
        val links = version.link
        buildSingleChoiceAlertDialog(getString(R.string.download), links.map { it.name }) { _, which ->
            requireContext().startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(links[which].uri)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }.show()
    }

    private inline fun buildSingleChoiceAlertDialog(
        title: String,
        items: List<String>,
        crossinline callback: (DialogInterface, Int) -> Unit,
    ): AlertDialog.Builder {
        return AlertDialog.Builder(requireActivity())
            .setTitle(title)
            .setSingleChoiceItems(items.toTypedArray(), -1) { dialog, which: Int ->
                dialog.dismiss()
                callback(dialog, which)
            }
            .setPositiveButton(android.R.string.ok) { dialog, _: Int -> dialog.dismiss() }
    }

    companion object {
        fun create(versionCatalog: VersionCatalog): UpgradeDialog = UpgradeDialog().apply {
            arguments = Bundle().also {
                it.putParcelable(VERSION_CATALOG, versionCatalog)
            }
        }

        private const val VERSION_CATALOG = "VERSION_CATALOG"
    }

    private val accentColor get() = ThemeColor.accentColor(requireContext())

    override fun onStart() {
        // set up size
        requireDialog().window!!.attributes =
            requireDialog().window!!.let { window ->
                window.attributes.apply {
                    width = (requireActivity().window.decorView.width * 0.90).toInt()
                }
            }
        super.onStart()
    }
}
