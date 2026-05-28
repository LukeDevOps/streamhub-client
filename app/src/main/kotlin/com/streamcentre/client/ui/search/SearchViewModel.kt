package com.streamcentre.client.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.api.SearchResult
import com.streamcentre.client.api.StreamResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val api: ApiClient) : ViewModel() {

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isStartingStream = MutableStateFlow(false)
    val isStartingStream = _isStartingStream.asStateFlow()

    private val _streamResult = MutableStateFlow<StreamResponse?>(null)
    val streamResult = _streamResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched = _hasSearched.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String, category: Int = ApiClient.CATEGORY_MOVIES) {
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            _results.value = emptyList()
            try {
                _results.value = api.search(query, category)
                _hasSearched.value = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                _error.value = e.message ?: "Search failed"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearResults() {
        searchJob?.cancel()
        _results.value = emptyList()
        _error.value = null
        _isSearching.value = false
        _hasSearched.value = false
    }

    fun startStream(result: SearchResult) {
        viewModelScope.launch {
            _isStartingStream.value = true
            _error.value = null
            try {
                _streamResult.value = api.stream(listOf(result.magnetUrl))
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start stream"
                _isStartingStream.value = false
            }
        }
    }

    fun clearStream() {
        _streamResult.value = null
        _isStartingStream.value = false
    }

    companion object {
        fun factory(api: ApiClient) = viewModelFactory {
            initializer { SearchViewModel(api) }
        }
    }
}
