/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.settings

import player.phonograph.mechanism.event.EventHub
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

abstract class AbsStringSetSetting {

    /**
     * Underlying [Preference]
     */
    protected abstract fun preference(context: Context): Preference<Set<String>>

    /**
     * Callback when it has changed.
     */
    protected abstract fun onChanged(context: Context, content: Set<String>)

    /**
     * Reads and returns the current content in the preference.
     */
    suspend fun read(context: Context): Set<String> = preference(context).read()

    /**
     * Adds an item to preference if it does not already exist.
     * @return true, if the new item was added
     */
    suspend fun add(context: Context, item: String): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context)
            val existed = preference.read().toMutableSet()
            if (existed.add(item)) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    /**
     * Adds multiple items to preference.
     * @return true, if any new items were added
     */
    suspend fun add(context: Context, items: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context)
            val existed = preference.read().toMutableSet()
            if (existed.addAll(items)) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    /**
     * Removes an item from preference if it exists.
     * @return true, if the new item was removed
     */
    suspend fun remove(context: Context, item: String): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context)
            val existed = preference.read().toMutableSet()
            if (existed.remove(item)) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    /**
     * Removes multiple items from preference.
     * @return true, if any items were removed
     */
    suspend fun remove(context: Context, items: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context)
            val existed = preference.read().toMutableSet()
            if (existed.removeAll(items.toSet())) {
                commit(context, preference, existed)
                true
            } else {
                false
            }
        }

    /**
     * Replaces an existing item with a new one.
     * @return true, if success
     */
    suspend fun edit(context: Context, from: String, to: String): Boolean =
        withContext(Dispatchers.IO) {
            val preference = preference(context)
            val existed = preference.read().toMutableSet()
            if (existed.remove(from)) {
                if (existed.add(to)) {
                    commit(context, preference, existed)
                    return@withContext true
                }
            }
            false
        }

    /**
     * Replaces the entire items.
     */
    suspend fun replace(context: Context, items: Collection<String>) =
        withContext(Dispatchers.IO) {
            commit(context, preference(context), items.toSet())
        }


    /**
     * Reset preference to default
     */
    suspend fun reset(context: Context) =
        withContext(Dispatchers.IO) {
            val preference = preference(context)
            val default = preference.default
            commit(context, preference, default)
        }

    /**
     * Removes all items from preference.
     */
    suspend fun clear(context: Context) =
        withContext(Dispatchers.IO) {
            commit(context, preference(context), emptySet())
        }

    protected suspend fun commit(context: Context, preference: Preference<Set<String>>, content: Set<String>) {
        preference.edit { content }
        delay(160)
        onChanged(context, content)
    }

}

class PathFilterSetting(private var excludeMode: Boolean) : AbsStringSetSetting() {

    fun mode(newMode: Boolean): PathFilterSetting {
        excludeMode = newMode
        return this
    }

    override fun preference(context: Context): Preference<Set<String>> =
        Setting(context)[if (excludeMode) Keys.pathFilterExcludePaths else Keys.pathFilterIncludePaths]

    override fun onChanged(context: Context, content: Set<String>) {
        EventHub.sendEvent(context.applicationContext, EventHub.EVENT_MUSIC_LIBRARY_CHANGED)
    }
}