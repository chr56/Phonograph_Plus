/*
 * Copyright (c) 2021-2023 alorma
 */


package lib.phonograph.preference.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun RowScope.SettingsTileTexts(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        SettingsTileTitle(title)
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(2.dp))
            SettingsTileSubtitle(subtitle)
        }
    }
}


@Composable
private fun SettingsTileTitle(title: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
        title()
    }
}

@Composable
private fun SettingsTileSubtitle(subtitle: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.caption) {
        CompositionLocalProvider(
            LocalContentAlpha provides ContentAlpha.medium,
            content = subtitle,
        )
    }
}


@Composable
fun SettingsTileAction(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun SettingsTileIcon(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    if (icon == null) {
        Spacer(
            modifier = modifier
                .padding(end = 16.dp)
                .size(width = 16.dp, height = 64.dp),
        )
    } else {
        Box(
            modifier = modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}


@Composable
fun WrapContentColor(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val alpha = if (enabled) {
        1.0f
    } else {
        0.6f
    }
    val contentColor = LocalContentColor.current.copy(alpha = alpha)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalContentAlpha provides alpha,
    ) {
        content()
    }
}
