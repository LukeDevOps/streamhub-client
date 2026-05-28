package com.streamcentre.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.streamcentre.client.ui.AppNav
import com.streamcentre.client.ui.theme.StreamcentreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamcentreTheme {
                AppNav(api = app.api)
            }
        }
    }
}
