package com.streamcentre.client.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val title: String,
    val infoHash: String,
    val magnetUrl: String,
    val size: Long,
    val seeders: Int,
    val indexer: String,
    val quality: String,
)

@Serializable
data class StreamResponse(
    val url: String,
    val magnetUrl: String,
    val contentId: String,
    val durationSeconds: Double,
)

@Serializable
data class HistoryItem(
    val id: Long,
    @SerialName("watched_at") val watchedAt: String,
    val type: String,
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
    val episode: TraktEpisode? = null,
) {
    val displayTitle: String get() = when (type) {
        "movie" -> movie?.title ?: ""
        "episode" -> show?.title?.let { showTitle ->
            episode?.let { ep -> "$showTitle S${ep.season.toString().padStart(2, '0')}E${ep.number.toString().padStart(2, '0')}" }
        } ?: show?.title ?: ""
        else -> ""
    }
    val tmdbId: Int get() = movie?.ids?.tmdb ?: show?.ids?.tmdb ?: 0
    val mediaType: String get() = if (type == "movie") "movie" else "show"
    val contentId: String get() = when (type) {
        "movie" -> "movie:${movie?.ids?.tmdb ?: 0}"
        "episode" -> "show:${show?.ids?.tmdb ?: 0}:s${episode?.season ?: 0}e${episode?.number ?: 0}"
        else -> ""
    }
    val searchQuery: String get() = when (type) {
        "movie" -> movie?.title ?: ""
        "episode" -> show?.title?.let { title ->
            episode?.let { ep -> "$title season ${ep.season}" } ?: title
        } ?: ""
        else -> ""
    }
}

@Serializable
data class TraktMovie(
    val title: String,
    val year: Int,
    val ids: MediaIds,
)

@Serializable
data class TraktShow(
    val title: String,
    val year: Int,
    val ids: MediaIds,
)

@Serializable
data class TraktEpisode(
    val season: Int,
    val number: Int,
    val title: String,
)

@Serializable
data class RecommendationItem(
    val title: String,
    val year: Int,
    val ids: MediaIds,
)

@Serializable
data class MediaIds(
    val trakt: Int = 0,
    val slug: String = "",
    val imdb: String = "",
    val tmdb: Int = 0,
)

@Serializable
data class ResumePosition(
    val position: Int,
    val duration: Int,
)

@Serializable
data class PosterResponse(
    val url: String,
)

@Serializable
data class Suggestion(
    val id: Int,
    val title: String,
    val year: String,
)
