/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.settings

import player.phonograph.mechanism.event.EventHub
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object PathFilterSetting {

    suspend fun read(context: Context, excludeMode: Boolean): List<String> =
        preference(context, excludeMode).read().toList()

    suspend fun add(context: Context, excludeMode: Boolean, path: String): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context, excludeMode)
            val existed = preference.read().toMutableSet()
            if (existed.add(path)) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    suspend fun add(context: Context, excludeMode: Boolean, paths: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context, excludeMode)
            val existed = preference.read().toMutableSet()
            if (existed.addAll(paths)) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    suspend fun remove(context: Context, excludeMode: Boolean, path: String): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context, excludeMode)
            val existed = preference.read().toMutableSet()
            if (existed.remove(path)) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    suspend fun remove(context: Context, excludeMode: Boolean, paths: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context, excludeMode)
            val existed = preference.read().toMutableSet()
            if (existed.removeAll(paths)) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    suspend fun edit(context: Context, excludeMode: Boolean, from: String, to: String): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context, excludeMode)
            val existed = preference.read().toMutableSet()
            if (existed.remove(from)) {
                if (existed.add(to)) {
                    commit(context, preference, existed)
                    return@withContext true
                }
            }
            false
        }

    suspend fun replace(context: Context, excludeMode: Boolean, paths: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context, excludeMode)
            commit(context, preference, paths.toSet())
            true
        }

    suspend fun clear(context: Context, excludeMode: Boolean) =
        withContext(Dispatchers.IO) {
            commit(context, preference(context, excludeMode), emptySet())
        }

    private suspend fun commit(context: Context, preference: Preference<Set<String>>, paths: Set<String>) {
        preference.edit { paths }
        delay(160)
        notifyMediaStoreChanged(context)
    }

    private fun preference(context: Context, excludeMode: Boolean) =
        Setting(context)[if (excludeMode) Keys.pathFilterExcludePaths else Keys.pathFilterIncludePaths]

    private fun notifyMediaStoreChanged(context: Context) =
        EventHub.sendEvent(context.applicationContext, EventHub.EVENT_MEDIASTORE_CHANGED)
}