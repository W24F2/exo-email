package com.example.exoemail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.exoemail.ui.navigation.AppNavHost
import com.example.exoemail.ui.theme.LiquidMailTheme

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
