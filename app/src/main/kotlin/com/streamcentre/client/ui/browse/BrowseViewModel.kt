package com.streamcentre.client.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.api.HistoryItem
import com.streamcentre.client.api.RecommendationItem
import com.streamcentre.client.api.ResumePosition
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowseViewModel(private val api: ApiClient) : ViewModel() {

    private val _recommendations = MutableStateFlow<List<RecommendationItem>>(emptyList())
    val recommendations = _recommendations.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    private val _resumePositions = MutableStateFlow<Map<String, ResumePosition>>(emptyMap())
    val resumePositions = _resumePositions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val recDeferred = async { api.getRecommendations("movies", 30) }
                val histDeferred = async { api.getHistory(20) }
                val resumeDeferred = async { runCatching { api.getAllResume() }.getOrDefault(emptyMap()) }
                _recommendations.value = recDeferred.await()
                _history.value = histDeferred.await()
                    .filter { it.tmdbId > 0 }
                    .distinctBy { it.tmdbId }
                _resumePositions.value = resumeDeferred.await()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun factory(api: ApiClient) = viewModelFactory {
            initializer { BrowseViewModel(api) }
        }
    }
}
