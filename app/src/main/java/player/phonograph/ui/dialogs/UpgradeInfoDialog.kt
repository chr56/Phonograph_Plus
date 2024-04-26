/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import lib.phonograph.dialog.alertDialog
import mt.color.MaterialColor
import player.phonograph.R
import player.phonograph.UpdateConfig.CHANNEL_NAME
import player.phonograph.UpdateConfig.DOMAIN_GITHUB
import player.phonograph.UpdateConfig.DOMAIN_TG_LINK
import player.phonograph.UpdateConfig.GITHUB_REPO
import player.phonograph.mechanism.canAccessGitHub
import player.phonograph.model.version.ReleaseChannel
import player.phonograph.model.version.Version
import player.phonograph.model.version.VersionCatalog
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.TempPopupContent
import player.phonograph.util.parcelable
import player.phonograph.util.text.dateText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.widget.Toast

class UpgradeInfoDialog : ComposeViewDialogFragment() {

    private lateinit var versionCatalog: VersionCatalog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        versionCatalog = requireArguments().parcelable(VERSION_CATALOG)
            ?: throw IllegalStateException("VersionCatalog non-exist")
    }

    @Composable
    override fun Content() {
        MainContent(versionCatalog, ::dismiss)
    }

    companion object {

        private const val VERSION_CATALOG = "VERSION_CATALOG"
        fun create(versionCatalog: VersionCatalog): UpgradeInfoDialog =
            UpgradeInfoDialog().apply {
                arguments = Bundle().also {
                    it.putParcelable(VERSION_CATALOG, versionCatalog)
                }
            }

    }

}
@Composable
private fun MainContent(versionCatalog: VersionCatalog, dismiss: () -> Unit) {
    PhonographTheme {
        val context = LocalContext.current
        MaterialDialog(
            dialogState = rememberMaterialDialogState(true),
            onCloseRequest = { dismiss() },
            buttons = {
                button(
                    res = R.string.ignore_once,
                    textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary),
                ) {
                    dismiss()
                    actionIgnore(context, versionCatalog)
                }
                positiveButton(
                    res = R.string.more_actions,
                    textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary),
                    disableDismiss = true
                ) {
                    actionMore(context)
                }
            }
        ) {
            title(res = R.string.new_version)
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                for (version in versionCatalog.versions) {
                    Card(modifier = Modifier.padding(vertical = 8.dp), elevation = 2.dp) {
                        Version(version)
                    }
                }
            }
        }
    }
}

@Composable
private fun Version(version: Version) {
    var showPopup: Boolean by remember { mutableStateOf(false) }
    val dismissPopup = { showPopup = false }
    Box(Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
        VersionInfo(version) { showPopup = true }
        if (showPopup) Popup(Alignment.Center, onDismissRequest = dismissPopup) {
            VersionPopupContent(version, dismissPopup)
        }
    }
}

@Composable
private fun VersionInfo(version: Version, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        VersionTitle(version)
        VersionNote(version)
    }
}

@Composable
private fun VersionTitle(version: Version, modifier: Modifier = Modifier) {
    Row(modifier) {
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .weight(6f)
        ) {
            Text(
                version.versionName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.secondary
            )
            Text(
                version.channel,
                fontWeight = FontWeight.Bold,
                color = channelColor(version.channel)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            dateText(version.date),
            Modifier
                .weight(5f)
                .padding(horizontal = 8.dp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
    }
}

private fun channelColor(channel: String) =
    Color(
        when (channel.lowercase()) {
            ReleaseChannel.Stable.determiner -> MaterialColor.Blue._A200.asColor
            ReleaseChannel.Preview.determiner -> MaterialColor.DeepOrange._A200.asColor
            ReleaseChannel.LTS.determiner -> MaterialColor.Lime._A700.asColor
            else -> MaterialColor.BlueGrey._700.asColor
        }
    )

@Composable
private fun VersionNote(version: Version) {
    val context = LocalContext.current
    Box(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val note = remember { noteAnnotationString(version.releaseNote.parsed(context.resources)) }
        Text(note, fontSize = 14.sp)
    }
}

private fun noteAnnotationString(spanned: Spanned): AnnotatedString = buildAnnotatedString {
    for (line in spanned.toString().lines().filter { it.isNotEmpty() }) {
        append('\n')
        val items = line.split(Regex("\\s|:\\s"), limit = 2)
        if (items.size == 2) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(items[0])
                append("\t ")
            }
            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                append(items[1])
            }
        } else {
            append(line)
        }
        append('\n')
    }
}

@Composable
private fun VersionPopupContent(version: Version, dismissPopup: () -> Unit) {
    val context = LocalContext.current
    TempPopupContent(dismissPopup = dismissPopup, onClick = dismissPopup) {
        Column {
            for (link in version.link) {
                TextButton(onClick = { context.open(link.uri) }) {
                    Icon(
                        Icons.Default.Home,
                        null,
                        Modifier
                            .padding(4.dp)
                            .align(Alignment.CenterVertically)
                    )
                    Text(
                        link.name,
                        Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }
    }
}

private fun actionIgnore(context: Context, versionCatalog: VersionCatalog) {
    Setting(context)[Keys.ignoreUpgradeDate].data = versionCatalog.currentLatestChannelVersionBy { it.date }.date
    Toast.makeText(context, R.string.upgrade_ignored, Toast.LENGTH_SHORT).show()
}

private fun actionMore(context: Context) {
    val map = mutableListOf(
        Pair("${context.getString(R.string.git_hub)} (Release Page)") { _: DialogInterface ->
            context.open(GITHUB_RELEASE_URL)
        }
    )
    if (canAccessGitHub) {
        map += Pair(context.getString(R.string.tg_channel)) { _: DialogInterface -> context.open(TG_CHANNEL) }
    }
    alertDialog(context) {
        title(R.string.download)
        positiveButton(android.R.string.ok) { dialog -> dialog.dismiss() }
        singleChoiceItems(map, -1, true)
    }.show()
}


private fun Context.open(uri: String) {
    startActivity(
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(if (uri.matches(Regex("[a-zA-Z]*://.+"))) uri else "http://$uri")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}

private const val GITHUB_RELEASE_URL = "$DOMAIN_GITHUB/$GITHUB_REPO/releases"
private const val TG_CHANNEL = "$DOMAIN_TG_LINK/$CHANNEL_NAME"