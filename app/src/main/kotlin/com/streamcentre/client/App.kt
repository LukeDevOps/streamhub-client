package com.streamcentre.client

import android.app.Application
import android.content.Context
import com.streamcentre.client.api.ApiClient

class App : Application() {

    val api: ApiClient by lazy {
        ApiClient(baseUrl = BuildConfig.BASE_URL)
    }
}

val Context.app: App get() = applicationContext as App
