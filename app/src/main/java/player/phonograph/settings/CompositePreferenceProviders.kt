/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.NowPlayingScreen
import player.phonograph.model.coil.ImageSourceConfig
import player.phonograph.model.file.defaultStartDirectory
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.model.pages.PagesConfig
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.util.file.safeGetCanonicalPath
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
import java.io.File

/**
 * Provider for Composite Preference (not primitive type)
 */
interface CompositePreferenceProvider<T> {
    val default: () -> T
    suspend fun flow(dataStore: DataStore<Preferences>): Flow<T>
    suspend fun edit(dataStore: DataStore<Preferences>, value: () -> T)
}
/**
 * Provider for Composite Preference which has only one [backField],
 * and we just need to deserialize and serialize
 * @param T backfield primitive type
 * @param R actual composite preference type
 */
sealed class MonoPreferenceProvider<T, R>(
    private val backField: PrimitiveKey<R>,
    override val default: () -> T,
) : CompositePreferenceProvider<T> {

    abstract fun read(flow: Flow<R>): Flow<T>
    abstract fun save(data: T): R

    override suspend fun flow(dataStore: DataStore<Preferences>): Flow<T> {
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
        MonoPreferenceProvider<Duration, String>(
            Keys._checkUpdateInterval, { Duration.Day(1) }
        ) {

    override fun read(flow: Flow<String>): Flow<Duration> =
        flow.map { Duration.from(it) ?: default() }

    override fun save(data: Duration): String =
        data.serialise()
}

data object NowPlayingScreenPreferenceProvider :
        MonoPreferenceProvider<NowPlayingScreen, Int>(
            Keys._nowPlayingScreenIndex, { NowPlayingScreen.CARD }
        ) {

    override fun read(flow: Flow<Int>): Flow<NowPlayingScreen> = flow.map { id ->
        var screen = NowPlayingScreen.CARD
        for (nowPlayingScreen in NowPlayingScreen.entries) {
            if (nowPlayingScreen.id == id) screen = nowPlayingScreen
        }
        screen
    }

    override fun save(data: NowPlayingScreen): Int = data.id
}

data object StartDirectoryPreferenceProvider :
        MonoPreferenceProvider<File, String>(Keys._startDirectoryPath, { defaultStartDirectory }) {

    override fun read(flow: Flow<String>): Flow<File> = flow.map { path ->
        File(path)
    }

    override fun save(data: File): String = safeGetCanonicalPath(data)
}

sealed class SortModePreferenceProvider(backField: PrimitiveKey<String>) :
        MonoPreferenceProvider<SortMode, String>(
            backField, { SortMode(SortRef.ID) }
        ) {
    override fun read(flow: Flow<String>): Flow<SortMode> = flow.map { SortMode.deserialize(it) }
    override fun save(data: SortMode): String = data.serialize()

    data object SongSortMode : SortModePreferenceProvider(Keys._songSortMode)
    data object AlbumSortMode : SortModePreferenceProvider(Keys._albumSortMode)
    data object ArtistSortMode : SortModePreferenceProvider(Keys._artistSortMode)
    data object GenreSortMode : SortModePreferenceProvider(Keys._genreSortMode)
    data object PlaylistSortMode : SortModePreferenceProvider(Keys._playlistSortMode)
    data object CollectionSortMode : SortModePreferenceProvider(Keys._collectionSortMode)
    data object FileSortMode : SortModePreferenceProvider(Keys._fileSortMode)
}

sealed class ItemLayoutProvider(backField: PrimitiveKey<Int>, default: () -> ItemLayoutStyle) :
        MonoPreferenceProvider<ItemLayoutStyle, Int>(backField, default) {
    override fun read(flow: Flow<Int>): Flow<ItemLayoutStyle> = flow.map { ItemLayoutStyle.from(it) }
    override fun save(data: ItemLayoutStyle): Int = data.ordinal

    data object SongItemLayoutProvider :
            ItemLayoutProvider(Keys._songItemLayout, { ItemLayoutStyle.LIST_EXTENDED })

    data object AlbumItemLayoutProvider :
            ItemLayoutProvider(Keys._albumItemLayout, { ItemLayoutStyle.LIST_3L })

    data object ArtistItemLayoutProvider :
            ItemLayoutProvider(Keys._artistItemLayout, { ItemLayoutStyle.LIST })

    data object FolderItemLayoutProvider :
            ItemLayoutProvider(Keys._folderItemLayout, { ItemLayoutStyle.LIST })

    data object LandSongItemLayoutProvider :
            ItemLayoutProvider(Keys._songItemLayoutLand, { ItemLayoutStyle.LIST })

    data object LandAlbumItemLayoutProvider :
            ItemLayoutProvider(Keys._albumItemLayoutLand, { ItemLayoutStyle.LIST_3L })

    data object LandArtistItemLayoutProvider :
            ItemLayoutProvider(Keys._artistItemLayoutLand, { ItemLayoutStyle.LIST_3L })

    data object LandFolderItemLayoutProvider :
            ItemLayoutProvider(Keys._folderItemLayoutLand, { ItemLayoutStyle.LIST })
}

object LastAddedCutOffDurationPreferenceProvider : CompositePreferenceProvider<Long> {
    override val default: () -> Long get() = { System.currentTimeMillis() }

    override suspend fun flow(dataStore: DataStore<Preferences>): Flow<Long> {
        val keyDuration = Keys._lastAddedCutOffDuration
        val keyMode = Keys._lastAddedCutOffMode

        val rawDuration = dataStore.data.map { it[keyDuration.preferenceKey] ?: keyDuration.defaultValue() }
        val rawMode = dataStore.data.map { it[keyMode.preferenceKey] ?: keyMode.defaultValue() }


        val duration = rawDuration.map { Duration.from(it) }
        val mode = rawMode.map { TimeIntervalCalculationMode.from(it) }

        return mode.combine(duration) { calculationMode, lastAddedDuration ->
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

    override suspend fun edit(dataStore: DataStore<Preferences>, value: () -> Long) {
        // unable to edit
    }
}


sealed class JsonPreferenceProvider<T>(
    private val backField: PrimitiveKey<String>,
    override val default: () -> T,
) : CompositePreferenceProvider<T> {

    abstract fun decode(string: String): T
    abstract fun encode(data: T): String
    abstract fun validate(data: T): Boolean

    override suspend fun flow(dataStore: DataStore<Preferences>): Flow<T> {
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
        edit(dataStore, default) // reset to default
        default()
    } catch (_: Exception) {
        default()
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