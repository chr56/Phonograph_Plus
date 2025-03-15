/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.tag.util

import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.model.Song
import player.phonograph.ui.compose.components.CascadeVerticalItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ErrorMessage(errors: Map<Song, List<Throwable>>) {
    Spacer(modifier = Modifier.height(16.dp))
    ErrorMessageAlertBox {
        Column(Modifier.padding(horizontal = 24.dp)) {
            for ((song, list) in errors) {
                SongItem(song)
                for (error in list) {
                    ErrorMessageItem(error)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(errors) {
        val messages = errors.mapValues {
            details(it.value)
        }.entries.joinToString(prefix = "Multiple errors occur: \n") { (song, errors) ->
            "> Song ${song.title}(${song.data}): $errors"
        }
        warning(context, TAG, messages)
    }
}

@Composable
fun ErrorMessage(errors: List<Throwable>) {

    Spacer(modifier = Modifier.height(16.dp))
    ErrorMessageAlertBox {
        Column(Modifier.padding(horizontal = 24.dp)) {
            for (error in errors) {
                ErrorMessageItem(error)
            }
        }
    }

    val context = LocalContext.current
    LaunchedEffect(errors.size) {
        if (errors.size > 1) {
            warning(context, TAG, details(errors))
        } else if (errors.size == 1) {
            warning(context, TAG, detail(errors.first()))
        }
    }
}

@Composable
private fun ErrorMessageAlertBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    CascadeVerticalItem(
        stringResource(R.string.title_internal_error), modifier,
        textColor = errorColor, collapsed = true,
    ) {
        Spacer(Modifier.height(4.dp))
        content()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SongItem(song: Song) {
    SelectionContainer {
        Text(song.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
    SelectionContainer {
        Text(song.data, fontSize = 12.sp, fontWeight = FontWeight.ExtraLight)
    }
}


@Composable
private fun ErrorMessageItem(throwable: Throwable) {
    val message = remember { summary(throwable) }
    val stacktrace = remember { throwable.stackTraceToString() }
    SelectionContainer {
        Text(message, fontSize = 14.sp)
    }
    SelectionContainer {
        Text(
            stacktrace,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 0.3.sp,
            lineHeight = 12.sp
        )
    }
    Spacer(Modifier.height(4.dp))
}

private val errorColor = Color(222, 56, 56)

private const val TAG = "TagBrowser"

private fun summary(throwable: Throwable): String {
    fun description(throwable: Throwable): String {
        val clazz = throwable.javaClass.name
        val message = throwable.message
        return if (message != null) {
            "$clazz: $message"
        } else {
            clazz
        }
    }

    val causeThrowable: Throwable? = throwable.cause
    return if (causeThrowable != null) {
        "${description(throwable)} \n${description(causeThrowable)}"
    } else {
        description(throwable)
    }
}

private fun detail(throwable: Throwable): String =
    "${summary(throwable)}\n${throwable.stackTraceToString()}"

private fun details(exceptions: List<Throwable>): String =
    buildString {
        append("${exceptions.size} exceptions: \n")
        for (throwable in exceptions) {
            append('-')
            append(' ')
            append(detail(throwable))
            append('\n')
        }
    }