/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.model.text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.net.Uri



internal class TagDiff(
    /**
     * <TagFieldKey, oldValue, newValue> triple
     */
    val tagDiff: List<Triple<FieldKey, String?, String?>>,
    val artworkDiff: ArtworkDiff
) {
    sealed class ArtworkDiff {
        class Replaced(val uri: Uri?) : ArtworkDiff()
        object Deleted : ArtworkDiff()
        object None : ArtworkDiff()
    }

    fun noChange() = tagDiff.isEmpty() && artworkDiff is ArtworkDiff.None
}


@Composable
internal fun DiffScreen(diff: TagDiff) {
    if (diff.noChange())
        Text(text = stringResource(id = R.string.no_changes))
    else
        LazyColumn(Modifier.padding(8.dp)) {
            for (tag in diff.tagDiff) {
                item {
                    TagDiff(tag)
                }
            }
            if (diff.artworkDiff is TagDiff.ArtworkDiff.Deleted)
                item {
                    player.phonograph.ui.compose.components.Title(stringResource(id = R.string.remove_cover))
                }
            if (diff.artworkDiff is TagDiff.ArtworkDiff.Replaced)
                item {
                    player.phonograph.ui.compose.components.Title(stringResource(id = R.string.update_image))
                    DiffText("-> ${diff.artworkDiff.uri}")
                }
        }
}

@Composable
private fun TagDiff(tag: Triple<FieldKey, String?, String?>) {
    Column(Modifier.padding(vertical = 16.dp)) {
        player.phonograph.ui.compose.components.Title(tag.first.text(LocalContext.current.resources), horizontalPadding = 0.dp)
        DiffText(tag.second)
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        DiffText(tag.third)
    }
}

@Composable
private fun DiffText(string: String?, modifier: Modifier = Modifier) {
    if (string.isNullOrEmpty()) {
        Text(
            stringResource(id = R.string.empty),
            modifier
                .fillMaxWidth()
                .alpha(0.5f)
        )
    } else {
        Text(string, modifier.fillMaxWidth())
    }
}