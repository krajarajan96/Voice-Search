package com.voicesearch.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voicesearch.RecognizerState
import com.voicesearch.VoiceCaptureController
import com.voicesearch.VoiceCaptureSheet
import com.voicesearch.VoiceFab
import com.voicesearch.VoiceInteractionMode
import com.voicesearch.VoicePresentation
import com.voicesearch.VoiceRecognizer
import com.voicesearch.VoiceSearchColors
import com.voicesearch.VoiceSearchConfig
import com.voicesearch.rememberVoiceRecognizer

/** Canned recognizer so the review/edit flow can be demoed without a microphone or speech engine. */
private class DemoRecognizer : VoiceRecognizer {
    private val _state = mutableStateOf<RecognizerState>(RecognizerState.Idle)
    override val state: State<RecognizerState> = _state
    override fun startListening() { _state.value = RecognizerState.Listening("What were my sales") }
    override fun stopListening() { _state.value = RecognizerState.Finished("What were my sales last week?") }
    override fun reset() { _state.value = RecognizerState.Idle }
}

@Composable
fun App() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0070BA))) {
        var lastQuery by remember { mutableStateOf<String?>(null) }
        var mode by remember { mutableStateOf(VoiceInteractionMode.HoldToTalk) }
        var presentation by remember { mutableStateOf(VoicePresentation.BottomSheet) }
        var demo by remember { mutableStateOf(false) }

        val config = remember(mode, presentation) {
            VoiceSearchConfig(
                interactionMode = mode,
                presentation = presentation,
                colors = VoiceSearchColors(
                    fabIdleContainer = Color(0xFFFFC439),
                    fabIdleContent = Color(0xFF2C2E2F),
                ),
            )
        }
        val real = rememberVoiceRecognizer(config)
        val demoRecognizer = remember { DemoRecognizer() }
        val controller = remember(config, demo) {
            VoiceCaptureController(if (demo) demoRecognizer else real, config) { lastQuery = it }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().safeContentPadding().padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Voice Search", style = MaterialTheme.typography.headlineMedium)
                    Text("Interaction", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceInteractionMode.entries.forEach {
                            FilterChip(selected = mode == it, onClick = { mode = it }, label = { Text(it.name) })
                        }
                    }
                    Text("Presentation", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoicePresentation.entries.forEach {
                            FilterChip(selected = presentation == it, onClick = { presentation = it }, label = { Text(it.name) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = demo, onCheckedChange = { demo = it })
                        Text("Demo recognizer (no microphone)")
                    }
                    Card {
                        Text(
                            lastQuery?.let { "You said: $it" } ?: "Nothing sent yet.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    // Inline presentation renders here, in the page's own layout.
                    if (presentation == VoicePresentation.Inline) {
                        VoiceCaptureSheet(controller, config)
                    }
                }
                VoiceFab(
                    controller = controller,
                    config = config,
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(24.dp),
                )
            }
        }
        if (presentation != VoicePresentation.Inline) VoiceCaptureSheet(controller, config)
    }
}
