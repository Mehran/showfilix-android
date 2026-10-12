package com.showfilix.tv.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the picker sees every version, season, episode and quality. */
class PlaybackPlanTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    private fun read(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader().readText()

    private fun plan(name: String): PlaybackPlan =
        json.decodeFromString<LinksResponse>(read(name)).toPlaybackPlan()

    @Test
    fun moviePlanListsEveryQuality() {
        val p = plan("links_auth_movie.json")
        assertFalse(p.isSeries)
        assertFalse(p.isEmpty)
        assertEquals(1, p.movieQualities.size)
        val q = p.movieQualities[0]
        assertEquals("BluRay 1080p", q.label)
        assertEquals("2.3 GB", q.size)
        assertEquals("https://cdn.example.test/a/movie-1080p.mkv", q.url)
        assertEquals("BluRay 1080p · 2.3 GB", q.displayLabel)
    }

    @Test
    fun seriesPlanExposesVersionsSeasonsEpisodesAndQualities() {
        val p = plan("links_auth_series_multigroup.json")
        assertTrue(p.isSeries)
        assertEquals(2, p.versions.size)
        assertEquals("دانلود دوبله فارسی", p.versions[0].title)
        assertEquals("دانلود نسخه اصلی", p.versions[1].title)

        val original = p.versions[1]
        assertEquals(2, original.seasons.size)
        assertEquals(1, original.seasons[0].number)
        assertEquals(2, original.seasons[1].number)
        assertEquals(2, original.seasons[0].episodes.size)

        val ep = original.seasons[0].episodes[0]
        assertEquals(1, ep.season)
        assertEquals(1, ep.episode)
        assertEquals("قسمت 1", ep.title)
        assertEquals(listOf("WEB-DL 1080p", "WEB-DL 720p"), ep.qualities.map { it.label })
        assertEquals("https://cdn.example.test/orig-s1e1-1080.mkv", ep.qualities[0].url)
        assertEquals("WEB-DL 1080p · 1.0 GB", ep.qualities[0].displayLabel)
    }

    @Test
    fun seasonAndEpisodeIndicesAddressTheRightStream() {
        val p = plan("links_auth_series_multigroup.json")
        val original = p.versions[1]
        val s2e1 = original.seasons[1].episodes[0]
        assertEquals(2, s2e1.season)
        assertEquals(1, s2e1.episode)
        assertEquals("https://cdn.example.test/orig-s2e1-1080.mkv", s2e1.qualities[0].url)
    }

    @Test
    fun singleGroupSeriesYieldsOneVersion() {
        val p = plan("links_auth_series.json")
        assertTrue(p.isSeries)
        assertEquals(1, p.versions.size)
        assertEquals(2, p.versions[0].seasons[0].episodes.size)
        assertEquals("https://cdn.example.test/s1e1-mirror1.mkv",
            p.versions[0].seasons[0].episodes[0].qualities[0].url)
    }
}
