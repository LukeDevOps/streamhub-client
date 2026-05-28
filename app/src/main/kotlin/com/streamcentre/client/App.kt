package com.streamcentre.client

import android.app.Application
import android.content.Context
import com.streamcentre.client.api.ApiClient

class App : Application() {

    val api: ApiClient by lazy {
        ApiClient(baseUrl = BASE_URL)
    }

    companion object {
        // TODO: point this at your server IP
        const val BASE_URL = "http://192.168.1.1:8080"
    }
}

val Context.app: App get() = applicationContext as App
