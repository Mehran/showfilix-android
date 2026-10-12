package com.showfilix.tv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

/* ------------------------------- links --------------------------------- */

@Serializable
data class LinksResponse(
    val type: String? = null,
    val externalId: String? = null,
    val siteKey: String? = null,
    val playLinks: List<PlayLink> = emptyList(),
    val downloadGroups: List<JsonObjectLike> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val seriesDownloadGroups: List<JsonObjectLike> = emptyList(),
    val access: Access? = null
)

@Serializable
data class PlayLink(
    val kind: String? = null,
    val label: String? = null,
    val quality: String? = null,
    val url: String? = null,
    val link: String? = null,
    val source: String? = null,
    val language: String? = null,
    val dub: String? = null,
    val episode: Int? = null,
    val season: Int? = null
) {
    /** The playable URL, whichever field carries it. */
    val playUrl: String? get() = url ?: link ?: source
}

@Serializable
data class Season(
    val season: Int? = null,
    val title: String? = null,
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class Episode(
    val episode: Int? = null,
    val season: Int? = null,
    val title: String? = null,
    val durationMinutes: Int? = null
)

@Serializable
data class Access(
    val hasAccess: Boolean = false,
    val isExempt: Boolean = false,
    val subscription: JsonObjectLike? = null,
    val subscriptionsRequired: Boolean = false
)

/** Opaque payload we don't need to interpret yet; kept as a passthrough map. */
@Serializable
data class JsonObjectLike(
    val title: String? = null,
    val label: String? = null,
    val quality: String? = null,
    val items: List<PlayLink> = emptyList()
)
