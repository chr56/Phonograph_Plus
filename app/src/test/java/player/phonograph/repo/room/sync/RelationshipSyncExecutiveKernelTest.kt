/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import player.phonograph.mechanism.metadata.RelationshipResolver
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.sync.DataSource
import player.phonograph.model.repo.sync.ProgressConnection
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.room.dao.AlbumManipulateDao
import player.phonograph.repo.room.dao.AlbumQueryDao
import player.phonograph.repo.room.dao.ArtistManipulateDao
import player.phonograph.repo.room.dao.ArtistQueryDao
import player.phonograph.repo.room.dao.GenreManipulateDao
import player.phonograph.repo.room.dao.GenreQueryDao
import player.phonograph.repo.room.dao.RelationshipManipulateDao
import player.phonograph.repo.room.dao.RelationshipQueryDao
import player.phonograph.repo.room.dao.SongManipulateDao
import player.phonograph.repo.room.dao.SongQueryDao
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.GenreEntity
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageGenreAndSong
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ALBUM_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_COMPOSER
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_FEATURE_ARTIST
import player.phonograph.repo.room.entity.MediastoreSongEntity
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.`when` as whenever

class RelationshipSyncExecutiveKernelTest {
    private var _withSyncLog = false

    private lateinit var source: DataSource
    private lateinit var database: MusicDatabaseDataSink

    private lateinit var songQuery: SongQueryDao
    private lateinit var songManipulate: SongManipulateDao
    private lateinit var albumQuery: AlbumQueryDao
    private lateinit var albumManipulate: AlbumManipulateDao
    private lateinit var artistQuery: ArtistQueryDao
    private lateinit var artistManipulate: ArtistManipulateDao
    private lateinit var genreQuery: GenreQueryDao
    private lateinit var genreManipulate: GenreManipulateDao
    private lateinit var relationshipQuery: RelationshipQueryDao
    private lateinit var relationshipManipulate: RelationshipManipulateDao

    private var channel: PrintingProgressConnection? = null

    @Before
    fun setUp() {
        runBlocking {
            source = mock()
            songQuery = mock()
            songManipulate = mock()
            albumQuery = mock()
            albumManipulate = mock()
            artistQuery = mock()
            artistManipulate = mock()
            genreQuery = mock()
            genreManipulate = mock()
            relationshipQuery = mock()
            relationshipManipulate = mock()

            database = object : MusicDatabaseDataSink {
                override fun SongQueryDao() = songQuery
                override fun SongManipulateDao() = songManipulate
                override fun AlbumQueryDao() = albumQuery
                override fun AlbumManipulateDao() = albumManipulate
                override fun ArtistQueryDao() = artistQuery
                override fun ArtistManipulateDao() = artistManipulate
                override fun GenreQueryDao() = genreQuery
                override fun GenreManipulateDao() = genreManipulate
                override fun RelationshipQueryDao() = relationshipQuery
                override fun RelationshipManipulateDao() = relationshipManipulate
                override suspend fun <R> withTransaction(block: suspend () -> R): R = block()
            }
            whenever(songQuery.latest()).thenReturn(null)
            whenever(artistQuery.count()).thenReturn(0)
            whenever(albumQuery.count()).thenReturn(0)
            whenever(artistQuery.artistSongCount(org.mockito.ArgumentMatchers.anyLong())).thenReturn(1)
            whenever(artistQuery.artistAlbumCount(org.mockito.ArgumentMatchers.anyLong())).thenReturn(1)

            if (_withSyncLog) channel = PrintingProgressConnection()
        }
    }

    @Test
    fun `multiple songs aggregate album metadata and preserve per-song relationships`() = runBlocking {
        val songs = listOf(
            song(
                id = 1,
                title = "Opening",
                albumId = 10,
                album = "Album A",
                artist = "Artist One",
                albumArtist = "Album Artist",
                year = 2020,
                dateModified = 20,
            ),
            song(
                id = 2,
                title = "Finale (feat. Guest)",
                albumId = 10,
                album = "Album A",
                artist = "Artist Two",
                albumArtist = "Album Artist",
                year = 2024,
                dateModified = 40,
            ),
            song(
                id = 3,
                title = "Bonus",
                albumId = 11,
                album = "Album A",
                artist = "Artist Three",
                albumArtist = null,
                year = 2022,
                dateModified = 30
            ),
        )
        givenSongs(songs)
        execute()

        verify(albumManipulate, Mockito.times(2)).update(anyList())
        val created = capturedUpdates<AlbumEntity>(albumManipulate)
        assertEquals(setOf(10L, 11L), created.map { it.albumId }.toSet())
        assertEquals(2, created.first { it.albumId == 10L }.songCount)
        assertEquals(2024, created.first { it.albumId == 10L }.year)
        assertEquals(40L, created.first { it.albumId == 10L }.dateModified)
        assertEquals(1, created.first { it.albumId == 11L }.songCount)
        assertEquals("Artist Three", created.first { it.albumId == 11L }.albumArtistName)

        val links = captureSongLinks()
        val expectedLinks = setOf(
            link(1, "Artist One", ROLE_ARTIST),
            link(1, "Album Artist", ROLE_ALBUM_ARTIST),
            link(2, "Artist Two", ROLE_ARTIST),
            link(2, "Album Artist", ROLE_ALBUM_ARTIST),
            link(2, "Guest", ROLE_FEATURE_ARTIST),
            link(3, "Artist Three", ROLE_ARTIST),
        )
        assertEquals(expectedLinks, links.toSet())
        assertEquals(expectedLinks.size, links.size)
    }

    @Test
    fun `refresh writes each parsed artist role`() = runBlocking {
        givenSongs(
            song(
                title = "Track (feat. Guest Artist)",
                artist = "Main Artist;Second Artist",
                albumArtist = "Album Artist",
                composer = "Composer",
            )
        )

        execute(countComposerAsArtist = true)

        val links = captureSongLinks()
        assertEquals(
            setOf(
                link(1, "Main Artist", ROLE_ARTIST),
                link(1, "Second Artist", ROLE_ARTIST),
                link(1, "Album Artist", ROLE_ALBUM_ARTIST),
                link(1, "Composer", ROLE_COMPOSER),
                link(1, "Guest Artist", ROLE_FEATURE_ARTIST),
            ),
            links.toSet()
        )
        assertEquals(5, links.size)

        val albums = captureAlbumArtists()
        assertEquals(5, albums.size)
        assertEquals(
            setOf("Main Artist", "Second Artist", "Album Artist", "Composer", "Guest Artist")
                .map { 10L to it.hashCode().toLong() }
                .toSet(),
            albums.map { it.albumId to it.artistId }.toSet()
        )
    }

    @Test
    fun `refresh parses all artist separators and multiple feature artists`() = runBlocking {
        givenSongs(
            song(
                title = "Track (FEAT. Feature One & Feature Two)",
                album = "Album A (feat. Album Feature)",
                artist = "Main One; Main Two / Main Three & Main Four ft. Main Five",
                albumArtist = "Album Artist; Main One",
                composer = "Composer One / Composer Two",
            )
        )

        execute(countComposerAsArtist = true)

        val links = captureSongLinks()
        val artistNamesById = capturedUpdates<ArtistEntity>(artistManipulate)
            .associate { it.artistId to it.artistName }

        fun namesFor(role: Int) = links.filter { it.role == role }.mapNotNull { artistNamesById[it.artistId] }

        assertEquals(
            setOf("Main One", "Main Two", "Main Three", "Main Four", "Main Five"),
            namesFor(ROLE_ARTIST).toSet()
        )
        assertEquals(
            setOf("Album Artist", "Main One"),
            namesFor(ROLE_ALBUM_ARTIST).toSet()
        )
        assertEquals(
            setOf("Composer One", "Composer Two"),
            namesFor(ROLE_COMPOSER).toSet()
        )
        assertEquals(
            setOf("Feature One", "Feature Two", "Album Feature"),
            namesFor(ROLE_FEATURE_ARTIST).toSet()
        )
        assertEquals(
            setOf(
                "Main One" to ROLE_ARTIST,
                "Main Two" to ROLE_ARTIST,
                "Main Three" to ROLE_ARTIST,
                "Main Four" to ROLE_ARTIST,
                "Main Five" to ROLE_ARTIST,
                "Album Artist" to ROLE_ALBUM_ARTIST,
                "Main One" to ROLE_ALBUM_ARTIST,
                "Composer One" to ROLE_COMPOSER,
                "Composer Two" to ROLE_COMPOSER,
                "Feature One" to ROLE_FEATURE_ARTIST,
                "Feature Two" to ROLE_FEATURE_ARTIST,
                "Album Feature" to ROLE_FEATURE_ARTIST,
            ),
            links.map { artistNamesById[it.artistId].orEmpty() to it.role }.toSet()
        )
        assertEquals(links.distinct().size, links.size)
    }

    @Test
    fun `refresh handles songs without relationship metadata`() = runBlocking {
        givenSongs(
            song(
                album = null,
                artist = null,
                albumArtist = null,
                composer = null,
            )
        )

        execute()

        val albums = capturedUpdates<AlbumEntity>(albumManipulate)
        assertEquals(
            AlbumEntity(10L, "", 0L, "", 2026, 0L, 1),
            albums.single()
        )
        assertTrue(capturedUpdates<ArtistEntity>(artistManipulate).isEmpty())
        assertTrue(captureSongLinks().isEmpty())
        verify(relationshipManipulate).overrideAlbumArtists(emptyList())
    }

    @Test
    fun `refresh reuses existing artists while creating unknown artists`() = runBlocking {
        whenever(artistQuery.count()).thenReturn(1)
        whenever(artistQuery.named(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(
            listOf(ArtistEntity(artistId = 42L, artistName = "Known Artist"))
        )
        givenSongs(
            song(
                artist = "Known Artist;New Artist",
                albumArtist = "Known Artist",
            )
        )

        execute()

        assertEquals(
            listOf(ArtistEntity(artistId = "New Artist".hashCode().toLong(), artistName = "New Artist")),
            capturedUpdates<ArtistEntity>(artistManipulate)
        )
        val links = captureSongLinks()
        val expectedLinks = setOf(
            LinkageSongAndArtist(1L, 42L, ROLE_ARTIST),
            LinkageSongAndArtist(1L, "New Artist".hashCode().toLong(), ROLE_ARTIST),
            LinkageSongAndArtist(1L, 42L, ROLE_ALBUM_ARTIST),
        )
        assertEquals(expectedLinks, links.toSet())
        assertEquals(expectedLinks.size, links.size)
    }

    @Test
    fun `genres are split deduplicated and linked`() = runBlocking {
        givenSongs(song())
        whenever(source.songGenres(listOf(1L))).thenReturn(
            mapOf(1L to listOf(Genre(10, "Pop, Rock", 0), Genre(11, "Rock", 0), Genre(12, " ", 0)))
        )
        whenever(genreQuery.all(SortMode(SortRef.MODIFIED_DATE, true))).thenReturn(emptyList())
        whenever(genreManipulate.update(GenreEntity(id = 0, name = "Pop", mediastoreId = 10))).thenReturn(101L)
        whenever(genreManipulate.update(GenreEntity(id = 0, name = "Rock", mediastoreId = 10))).thenReturn(102L)
        whenever(relationshipQuery.songIdsOfGenre(101L)).thenReturn(listOf(1L))
        whenever(relationshipQuery.songIdsOfGenre(102L)).thenReturn(listOf(1L))

        execute(withGenres = true)

        val inserted = mockingDetails(genreManipulate).invocations
            .asSequence()
            .filter { it.method.name == "update" }
            .mapNotNull { it.arguments.firstOrNull() as? GenreEntity }
            .toList()
        assertEquals(
            listOf(
                GenreEntity(id = 0, name = "Pop", mediastoreId = 10),
                GenreEntity(id = 0, name = "Rock", mediastoreId = 10),
            ),
            inserted
        )
        verify(relationshipManipulate).overrideGenreSongs(
            listOf(
                LinkageGenreAndSong(genreId = 101L, songId = 1L),
                LinkageGenreAndSong(genreId = 102L, songId = 1L),
            )
        )
        verify(genreManipulate).updateCounter(genreQuery, 101L, 1)
        verify(genreManipulate).updateCounter(genreQuery, 102L, 1)
        verify(relationshipManipulate).removeSongs(listOf(1L))
        Unit
    }

    @Test
    fun `refresh omits feature artists when resolver extraction is disabled`() = runBlocking {
        givenSongs(song(title = "Track (feat. Guest)", artist = "Main Artist"))

        val resolver = RelationshipResolver.default().apply {
            enableFeatureArtistsExtraction = false
        }

        execute(relationshipResolver = resolver)

        assertFalse(captureSongLinks().any { it.role == ROLE_FEATURE_ARTIST })
    }


    @Test
    fun `composer is omitted entirely when composer counting is disabled`() = runBlocking {
        givenSongs(song(composer = "Composer"))

        execute(countComposerAsArtist = false)

        assertFalse(captureSongLinks().any { it.role == ROLE_COMPOSER })
        assertFalse(capturedUpdates<ArtistEntity>(artistManipulate).any { it.artistName == "Composer" })
        assertFalse(captureAlbumArtists().any { it.artistId == "Composer".hashCode().toLong() })
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `repeated refresh replaces relationships without duplicates`() = runBlocking {
        val existing = MediastoreSongEntity(mediastorId = 1L, dateModified = 0L)
        whenever(songQuery.latest()).thenReturn(null, existing)
        givenSongs(song())

        execute()
        execute()

        val expectedSongLinks = listOf(link(1, "Artist A", ROLE_ALBUM_ARTIST), link(1, "Artist A", ROLE_ARTIST))
        val songOverrides = mockingDetails(relationshipManipulate).invocations
            .asSequence()
            .filter { it.method.name == "overrideArtistSongs" }
            .map { it.arguments.first() as List<LinkageSongAndArtist> }
            .toList()
        assertEquals(2, songOverrides.size)
        songOverrides.forEach { links ->
            assertEquals(expectedSongLinks.toSet(), links.toSet())
            assertEquals(expectedSongLinks.size, links.size)
        }

        val albumOverrides = mockingDetails(relationshipManipulate).invocations
            .asSequence()
            .filter { it.method.name == "overrideAlbumArtists" }
            .map { it.arguments.first() as List<LinkageAlbumAndArtist> }
            .toList()
        assertEquals(2, albumOverrides.size)
        albumOverrides.forEach { links ->
            assertEquals(1, links.size)
            assertEquals(10L, links.single().albumId)
            assertEquals("Artist A".hashCode().toLong(), links.single().artistId)
        }
        verify(artistManipulate, Mockito.times(2)).updateCounter(
            artistQuery,
            "Artist A".hashCode().toLong(),
            1,
            1,
        )
        Unit
    }

    @Test
    fun `refresh uses latest modified timestamp as source cutoff`() = runBlocking {
        whenever(songQuery.latest()).thenReturn(
            MediastoreSongEntity(mediastorId = 99, dateModified = 123L)
        )
        whenever(source.songs(123L)).thenReturn(listOf(song(dateModified = 124L)))

        assertEquals(1, execute())

        verify(source).songs(123L)
        verify(source, never()).songs(0L)
        verify(songManipulate).update(anyList())
    }

    @Test
    fun `refresh updates existing artist and album metadata`() = runBlocking {
        whenever(artistQuery.count()).thenReturn(1)
        whenever(artistQuery.named(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(
            listOf(ArtistEntity(artistId = 42L, artistName = "Artist A"))
        )
        whenever(albumQuery.count()).thenReturn(1)
        whenever(albumQuery.ids(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(
            listOf(
                AlbumEntity(
                    albumId = 10L,
                    albumName = "Album A",
                    artistId = 42L,
                    albumArtistName = "Artist A",
                    year = 2019,
                    dateModified = 10L,
                    songCount = 2,
                )
            )
        )
        givenSongs(song(year = 2024, dateModified = 40L))

        execute()

        assertTrue(capturedUpdates<ArtistEntity>(artistManipulate).isEmpty())
        verify(artistManipulate).updateCounter(artistQuery, 42L, 1, 1)
        val updatedAlbums = capturedUpdates<AlbumEntity>(albumManipulate)
        assertEquals(1, updatedAlbums.size)
        assertEquals(
            AlbumEntity(10L, "Album A", 42L, "Artist A", 2024, 40L, 3),
            updatedAlbums.single()
        )
    }

    @Test
    fun `refresh reuses existing genres and updates their counter`() = runBlocking {
        givenSongs(song())
        whenever(source.songGenres(listOf(1L))).thenReturn(
            mapOf(1L to listOf(Genre(10L, "Pop", 0)))
        )
        whenever(genreQuery.all(SortMode(SortRef.MODIFIED_DATE, true))).thenReturn(
            listOf(GenreEntity(id = 7L, name = "Pop", mediastoreId = 10L))
        )
        whenever(relationshipQuery.songIdsOfGenre(7L)).thenReturn(listOf(1L, 2L))

        execute(withGenres = true)

        verify(genreManipulate, never()).update(GenreEntity(id = 0L, name = "Pop", mediastoreId = 10L))
        verify(relationshipManipulate).overrideGenreSongs(
            listOf(LinkageGenreAndSong(genreId = 7L, songId = 1L))
        )
        verify(genreManipulate).updateCounter(genreQuery, 7L, 2)
        Unit
    }

    @Test
    fun `cleanup retains genres that still have songs`() = runBlocking {
        whenever(songQuery.total()).thenReturn(2)
        whenever(source.songCount()).thenReturn(1)
        whenever(songQuery.allIds()).thenReturn(listOf(1L, 2L))
        whenever(source.songIds()).thenReturn(setOf(1L))
        whenever(songQuery.ids(listOf(2L))).thenReturn(
            listOf(MediastoreSongEntity(mediastorId = 2L, albumId = 10L))
        )
        whenever(relationshipQuery.artistsOfSongs(listOf(2L))).thenReturn(emptyList())
        whenever(albumQuery.ids(setOf(10L))).thenReturn(emptyList())
        whenever(relationshipQuery.genresOfSongs(listOf(2L))).thenReturn(
            listOf(LinkageGenreAndSong(7L, 2L))
        )
        whenever(relationshipQuery.songIdsOfGenre(7L)).thenReturn(listOf(1L))
        Mockito.doReturn(true).`when`(genreManipulate).updateCounter(genreQuery, 7L, 1)

        assertEquals(1, kernel(withGenres = true).stageClean())

        verify(relationshipManipulate).removeGenreSongs(
            listOf(LinkageGenreAndSong(7L, 2L))
        )
        verify(genreManipulate).updateCounter(genreQuery, 7L, 1)
        verify(genreManipulate, never()).delete(genreQuery, 7L)
        Unit
    }

    @Test
    fun `refresh with genres removes stale links when no genre rows are returned`() = runBlocking {
        givenSongs(song())
        whenever(source.songGenres(listOf(1L))).thenReturn(emptyMap())
        whenever(genreQuery.all(SortMode(SortRef.MODIFIED_DATE, true))).thenReturn(emptyList())

        execute(withGenres = true)

        verify(relationshipManipulate).removeSongs(listOf(1L))
        verify(relationshipManipulate, never()).overrideGenreSongs(anyList())
        assertTrue(capturedUpdates<GenreEntity>(genreManipulate).isEmpty())
        Unit
    }

    @Test
    fun `cleanup removes missing songs and deletes empty related records`() = runBlocking {
        whenever(songQuery.total()).thenReturn(2)
        whenever(source.songCount()).thenReturn(1)
        whenever(songQuery.allIds()).thenReturn(listOf(1L, 2L))
        whenever(source.songIds()).thenReturn(setOf(1L))
        val missing = MediastoreSongEntity(mediastorId = 2L, albumId = 10L)
        whenever(songQuery.ids(listOf(2L))).thenReturn(listOf(missing))
        val artistLink = LinkageSongAndArtist(2L, 42L, ROLE_ARTIST)
        whenever(relationshipQuery.artistsOfSongs(listOf(2L))).thenReturn(listOf(artistLink))
        whenever(albumQuery.ids(setOf(10L))).thenReturn(
            listOf(AlbumEntity(10L, "Album A", 42L, "Artist A", 2020, 1L, 1))
        )
        whenever(relationshipQuery.genresOfSongs(listOf(2L))).thenReturn(
            listOf(LinkageGenreAndSong(7L, 2L))
        )
        whenever(artistQuery.artistSongCount(42L)).thenReturn(0)
        whenever(albumQuery.albumSongCount(10L)).thenReturn(0)
        whenever(relationshipQuery.songIdsOfGenre(7L)).thenReturn(emptyList())
        Mockito.doReturn(true).`when`(artistManipulate).delete(artistQuery, 42L)
        Mockito.doReturn(true).`when`(albumManipulate).delete(albumQuery, 10L)
        Mockito.doReturn(true).`when`(genreManipulate).delete(genreQuery, 7L)

        assertEquals(1, kernel(withGenres = true).stageClean())

        verify(songManipulate).delete(listOf(missing))
        verify(relationshipManipulate).removeArtistSongs(listOf(artistLink))
        verify(artistManipulate).delete(artistQuery, 42L)
        verify(relationshipManipulate).removeArtists(setOf(42L))
        verify(albumManipulate).delete(albumQuery, 10L)
        verify(relationshipManipulate).removeAlbums(setOf(10L))
        verify(relationshipManipulate).removeGenreSongs(listOf(LinkageGenreAndSong(7L, 2L)))
        verify(genreManipulate).delete(genreQuery, 7L)
        Unit
    }

    @Test
    fun `cleanup updates counters for records that still have data`() = runBlocking {
        whenever(songQuery.total()).thenReturn(2)
        whenever(source.songCount()).thenReturn(1)
        whenever(songQuery.allIds()).thenReturn(listOf(1L, 2L))
        whenever(source.songIds()).thenReturn(setOf(1L))
        whenever(songQuery.ids(listOf(2L))).thenReturn(
            listOf(MediastoreSongEntity(mediastorId = 2L, albumId = 10L))
        )
        whenever(relationshipQuery.artistsOfSongs(listOf(2L))).thenReturn(
            listOf(LinkageSongAndArtist(2L, 42L, ROLE_ARTIST))
        )
        whenever(albumQuery.ids(setOf(10L))).thenReturn(
            listOf(AlbumEntity(10L, "Album A", 42L, "Artist A", 2020, 1L, 1))
        )
        whenever(artistQuery.artistSongCount(42L)).thenReturn(2)
        whenever(albumQuery.albumSongCount(10L)).thenReturn(3)
        whenever(artistQuery.artistAlbumCount(42L)).thenReturn(4)
        Mockito.doReturn(true).`when`(artistManipulate)
            .updateCounter(artistQuery, 42L, 2, null)
        Mockito.doReturn(true).`when`(artistManipulate)
            .updateCounter(artistQuery, 42L, null, 4)
        Mockito.doReturn(true).`when`(albumManipulate).updateCounter(albumQuery, 10L, 3)

        assertEquals(1, kernel().stageClean())

        verify(artistManipulate).updateCounter(artistQuery, 42L, songCount = 2)
        verify(albumManipulate).updateCounter(albumQuery, 10L, 3)
        verify(artistManipulate).updateCounter(artistQuery, 42L, albumCount = 4)
        verify(artistManipulate, never()).delete(artistQuery, 42L)
        verify(albumManipulate, never()).delete(albumQuery, 10L)
        Unit
    }

    @Test
    fun `cleanup skips all work when media store count matches database`() = runBlocking {
        whenever(songQuery.total()).thenReturn(2)
        whenever(source.songCount()).thenReturn(2)
        whenever(songQuery.allIds()).thenReturn(listOf(1L, 2L))
        whenever(source.songIds()).thenReturn(setOf(1L, 2L))

        assertEquals(0, kernel().stageClean())

        verifyNoInteractions(songManipulate, artistManipulate, albumManipulate, relationshipManipulate)
    }

    @Test
    fun `execute reports refreshed count and no removals`() = runBlocking {
        givenSongs(song())
        whenever(songQuery.total()).thenReturn(1)
        whenever(source.songCount()).thenReturn(1)
        whenever(songQuery.allIds()).thenReturn(listOf(1L))
        whenever(source.songIds()).thenReturn(setOf(1L))

        val report = kernel().execute()

        assertTrue(report.success)
        assertEquals(1, report.modified)
        assertEquals(0, report.removed)
    }

    @Test
    fun `empty refresh does not write`() = runBlocking {
        givenSongs()

        assertEquals(0, execute())

        verifyNoInteractions(songManipulate, artistManipulate, albumManipulate, relationshipManipulate)
    }

    private fun kernel(
        withGenres: Boolean = false,
        countComposerAsArtist: Boolean = true,
        relationshipResolver: RelationshipResolver = RelationshipResolver.default(),
    ) = RelationshipSyncExecutiveKernel(
        musicDatabase = database,
        musicDataSource = source,
        relationshipResolver = relationshipResolver.apply { this.regardComposerAsArtist = countComposerAsArtist },
        withGenres = withGenres,
        channel = channel,
    )

    private suspend fun givenSongs(vararg songs: Song) {
        whenever(source.songs(0L)).thenReturn(songs.toList())
    }

    private suspend fun givenSongs(songs: List<Song>) {
        whenever(source.songs(0L)).thenReturn(songs)
    }

    private suspend fun execute(
        withGenres: Boolean = false,
        countComposerAsArtist: Boolean = true,
        relationshipResolver: RelationshipResolver = RelationshipResolver.default(),
    ): Int {
        channel?.report()
        return kernel(
            withGenres = withGenres,
            countComposerAsArtist = countComposerAsArtist,
            relationshipResolver = relationshipResolver,
        ).stageRefresh()
    }

    private inline fun <reified T> capturedUpdates(mock: Any): List<T> =
        mockingDetails(mock).invocations
            .asSequence()
            .filter { it.method.name == "update" }
            .flatMap { invocation ->
                when (val argument = invocation.arguments.firstOrNull()) {
                    is Iterable<*> -> argument.asSequence()
                    else           -> sequenceOf(argument)
                }
            }
            .filterIsInstance<T>()
            .toList()

    @Suppress("UNCHECKED_CAST")
    private suspend fun captureSongLinks(): List<LinkageSongAndArtist> {
        verify(relationshipManipulate).overrideArtistSongs(anyList())
        return capturedArgument(relationshipManipulate, "overrideArtistSongs")
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun captureAlbumArtists(): List<LinkageAlbumAndArtist> {
        verify(relationshipManipulate).overrideAlbumArtists(anyList())
        return capturedArgument(relationshipManipulate, "overrideAlbumArtists")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> capturedArgument(mock: Any, methodName: String): T =
        mockingDetails(mock).invocations
            .last { it.method.name == methodName }
            .arguments.first() as T

    private fun link(songId: Long, name: String, role: Int) =
        LinkageSongAndArtist(songId, name.hashCode().toLong(), role)

    private fun song(
        id: Long = 1,
        title: String = "Track",
        albumId: Long = 10,
        artistId: Long = 42,
        album: String? = "Album A",
        artist: String? = "Artist A",
        albumArtist: String? = "Artist A",
        year: Int = 2026,
        dateAdded: Long = 0,
        dateModified: Long = 0,
        composer: String? = null,
    ) = Song(
        id = id,
        title = title,
        trackNumber = 1,
        year = year,
        duration = 240_000,
        data = "/storage/emulated/0/Music/$title.mp3",
        dateAdded = dateAdded,
        dateModified = dateModified,
        albumId = albumId,
        albumName = album,
        artistId = artistId,
        artistName = artist,
        albumArtistName = albumArtist,
        composer = composer,
    )

    private class PrintingProgressConnection : ProgressConnection {
        fun report() {
            val stackTrace = Thread.currentThread().stackTrace
            val methods = stackTrace.filter { it.className.startsWith("player.phonograph") }
            onStart()
            onProcessUpdate("Test (${methods.last().methodName})")
        }

        override fun onStart() = println("[sync] start")

        override fun onStart(notificationId: Int) = println("[sync] start notification=$notificationId")

        override fun onProcessUpdate(message: String?) = println("[sync] ${message.orEmpty()}")

        override fun onProcessUpdate(current: Int, total: Int) =
            println("[sync] $current/$total")

        override fun onProcessUpdate(current: Int, total: Int, message: String?) =
            println("[sync] $current/$total ${message.orEmpty()}")

        override fun onCompleted() = println("[sync] completed")

        override fun onReset() = println("[sync] reset")
    }
}
