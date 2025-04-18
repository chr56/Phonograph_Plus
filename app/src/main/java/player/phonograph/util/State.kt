/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


/**
 * collect a Flow within current Lifecycle
 * @param flow kotlin [Flow] which observed
 * @param state collect while current host's lifecycle at-least repeated state
 * @param coroutineContext the [CoroutineContext] when launching from [coroutineScope]
 * @param coroutineScope the [CoroutineScope] where [flowCollector] runs
 * @param flowCollector functional interface: (T) -> Unit
 */
inline fun <reified T> LifecycleOwner.observe(
    flow: Flow<T>,
    state: Lifecycle.State = Lifecycle.State.CREATED,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    coroutineScope: CoroutineScope = lifecycle.coroutineScope,
    distinctive: Boolean = false,
    flowCollector: FlowCollector<T>,
) = observe(
    lifecycle,
    flow,
    state,
    coroutineContext,
    coroutineScope,
    distinctive,
    flowCollector,
)

/**
 * collect a Flow within target Lifecycle
 * @param lifecycle target [Lifecycle]
 * @param flow kotlin [Flow] which observed
 * @param state collect while current host's lifecycle at-least repeated state
 * @param coroutineContext the [CoroutineContext] when launching from [coroutineScope]
 * @param coroutineScope the [CoroutineScope] where [flowCollector] runs
 * @param flowCollector functional interface: (T) -> Unit
 */
inline fun <reified T> observe(
    lifecycle: Lifecycle,
    flow: Flow<T>,
    state: Lifecycle.State = Lifecycle.State.CREATED,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    coroutineScope: CoroutineScope = lifecycle.coroutineScope,
    distinctive: Boolean = false,
    flowCollector: FlowCollector<T>,
) {
    coroutineScope.launch(coroutineContext) {
        lifecycle.repeatOnLifecycle(state) {
            (if (distinctive) flow.distinctUntilChanged() else flow).collect(flowCollector)
        }
    }
}

/**
 * collect a Flow without Lifecycle
 * @param flow kotlin [Flow] which observed
 * @param coroutineContext the [CoroutineContext] when launching from [coroutineContext]
 * @param coroutineScope the [CoroutineScope] where [flowCollector] runs
 * @param flowCollector functional interface: (T) -> Unit
 */
inline fun <reified T> LifecycleOwner.observeOnce(
    flow: Flow<T>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    coroutineScope: CoroutineScope = lifecycle.coroutineScope,
    flowCollector: FlowCollector<T>,
) {
    coroutineScope.launch(coroutineContext) {
        flow.collect(flowCollector)
    }
}