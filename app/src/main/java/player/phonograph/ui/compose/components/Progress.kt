/*
 *  Copyright (c) 2022~2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun Progress(text: StateFlow<String>) {//
    val str = text.collectAsState()
    Column(Modifier.padding(16.dp)) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colors.secondary
        )
        Text(
            text = str.value,
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}