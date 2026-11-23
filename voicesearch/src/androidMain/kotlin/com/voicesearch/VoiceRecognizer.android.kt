package com.voicesearch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private class AndroidVoiceRecognizer(
    private val context: Context,
    private val config: VoiceSearchConfig,
) : VoiceRecognizer {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = mutableStateOf<RecognizerState>(RecognizerState.Idle)
    override val state: State<RecognizerState> = _state

    /** Wired up from the composable below, since permission requests need an Activity. */
    var requestPermission: (() -> Unit)? = null
    var hasPermission: () -> Boolean = { false }

    override fun startListening() {
        if (!hasPermission()) {
            requestPermission?.invoke()
            return
        }
        beginRecognition()
    }

    private fun beginRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = RecognizerState.Error(config.strings.errorUnavailable)
            return
        }
        // Reuse one SpeechRecognizer for the recognizer's lifetime instead of destroying and
        // recreating it per tap — destroy() tears the bound recognition service down
        // asynchronously, so starting a fresh instance right after racing that teardown
        // reliably surfaced as an instant error on the very next press.
        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { created ->
            created.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = RecognizerState.Listening("")
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    _state.value = RecognizerState.Listening(text)
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    _state.value = RecognizerState.Finished(text)
                }

                override fun onError(error: Int) {
                    _state.value = RecognizerState.Error(describeError(error))
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
            })
            speechRecognizer = created
        }

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, config.partialResults)
            if (config.preferOffline) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            config.languageTag?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
        }
        recognizer.startListening(intent)
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    override fun reset() {
        _state.value = RecognizerState.Idle
    }

    fun reportError(message: String) {
        _state.value = RecognizerState.Error(message)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> config.strings.errorNoMatch
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> config.strings.errorSpeechTimeout
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> config.strings.errorMicPermissionRequired
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> config.strings.errorNetwork
        else -> config.strings.errorGeneric
    }
}

@Composable
actual fun rememberVoiceRecognizer(config: VoiceSearchConfig): VoiceRecognizer {
    val context = LocalContext.current
    val recognizer = remember(config) { AndroidVoiceRecognizer(context, config) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) {
            recognizer.startListening()
        } else {
            recognizer.reportError(config.strings.errorMicPermissionDenied)
        }
    }

    recognizer.hasPermission = { hasPermission }
    recognizer.requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    DisposableEffect(Unit) {
        onDispose { recognizer.destroy() }
    }

    return recognizer
}
