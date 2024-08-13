/*
 * Copyright (c) 2021-2023 alorma
 */

package lib.phonograph.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


@Composable
fun rememberBooleanSettingState(defaultValue: Boolean = false): SettingValueState<Boolean> {
    return remember { InMemoryBooleanSettingValueState(defaultValue) }
}

@Composable
fun rememberFloatSettingState(defaultValue: Float = 0f): SettingValueState<Float> {
    return remember { InMemoryFloatSettingValueState(defaultValue) }
}

@Composable
fun rememberIntSettingState(defaultValue: Int = -1): SettingValueState<Int> {
    return remember { InMemoryIntSettingValueState(defaultValue) }
}

@Composable
fun rememberStringSettingState(defaultValue: String? = null): SettingValueState<String?> {
    return remember { InMemoryStringSettingValueState(defaultValue) }
}

@Composable
fun rememberIntSetSettingState(defaultValue: Set<Int> = emptySet()): SettingValueState<Set<Int>> {
    return remember { InMemoryIntSetSettingValueState(defaultValue) }
}


private class InMemoryBooleanSettingValueState(private val defaultValue: Boolean) :
        SettingValueState<Boolean> {
    override var value: Boolean by mutableStateOf(defaultValue)
    override fun reset() {
        value = defaultValue
    }
}

private class InMemoryFloatSettingValueState(private val defaultValue: Float) :
        SettingValueState<Float> {
    override var value: Float by mutableFloatStateOf(defaultValue)
    override fun reset() {
        value = defaultValue
    }
}

private class InMemoryIntSettingValueState(private val defaultValue: Int) :
        SettingValueState<Int> {
    override var value: Int by mutableIntStateOf(defaultValue)
    override fun reset() {
        value = defaultValue
    }
}

private class InMemoryStringSettingValueState(private val defaultValue: String?) :
        SettingValueState<String?> {
    override var value: String? by mutableStateOf(defaultValue)
    override fun reset() {
        value = defaultValue
    }
}

private class InMemoryIntSetSettingValueState(private val defaultValue: Set<Int>) :
        SettingValueState<Set<Int>> {
    override var value: Set<Int> by mutableStateOf(defaultValue)
    override fun reset() {
        value = defaultValue
    }
}
