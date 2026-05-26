/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.settings

import android.content.Context
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SettingsObserver(
    context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val settings: Settings = Settings(context),
) {
    fun <T> collect(
        key: PreferenceKey<T>,
        coroutineContext: CoroutineContext = SupervisorJob(),
        collector: FlowCollector<T>,
    ): Job = coroutineScope.launch(coroutineContext) {
        settings[key].flow.distinctUntilChanged().collect(collector)
    }

    fun <T> blocking(key: PreferenceKey<T>): T = settings[key].data
}