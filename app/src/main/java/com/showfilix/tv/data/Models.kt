package com.showfilix.tv.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A catalog entry (movie or series). The list endpoints return a subset of these
 * fields; the details endpoint returns the full object. All optional fields are
 * defaulted so the same model can decode every catalog payload.
 */
@Serializable
data class MediaItem(
    val id: String? = null,
    val type: String? = null,              // "movie" | "series"
    val siteKey: String? = null,
    val providers: List<String> = emptyList(),
    val externalId: String? = null,        // IMDb id, e.g. tt0110912
    val slug: String? = null,
    val title: String = "",
    val titleEn: String? = null,
    val year: Int? = null,
    val posterUrl: String? = null,
    val trailerUrl: String? = null,
    val quality: String? = null,
    val ageRating: String? = null,
    val durationMinutes: Int? = null,
    val plot: String? = null,
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val people: List<Person> = emptyList(),
    val network: String? = null,
    val airDay: String? = null,
    val airingStatus: String? = null,
    val ratings: Ratings? = null,
    val availability: Availability? = null,
    val imdbRating: Double? = null         // present on list items
) {
    /** Best display title: English when the Persian title is empty. */
    val displayTitle: String get() = title.ifBlank { titleEn ?: externalId ?: "" }
    val isSeries: Boolean get() = type == "series"
    val normalizedImdb: Double? get() = imdbRating ?: ratings?.imdb
}

@Serializable
data class Person(
    val personId: String? = null,
    val name: String = "",
    val role: String? = null,              // "director" | "actor" | ...
    val avatarUrl: String? = null
)

@Serializable
data class Ratings(
    val imdb: Double? = null,
    val imdbVotes: Long? = null,
    val userScore: Double? = null,
    val criticsScore: Double? = null
)

@Serializable
data class Availability(
    val hasSubtitle: Boolean = false,
    val hasDubbed: Boolean = false,
    val hasOnlinePlay: Boolean = false,
    val hasBilingual: Boolean = false
)

/* ------------------------------- home ---------------------------------- */

@Serializable
data class HomeResponse(
    val latestUpdates: LatestUpdates? = null,
    val sections: List<HomeSection> = emptyList()
)

@Serializable
data class LatestUpdates(
    val title: String = "",
    val groups: List<HomeSection> = emptyList()
)

@Serializable
data class HomeSection(
    val pageKey: String? = null,
    val persianName: String = "",
    val mediaType: String? = null,
    val items: List<MediaItem> = emptyList()
)

/* ------------------------------ browse --------------------------------- */

@Serializable
data class BrowseResponse(
    val items: List<MediaItem> = emptyList(),
    val meta: PageMeta? = null
)

@Serializable
data class PageMeta(
    val page: Int = 1,
    val pageSize: Int = 48,
    val total: Int = 0,
    val totalPages: Int = 1
)

/* ------------------------------ search --------------------------------- */

@Serializable
data class SearchResponse(
    val query: String? = null,
    val items: List<MediaItem> = emptyList()
)

/* ------------------------------ similar -------------------------------- */

@Serializable
data class SimilarResponse(
    val items: List<MediaItem> = emptyList()
)

/* ------------------------------- links ---------------------------------

   Playback links live in `downloadGroups` (movies) and `seriesDownloadGroups`
   (series) — `playLinks` is usually empty. Each quality entry carries a
   `streamUrl`; the nested `links` array holds mirrors of the same file.

   Movie:   downloadGroups[].items[].streamUrl
   Series:  seriesDownloadGroups[].seasons[].episodes[].qualities[].streamUrl
*/

@Serializable
data class LinksResponse(
    val type: String? = null,
    val externalId: String? = null,
    val siteKey: String? = null,
    val playLinks: List<StreamLink> = emptyList(),
    val downloadGroups: List<LinkGroup> = emptyList(),
    val seasons: List<SeasonGroup> = emptyList(),
    val seriesDownloadGroups: List<LinkGroup> = emptyList(),
    val access: Access? = null
) {
    /** Every episode across all series groups, in server order. */
    val allEpisodes: List<EpisodeItem>
        get() = seriesDownloadGroups.flatMap { it.seasons }.flatMap { it.episodes }

    /**
     * Best playable URL: explicit play links first, then movie quality groups,
     * then the first series episode. Null when the server returned no links.
     */
    fun bestStreamUrl(season: Int? = null, episode: Int? = null): String? {
        playLinks.firstNotNullOfOrNull { it.playUrl }?.let { return it }
        downloadGroups.asSequence()
            .flatMap { it.items.asSequence() }
            .firstNotNullOfOrNull { it.bestUrl }
            ?.let { return it }
        val episodes = allEpisodes
        val chosen = episodes.firstOrNull { ep ->
            (season == null || ep.season == season) && (episode == null || ep.episode == episode)
        } ?: episodes.firstOrNull()
        return chosen?.qualities?.firstNotNullOfOrNull { it.bestUrl }
    }
}

@Serializable
data class StreamLink(
    val label: String? = null,
    val url: String? = null,
    val kind: String? = null,
    val mirror: Int? = null,
    val streamUrl: String? = null
) {
    val playUrl: String? get() = streamUrl ?: url
}

@Serializable
data class QualityItem(
    val quality: String? = null,
    val size: String? = null,
    val streamUrl: String? = null,
    val links: List<StreamLink> = emptyList()
) {
    val bestUrl: String?
        get() = streamUrl
            ?: links.firstNotNullOfOrNull { it.streamUrl }
            ?: links.firstNotNullOfOrNull { it.url }
}

@Serializable
data class EpisodeItem(
    val season: Int? = null,
    val episode: Int? = null,
    val title: String? = null,
    val qualities: List<QualityItem> = emptyList()
)

@Serializable
data class SeasonGroup(
    val season: Int? = null,
    val title: String? = null,
    val episodes: List<EpisodeItem> = emptyList()
)

@Serializable
data class LinkGroup(
    val title: String? = null,
    val items: List<QualityItem> = emptyList(),
    val seasons: List<SeasonGroup> = emptyList()
)

@Serializable
data class Access(
    val hasAccess: Boolean = false,
    val isExempt: Boolean = false,
    val subscription: JsonElement? = null,
    val subscriptionsRequired: Boolean = false
)
