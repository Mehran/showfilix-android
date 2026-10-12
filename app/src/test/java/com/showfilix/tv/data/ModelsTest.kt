package com.showfilix.tv.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodes real payloads captured from the live showfilix API (saved under
 * app/src/test/resources) to prove the models match the wire format.
 */
class ModelsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    private fun read(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader().readText()

    @Test
    fun detailDecodes() {
        val m = json.decodeFromString<MediaItem>(read("detail.json"))
        assertEquals("tt0110912", m.externalId)
        assertEquals("movie", m.type)
        assertEquals("Pulp Fiction (1994)", m.titleEn)
        assertEquals(1994, m.year)
        assertTrue(m.plot!!.isNotBlank())
        assertTrue(m.genres.contains("درام"))
        assertNotNull(m.posterUrl)
        assertEquals(8.9, m.normalizedImdb!!, 0.001)
        assertTrue(m.availability!!.hasOnlinePlay)
        assertTrue(m.people.any { it.role == "director" })
    }

    @Test
    fun homeDecodes() {
        val h = json.decodeFromString<HomeResponse>(read("home.json"))
        val rows = h.sections + (h.latestUpdates?.groups ?: emptyList())
        assertTrue("home should contain rows", rows.isNotEmpty())
        val first = rows.first { it.items.isNotEmpty() }
        assertTrue(first.persianName.isNotBlank())
        assertTrue(first.items.first().externalId!!.startsWith("tt"))
    }

    @Test
    fun browseDecodesWithPagination() {
        val b = json.decodeFromString<BrowseResponse>(read("browse.json"))
        assertEquals(48, b.items.size)
        val meta = checkNotNull(b.meta)
        assertEquals(1, meta.page)
        assertTrue(meta.total > 1000)
        assertTrue(meta.totalPages > 1)
    }

    @Test
    fun searchDecodes() {
        val s = json.decodeFromString<SearchResponse>(read("search.json"))
        assertEquals("matrix", s.query)
        assertTrue(s.items.isNotEmpty())
        assertTrue(s.items.all { it.displayTitle.isNotBlank() })
    }

    @Test
    fun linksDecodeWhenLoggedOut() {
        val l = json.decodeFromString<LinksResponse>(read("links.json"))
        assertEquals("tt0110912", l.externalId)
        assertTrue(l.playLinks.isEmpty())
        val access = checkNotNull(l.access)
        assertFalse(access.hasAccess)
    }

    /**
     * The reason playback failed: streams are in `downloadGroups[].items[].streamUrl`,
     * not in `playLinks`. Guards the regression.
     */
    @Test
    fun authenticatedMovieLinksExposeStreamUrl() {
        val l = json.decodeFromString<LinksResponse>(read("links_auth_movie.json"))
        assertTrue(checkNotNull(l.access).hasAccess)
        assertTrue(l.playLinks.isEmpty())
        assertEquals(1, l.downloadGroups.size)
        assertEquals("BluRay 1080p", l.downloadGroups[0].items[0].quality)
        assertEquals("https://cdn.example.test/a/movie-1080p.mkv", l.bestStreamUrl())
    }

    @Test
    fun authenticatedSeriesLinksPickEpisode() {
        val l = json.decodeFromString<LinksResponse>(read("links_auth_series.json"))
        assertTrue(checkNotNull(l.access).hasAccess)
        assertTrue(l.playLinks.isEmpty())
        assertEquals(2, l.allEpisodes.size)
        // first episode falls back to its mirror link when the quality has no streamUrl
        assertEquals("https://cdn.example.test/s1e1-mirror1.mkv", l.bestStreamUrl())
        assertEquals("https://cdn.example.test/s1e2.mkv", l.bestStreamUrl(season = 1, episode = 2))
    }
}
