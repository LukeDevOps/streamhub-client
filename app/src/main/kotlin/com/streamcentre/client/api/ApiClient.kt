package com.streamcentre.client.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiClient(val baseUrl: String) {

    val http = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        engine {
            connectTimeout = 10_000
            socketTimeout = 30_000
        }
    }

    suspend fun suggest(query: String, category: Int = CATEGORY_MOVIES): List<Suggestion> =
        http.get("$baseUrl/suggest") {
            parameter("query", query)
            parameter("type", if (category == CATEGORY_TV) "show" else "movie")
        }.body()

    suspend fun search(query: String, category: Int = CATEGORY_MOVIES): List<SearchResult> =
        http.get("$baseUrl/search") {
            parameter("query", query)
            parameter("category", category)
        }.body()

    suspend fun stream(magnetUrls: List<String>): StreamResponse {
        val response = http.post("$baseUrl/stream") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("magnetUrls" to magnetUrls))
        }
        if (!response.status.isSuccess()) {
            throw Exception("Content not available yet — try a different source")
        }
        return response.body()
    }

    suspend fun getHistory(limit: Int = 20): List<HistoryItem> =
        http.get("$baseUrl/trakt/history") {
            parameter("limit", limit)
        }.body()

    suspend fun getRecommendations(type: String = "movies", limit: Int = 30): List<RecommendationItem> =
        http.get("$baseUrl/trakt/recommendations") {
            parameter("type", type)
            parameter("limit", limit)
        }.body()

    fun posterImageUrl(tmdbId: Int, type: String): String =
        "$baseUrl/poster/img?tmdbId=$tmdbId&type=$type"

    suspend fun getAllResume(): Map<String, ResumePosition> =
        http.get("$baseUrl/resume").body()

    suspend fun getResume(infoHash: String): ResumePosition =
        http.get("$baseUrl/resume/$infoHash").body()

    suspend fun saveResume(infoHash: String, position: Int, duration: Int) {
        http.post("$baseUrl/resume/$infoHash") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("position" to position, "duration" to duration))
        }
    }

    suspend fun scrobble(contentId: String, progress: Double, action: String) {
        http.post("$baseUrl/trakt/scrobble") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("contentId" to contentId, "progress" to progress, "action" to action))
        }
    }

    companion object {
        const val CATEGORY_MOVIES = 2000
        const val CATEGORY_TV = 5000
    }
}
