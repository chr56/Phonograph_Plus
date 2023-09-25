/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import android.content.Context


class SettingStore(val context: Context) {

    operator fun <T> get(key: PrimitiveKey<T>): PrimitivePreference<T> =
        PrimitivePreference(key, context)

    @Suppress("PropertyName")
    val Composites = CompositesSettingStore()

    inner class CompositesSettingStore {
        operator fun <T> get(key: CompositeKey<T>): CompositePreference<T> =
            CompositePreference(key, context)
    }

}