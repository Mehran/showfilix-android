package com.showfilix.tv.data

/** Catalog (public) endpoints. */
class CatalogRepository(private val api: ApiClient) {

    /** Home page: hero sections + rows. */
    suspend fun home(): HomeResponse = api.get("/media/home")

    suspend fun latest(): BrowseResponse = api.get("/media/latest")

    /**
     * Paginated browse. [sortBy] default "3" mirrors the website default;
     * [genre]/[country] are comma-separated slugs.
     */
    suspend fun browse(
        type: String,
        page: Int = 1,
        limit: Int = 48,
        sortBy: String? = "3",
        genre: String? = null,
        country: String? = null,
        dubbed: Boolean = false,
        subtitle: Boolean = false,
        yearFrom: Int? = null,
        yearTo: Int? = null
    ): BrowseResponse = api.get(
        "/media/browse",
        buildMap {
            put("type", type)
            put("page", page.toString())
            put("limit", limit.toString())
            sortBy?.let { put("sortBy", it) }
            genre?.takeIf { it.isNotBlank() }?.let { put("genre", it) }
            country?.takeIf { it.isNotBlank() }?.let { put("country", it) }
            if (dubbed) put("dub", "1")
            if (subtitle) put("sub", "1")
            yearFrom?.let { put("from", it.toString()) }
            yearTo?.let { put("to", it.toString()) }
        }
    )

    suspend fun search(query: String, type: String? = null, page: Int = 1, limit: Int = 24): SearchResponse =
        api.get(
            "/media/search",
            buildMap {
                put("q", query)
                put("page", page.toString())
                put("limit", limit.toString())
                type?.let { put("type", it) }
            }
        )

    suspend fun detail(type: String, externalId: String): MediaItem =
        api.get("/media/$type/$externalId")

    suspend fun similar(type: String, externalId: String): SimilarResponse =
        api.get("/media/$type/$externalId/similar")

    /** Playback links. Requires a logged-in subscription; access is reported in payload. */
    suspend fun links(type: String, externalId: String, siteKey: String? = null): LinksResponse =
        api.get(
            "/media/$type/$externalId/links",
            if (siteKey.isNullOrBlank()) emptyMap() else mapOf("siteKey" to siteKey)
        )

    /** Builds a same-origin proxy URL the ExoPlayer can stream. */
    fun streamProxy(externalUrl: String): String =
        "${ApiClient.BASE}/media/stream?url=" + java.net.URLEncoder.encode(externalUrl, "UTF-8")
}
