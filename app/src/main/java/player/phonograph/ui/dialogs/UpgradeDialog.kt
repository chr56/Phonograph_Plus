/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.dialogs

import lib.phonograph.dialog.SingleChoiceItemMap
import lib.phonograph.dialog.alertDialog
import mt.color.MaterialColor
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.UpdateConfig.GITHUB_REPO
import player.phonograph.model.version.Version
import player.phonograph.model.version.VersionCatalog
import player.phonograph.settings.Setting
import player.phonograph.ui.components.viewcreater.ContentPanel
import player.phonograph.ui.components.viewcreater.buildDialogView
import player.phonograph.ui.components.viewcreater.buttonPanel
import player.phonograph.ui.components.viewcreater.contentPanel
import player.phonograph.ui.components.viewcreater.titlePanel
import player.phonograph.util.text.dateText
import player.phonograph.mechanism.canAccessGitHub
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
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

class UpgradeDialog : DialogFragment() {

    private lateinit var versionCatalog: VersionCatalog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        versionCatalog = requireArguments().getParcelable(VERSION_CATALOG)
            ?: throw IllegalStateException("VersionCatalog non-exist")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = buildDialog()

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
                setTextColor(accentColor)
                text =
                    dateText(versionCatalog.currentLatestChannelVersionBy { it.versionCode }.date)
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
                            text = toTitle(version)
                            textSize = 17f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            setPadding(8)
                        }
                        val log = TextView(context).apply {
                            textSize = 14f
                            text = version.releaseNote.parsed(resources)
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                            setPadding(8)
                        }
                        addView(title, MATCH_PARENT, WRAP_CONTENT)
                        addView(log, MATCH_PARENT, WRAP_CONTENT)
                    }, MATCH_PARENT, WRAP_CONTENT)
                    setOnClickListener {
                        actionSelectVersion(version)
                    }
                }
                addView(card,
                        FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                            .apply { setMargins(36) })
            }
        }
    }

    private fun toTitle(version: Version) = with(version) {
        val channelColor = ForegroundColorSpan(when (channel.lowercase()) {
                                                   "stable"  -> MaterialColor.Blue._A200.asColor
                                                   "preview" -> MaterialColor.DeepOrange._A200.asColor
                                                   "lts"     -> MaterialColor.Green._A200.asColor
                                                   else      -> MaterialColor.BlueGrey._700.asColor
                                               })
        SpannableStringBuilder().apply {
            append(versionName,
                   ForegroundColorSpan(accentColor),
                   SpannableStringBuilder.SPAN_EXCLUSIVE_INCLUSIVE)
            append(" ")
            append(dateText(date),
                   ForegroundColorSpan(primaryTextColor),
                   SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(" ")
            append(channel, channelColor, SpannableStringBuilder.SPAN_EXCLUSIVE_INCLUSIVE)
        }
    }

    private fun actionIgnore() {
        dismiss()
        Setting.instance.ignoreUpgradeDate =
            versionCatalog.currentLatestChannelVersionBy { it.date }.date
        Toast.makeText(activity, R.string.upgrade_ignored, Toast.LENGTH_SHORT).show()
    }

    private fun actionMore() {
        val map = mutableListOf(
            Pair("${getString(R.string.git_hub)} (Release Page)") { _: DialogInterface ->
                open(GITHUB_RELEASE_URL)
            }
        )
        if (canAccessGitHub) {
            map += Pair(getString(R.string.tg_channel)) { _: DialogInterface -> open(TG_CHANNEL) }
        }
        buildSingleChoiceAlertDialog(
            getString(R.string.download),
            map
        ).show()
    }


    private fun actionSelectVersion(version: Version) {
        val links = version.link
        buildSingleChoiceAlertDialog(
            getString(R.string.download),
            links.map { (name, uri) ->
                Pair(name) { open(uri) }
            }
        ).show()
    }

    fun open(uri: String) {
        requireContext().startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

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

    private val accentColor get() = ThemeColor.accentColor(requireContext())
    private val primaryTextColor get() = requireContext().primaryTextColor()

    companion object {
        const val GITHUB_RELEASE_URL = "https://github.com/$GITHUB_REPO/releases"
        const val TG_CHANNEL = "https://t.me/Phonograph_Plus"

        fun create(versionCatalog: VersionCatalog): UpgradeDialog = UpgradeDialog().apply {
            arguments = Bundle().also {
                it.putParcelable(VERSION_CATALOG, versionCatalog)
            }
        }

        private const val VERSION_CATALOG = "VERSION_CATALOG"
    }


    private fun buildSingleChoiceAlertDialog(
        title: String,
        map: SingleChoiceItemMap,
    ): AlertDialog {
        return alertDialog(requireActivity()) {
            title(title)
            positiveButton(android.R.string.ok) { dialog -> dialog.dismiss() }
            singleChoiceItems(map, -1, true)
        }
    }

}
