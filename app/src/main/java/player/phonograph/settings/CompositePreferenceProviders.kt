/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.settings

import player.phonograph.model.coil.ImageSourceConfig
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.model.pages.PagesConfig
import player.phonograph.model.repo.MusicLibraryBackendOptions
import player.phonograph.model.repo.PROVIDER_INTERNAL_DATABASE
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_DIRECT
import player.phonograph.model.repo.SYNC_MODE_EXCLUDE_GENRES
import player.phonograph.model.repo.SYNC_MODE_STANDARD
import player.phonograph.model.sort.SortMode
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.util.time.TimeInterval
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Provider for Composite Preference (not primitive type)
 */
interface CompositePreferenceProvider<T> {
    val defaultValue: () -> T
    fun flow(dataStore: DataStore<Preferences>): Flow<T>
    suspend fun edit(dataStore: DataStore<Preferences>, value: () -> T)
}

/**
 * Provider for Composite Preference with only one underlying PrimitiveKey [backField] only.
 * @param backField underlying PrimitiveKey, that can be simply deserialized and serialized
 * @param T backfield primitive type
 * @param R actual composite preference type
 */
sealed class MonoPreferenceProvider<T, R>(private val backField: PrimitiveKey<R>) : CompositePreferenceProvider<T> {

    abstract fun read(flow: Flow<R>): Flow<T>
    abstract fun save(data: T): R
    abstract fun default(raw: R): T

    override val defaultValue: () -> T get() = { default(backField.defaultValue()) }

    override fun flow(dataStore: DataStore<Preferences>): Flow<T> {
        val preferencesFlow = dataStore.data
        val rawFlow = preferencesFlow.map { it[backField.preferenceKey] ?: backField.defaultValue() }
        return read(rawFlow)
    }

    override suspend fun edit(dataStore: DataStore<Preferences>, value: () -> T) {
        dataStore.edit {
            it[backField.preferenceKey] = save(value())
        }
    }
}

data object CheckUpdateIntervalPreferenceProvider :
        MonoPreferenceProvider<Duration, String>(Keys._checkUpdateInterval) {

    override fun default(raw: String): Duration = Duration.from(raw) ?: Duration.Day(1)

    override fun read(flow: Flow<String>): Flow<Duration> =
        flow.map { Duration.from(it) ?: defaultValue() }

    override fun save(data: Duration): String =
        data.serialise()
}

class SortModePreferenceProvider(backField: PrimitiveKey<String>) :
        MonoPreferenceProvider<SortMode, String>(backField) {
    override fun default(raw: String): SortMode = SortMode.deserialize(raw)
    override fun read(flow: Flow<String>): Flow<SortMode> = flow.map { SortMode.deserialize(it) }
    override fun save(data: SortMode): String = data.serialize()
}

class ItemLayoutProvider(backField: PrimitiveKey<Int>) :
        MonoPreferenceProvider<ItemLayoutStyle, Int>(backField) {
    override fun default(raw: Int): ItemLayoutStyle = ItemLayoutStyle.from(raw)
    override fun read(flow: Flow<Int>): Flow<ItemLayoutStyle> = flow.map { ItemLayoutStyle.from(it) }
    override fun save(data: ItemLayoutStyle): Int = data.ordinal
}

/**
 * Provider for Composite Preference in JSON, stored in one underlying PrimitiveKey [backField]
 * @param backField underlying PrimitiveKey stores raw JSON
 * @param T the Composite type
 */
sealed class JsonPreferenceProvider<T>(
    private val backField: PrimitiveKey<String>,
    override val defaultValue: () -> T,
) : CompositePreferenceProvider<T> {

    abstract fun decode(string: String): T
    abstract fun encode(data: T): String
    abstract fun validate(data: T): Boolean

    override fun flow(dataStore: DataStore<Preferences>): Flow<T> {
        val rawFlow = dataStore.data.map { it[backField.preferenceKey] ?: backField.defaultValue() }
        return rawFlow.map { raw ->
            try {
                decode(raw).takeIf { validate(it) } ?: reset(dataStore)
            } catch (e: SerializationException) {
                Log.e(TAG, "Glitch config: $raw", e)
                reset(dataStore)
            }
        }
    }

    override suspend fun edit(dataStore: DataStore<Preferences>, value: () -> T) {
        dataStore.edit {
            it[backField.preferenceKey] = encode(value())
        }
    }

    suspend fun reset(dataStore: DataStore<Preferences>): T = try {
        Log.i(TAG, "Reset to default")
        edit(dataStore, defaultValue) // reset to default
        defaultValue()
    } catch (_: Exception) {
        defaultValue()
    }


    companion object {
        private const val TAG = "JsonPreferenceProvider"
    }
}

object NotificationActionsPreferenceProvider : JsonPreferenceProvider<NotificationActionsConfig>(
    Keys._notificationActionsJson, { NotificationActionsConfig.DEFAULT }
) {

    override fun decode(string: String): NotificationActionsConfig =
        parser.decodeFromString<NotificationActionsConfig>(string)

    override fun encode(data: NotificationActionsConfig): String =
        parser.encodeToString(data)

    override fun validate(data: NotificationActionsConfig): Boolean =
        data.actions.isNotEmpty()

    private val parser by lazy {
        Json { ignoreUnknownKeys = true }
    }
}

object CoilImageSourcePreferenceProvider : JsonPreferenceProvider<ImageSourceConfig>(
    Keys._imageSourceConfigJson, { ImageSourceConfig.DEFAULT }
) {

    override fun decode(string: String): ImageSourceConfig =
        parser.decodeFromString<ImageSourceConfig>(string)

    override fun encode(data: ImageSourceConfig): String =
        parser.encodeToString(data)

    override fun validate(data: ImageSourceConfig): Boolean =
        data.sources.isNotEmpty()

    private val parser by lazy {
        Json { ignoreUnknownKeys = true }
    }

}

object HomeTabConfigPreferenceProvider : JsonPreferenceProvider<PagesConfig>(
    Keys._homeTabConfigJson, { PagesConfig.DEFAULT_CONFIG }
) {

    override fun decode(string: String): PagesConfig =
        parser.decodeFromString<PagesConfig>(string)

    override fun encode(data: PagesConfig): String =
        parser.encodeToString(data)

    override fun validate(data: PagesConfig): Boolean =
        data.tabs.isNotEmpty()

    private val parser by lazy {
        Json { ignoreUnknownKeys = true }
    }

}

data object NowPlayingScreenStylePreferenceProvider : JsonPreferenceProvider<NowPlayingScreenStyle>(
    Keys._nowPlayingScreenStyle, { NowPlayingScreenStyle.DEFAULT }
) {
    override fun decode(string: String): NowPlayingScreenStyle =
        parser.decodeFromString<NowPlayingScreenStyle>(string)

    override fun encode(data: NowPlayingScreenStyle): String =
        parser.encodeToString(data)

    override fun validate(data: NowPlayingScreenStyle): Boolean = true

    private val parser by lazy {
        Json { ignoreUnknownKeys = true }
    }
}

/**
 * Provider for Composite Preference, which is read only and may be derived from multiple keys
 */
sealed class ReadOnlyPreferenceProvider<T> : CompositePreferenceProvider<T> {

    fun <T> readPrimitiveKey(preferences: Flow<Preferences>, key: PrimitiveKey<T>): Flow<T> =
        preferences.map { it[key.preferenceKey] ?: key.defaultValue() }

    override suspend fun edit(dataStore: DataStore<Preferences>, value: () -> T) {
        // unable to edit
    }
}

object LastAddedCutOffDurationPreferenceProvider : ReadOnlyPreferenceProvider<Long>() {
    override val defaultValue: () -> Long get() = { System.currentTimeMillis() }

    override fun flow(dataStore: DataStore<Preferences>): Flow<Long> {
        val duration = readPrimitiveKey(dataStore.data, Keys._lastAddedCutOffDuration)
            .map(Duration::from)
        val mode = readPrimitiveKey(dataStore.data, Keys._lastAddedCutOffMode)
            .map(TimeIntervalCalculationMode::from)
        return combine(mode, duration) { calculationMode, lastAddedDuration ->
            if (calculationMode != null && lastAddedDuration != null) {
                System.currentTimeMillis() - when (calculationMode) {
                    TimeIntervalCalculationMode.PAST -> TimeInterval.past(lastAddedDuration)
                    TimeIntervalCalculationMode.RECENT -> TimeInterval.recently(lastAddedDuration)
                }
            } else {
                System.currentTimeMillis()
            }
        }
    }
}


object MusicLibraryBackendPreferenceProvider : ReadOnlyPreferenceProvider<MusicLibraryBackendOptions>() {
    override val defaultValue: () -> MusicLibraryBackendOptions
        get() = {
            MusicLibraryBackendOptionsParser(
                Keys.musicLibrarySource.defaultValue(),
                Keys.musicLibrarySyncMode.defaultValue(),
            )
        }

    override fun flow(dataStore: DataStore<Preferences>): Flow<MusicLibraryBackendOptions> {
        val musicLibrarySource = readPrimitiveKey(dataStore.data, Keys.musicLibrarySource)
        val musicLibrarySyncMode = readPrimitiveKey(dataStore.data, Keys.musicLibrarySyncMode)
        return combine(musicLibrarySource, musicLibrarySyncMode) { source: String, syncMode: String ->
            MusicLibraryBackendOptionsParser(source, syncMode)
        }
    }

    class MusicLibraryBackendOptionsParser(
        dataSource: String,
        syncMode: String,
    ) : MusicLibraryBackendOptions {
        override val useMediaStoreSongs: Boolean = dataSource == PROVIDER_MEDIASTORE_DIRECT
        override val useMediaStoreArtists: Boolean = dataSource == PROVIDER_MEDIASTORE_DIRECT
        override val useMediaStoreAlbums: Boolean = dataSource == PROVIDER_MEDIASTORE_DIRECT
        override val useMediaStoreGenres: Boolean =
            dataSource == PROVIDER_MEDIASTORE_DIRECT || (dataSource == PROVIDER_INTERNAL_DATABASE && syncMode == SYNC_MODE_EXCLUDE_GENRES)

        override val syncBasicDatabase: Boolean = dataSource == PROVIDER_MEDIASTORE_DIRECT
        override val syncWithGenres: Boolean = syncMode == SYNC_MODE_STANDARD
    }
}
