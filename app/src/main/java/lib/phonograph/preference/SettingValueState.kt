/*
 * Copyright (c) 2021-2023 alorma
 */

package lib.phonograph.preference

import kotlin.reflect.KProperty


interface SettingValueState<T> {
    fun reset()

    var value: T
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> SettingValueState<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> SettingValueState<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) {
    this.value = value
}