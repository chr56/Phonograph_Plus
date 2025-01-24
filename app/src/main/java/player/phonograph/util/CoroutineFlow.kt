/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util

import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import android.content.Context
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SettingObserver(
    val context: Context,
    private val coroutineScope: CoroutineScope = context.lifecycleScopeOrNewOne(Dispatchers.IO),
    private val setting: Setting = Setting(context),
) {
    fun <T> collect(
        key: PrimitiveKey<T>,
        coroutineContext: CoroutineContext = SupervisorJob(),
        collector: FlowCollector<T>,
    ) {
        coroutineScope.launch(coroutineContext) {
            setting[key].flow.distinctUntilChanged().collect(collector)
        }
    }
}