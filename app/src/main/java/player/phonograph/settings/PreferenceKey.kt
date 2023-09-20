/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import androidx.datastore.preferences.core.Preferences

/**
 * Preference Key (Container)
 */
sealed interface PreferenceKey<T>

/**
 * Key container of primitive type preference
 */
sealed class PrimitiveKey<T>(
    val preferenceKey: Preferences.Key<T>,
    val defaultValue: () -> T,
) : PreferenceKey<T>

/**
 * Key container of composite type preference
 */
sealed class CompositeKey<T>(
    val valueProvider: CompositePreferenceProvider<T>,
) : PreferenceKey<T>