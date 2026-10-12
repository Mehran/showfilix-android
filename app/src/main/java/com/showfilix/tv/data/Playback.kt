package com.showfilix.tv.data

/** One selectable quality/stream. */
data class QualityOption(
    val label: String,
    val size: String?,
    val url: String
) {
    /** Label with the file size appended, as shown in the picker. */
    val displayLabel: String get() = if (size.isNullOrBlank()) label else "$label · $size"
}

data class EpisodeOption(
    val season: Int,
    val episode: Int,
    val title: String?,
    val qualities: List<QualityOption>
)

data class SeasonOption(
    val number: Int,
    val title: String?,
    val episodes: List<EpisodeOption>
)

/** An audio/dub variant ("نسخه اصلی", "دوبله فارسی", ...). */
data class VersionOption(
    val title: String,
    val seasons: List<SeasonOption>
)

/**
 * Everything the picker needs, from one `/links` response.
 *
 * Movies expose [movieQualities]; series expose [versions] (each with seasons,
 * episodes and their qualities).
 */
data class PlaybackPlan(
    val versions: List<VersionOption> = emptyList(),
    val movieQualities: List<QualityOption> = emptyList()
) {
    val isSeries: Boolean get() = versions.isNotEmpty()
    val isEmpty: Boolean get() = versions.isEmpty() && movieQualities.isEmpty()
}

/**
 * `playLinks` is usually empty; streams live in `downloadGroups` (movies) and
 * `seriesDownloadGroups` (series, grouped by dub/original with seasons inside).
 */
fun LinksResponse.toPlaybackPlan(): PlaybackPlan {
    val versions = seriesDownloadGroups.mapNotNull { group ->
        val seasons = group.seasons
            .sortedBy { it.season ?: 0 }
            .map { season ->
                SeasonOption(
                    number = season.season ?: 1,
                    title = season.title,
                    episodes = season.episodes
                        .sortedBy { it.episode ?: 0 }
                        .map { ep ->
                            EpisodeOption(
                                season = ep.season ?: season.season ?: 1,
                                episode = ep.episode ?: 0,
                                title = ep.title,
                                qualities = ep.qualities.mapNotNull { it.toOption() }
                            )
                        }
                )
            }
        val playable = seasons.any { s -> s.episodes.any { it.qualities.isNotEmpty() } }
        if (!playable) null else VersionOption(group.title ?: "نسخه", seasons)
    }

    val multipleGroups = downloadGroups.size > 1
    val movieQualities = downloadGroups.flatMap { group ->
        group.items.mapNotNull { it.toOption(group.title.takeIf { multipleGroups }) }
    }

    // Last resort: some titles may expose streams only as explicit play links.
    val fallback = if (versions.isEmpty() && movieQualities.isEmpty()) {
        playLinks.mapNotNull { link ->
            val url = link.playUrl ?: return@mapNotNull null
            QualityOption(link.label ?: "پخش آنلاین", null, url)
        }
    } else {
        emptyList()
    }

    return PlaybackPlan(versions, movieQualities + fallback)
}

private fun QualityItem.toOption(groupTitle: String? = null): QualityOption? {
    val url = bestUrl ?: return null
    val quality = quality ?: "کیفیت نامشخص"
    return QualityOption(
        label = if (groupTitle.isNullOrBlank()) quality else "$quality · $groupTitle",
        size = size,
        url = url
    )
}
