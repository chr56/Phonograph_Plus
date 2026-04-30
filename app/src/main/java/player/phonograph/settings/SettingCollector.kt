/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.settings

import player.phonograph.App
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingCollector<T>(val preferenceKey: () -> PreferenceKey<T>) {

    private var current: T? = null
    private var job: Job? = null

    suspend fun retrieve(context: Context): T = current ?: init(context)

    private suspend fun init(context: Context): T {
        val key = preferenceKey()
        val flow = Setting(context)[key].flow
        if (job == null) {
            job = coroutineScope(context).launch {
                flow.collect { current = it }
            }
        } else {
            Log.d("SettingCollector", "$key is already init once before!")
        }
        return withContext(Dispatchers.IO) { flow.first() }
    }

    private fun coroutineScope(context: Context): CoroutineScope {
        val appScope = (context.applicationContext as? App)?.appScope
        return appScope ?: (scope ?: CoroutineScope(Dispatchers.IO).also {
            scope = it
        })
    }

    private var scope: CoroutineScope? = null
}