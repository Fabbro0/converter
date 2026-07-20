package com.megaconverter.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.megaconverter.app.converter.ConversionEngine
import com.megaconverter.app.ui.ConverterScreen
import com.megaconverter.app.ui.theme.MegaConverterTheme

class MainActivity : ComponentActivity() {

    private val engine = ConversionEngine.default()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialUri = extractUriFromIntent(intent)

        setContent {
            MegaConverterTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ConverterScreen(engine = engine, initialUri = initialUri)
                }
            }
        }
    }

    private fun extractUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.parcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
    }
}

private fun <T : Parcelable> Intent.parcelableExtraCompat(name: String, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name)
    }
