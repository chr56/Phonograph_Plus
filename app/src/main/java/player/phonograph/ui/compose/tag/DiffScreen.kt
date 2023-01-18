/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.model.songTagNameRes
import player.phonograph.ui.compose.components.Title
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.net.Uri

@Composable
internal fun DiffScreen(model: TagEditorScreenViewModel) {
    val diff = remember { model.generateDiff() }
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
                    Title(stringResource(id = R.string.remove_cover))
                }
            if (diff.artworkDiff is TagDiff.ArtworkDiff.Replaced)
                item {
                    Title(stringResource(id = R.string.update_image))
                    DiffText("-> ${diff.artworkDiff.uri}")
                }
        }
}

@Composable
private fun TagDiff(tag: Triple<FieldKey, String?, String?>) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Title(stringResource(id = songTagNameRes(tag.first)), horizontalPadding = 0.dp)
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
