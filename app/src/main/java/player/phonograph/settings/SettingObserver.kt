/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.settings

import android.content.Context
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SettingObserver(
    context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
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

    fun <T> collect(
        key: CompositeKey<T>,
        coroutineContext: CoroutineContext = SupervisorJob(),
        collector: FlowCollector<T>,
    ) {
        coroutineScope.launch(coroutineContext) {
            setting.Composites[key].flow().distinctUntilChanged().collect(collector)
        }
    }

    fun <T> blocking(key: PrimitiveKey<T>): T = setting[key].data
    fun <T> blocking(key: CompositeKey<T>): T = setting.Composites[key].data
}