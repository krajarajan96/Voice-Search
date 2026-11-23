package com.voicesearch.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voicesearch.VoiceCaptureSheet
import com.voicesearch.VoiceFab
import com.voicesearch.VoiceSearchColors
import com.voicesearch.VoiceSearchConfig
import com.voicesearch.rememberVoiceCaptureController

@Composable
fun App() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0070BA))) {
        var lastQuery by remember { mutableStateOf<String?>(null) }
        val config = remember {
            VoiceSearchConfig(
                colors = VoiceSearchColors(
                    fabIdleContainer = Color(0xFFFFC439),
                    fabIdleContent = Color(0xFF2C2E2F),
                ),
            )
        }
        val controller = rememberVoiceCaptureController(config) { lastQuery = it }

        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().safeContentPadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Voice Search", style = MaterialTheme.typography.headlineMedium)
                    Text("Press and hold the mic, speak, then review and send.")
                    Card {
                        Text(
                            lastQuery?.let { "You said: $it" } ?: "Nothing sent yet.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                VoiceFab(
                    isListening = controller.isListening,
                    onPressStart = controller::onFabPressStart,
                    onPressEnd = controller::onFabPressEnd,
                    config = config,
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp),
                )
            }
        }
        VoiceCaptureSheet(controller, config)
    }
}
