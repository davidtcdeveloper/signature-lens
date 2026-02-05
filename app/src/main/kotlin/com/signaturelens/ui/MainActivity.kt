package com.signaturelens.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.signaturelens.ui.screen.PreviewScreen
import com.signaturelens.ui.theme.SignatureLensTheme

/**
 * Main activity for SignatureLens.
 * Per spec §11.1: Simple ComponentActivity with Compose content.
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SignatureLensApp()
        }
    }
}

/**
 * Root composable for SignatureLens app.
 * Per spec §11.1: Applies theme and shows PreviewScreen.
 */
@androidx.compose.runtime.Composable
fun SignatureLensApp() {
    SignatureLensTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            PreviewScreen()
        }
    }
}
