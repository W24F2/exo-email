package com.example.email_client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.email_client.ui.navigation.AppNavHost
import com.example.email_client.ui.theme.LiquidMailTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LiquidMailApp
        setContent {
            LiquidMailTheme {
                AppNavHost(app = app)
            }
        }
    }
}
