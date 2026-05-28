package com.streamcentre.client.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.streamcentre.client.api.ApiClient
import kotlinx.coroutines.launch

class PlayerViewModel(private val api: ApiClient) : ViewModel() {

    private var infoHash = ""
    private var contentId = ""

    fun initialize(contentId: String, infoHash: String) {
        this.contentId = contentId
        this.infoHash = infoHash
    }

    suspend fun getResumePosition(): Int = try {
        api.getResume(infoHash).position
    } catch (e: Exception) {
        0
    }

    fun savePosition(positionMs: Long, durationMs: Long) {
        if (infoHash.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                api.saveResume(
                    infoHash = infoHash,
                    position = (positionMs / 1000).toInt(),
                    duration = (durationMs / 1000).toInt(),
                )
            }
        }
    }

    fun scrobble(action: String, progress: Double) {
        if (contentId.isEmpty()) return
        viewModelScope.launch {
            runCatching { api.scrobble(contentId, progress, action) }
        }
    }

    companion object {
        fun factory(api: ApiClient) = viewModelFactory {
            initializer { PlayerViewModel(api) }
        }
    }
}
