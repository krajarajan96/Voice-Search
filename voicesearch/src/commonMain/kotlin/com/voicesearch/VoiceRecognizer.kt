package com.voicesearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Low-level state reported by the platform speech engine. Intentionally narrow: it only knows
 * about capturing audio and turning it into text. Review/edit/send lives in [VoiceCaptureController].
 */
sealed interface RecognizerState {
    data object Idle : RecognizerState
    data class Listening(val partialTranscript: String) : RecognizerState
    data class Finished(val transcript: String) : RecognizerState
    data class Error(val message: String) : RecognizerState
}

/**
 * Platform voice capture. Android uses `android.speech.SpeechRecognizer`; iOS uses
 * `SFSpeechRecognizer` + `AVAudioEngine`. Implement it yourself to plug in another engine.
 */
interface VoiceRecognizer {
    val state: State<RecognizerState>
    fun startListening()
    fun stopListening()

    /** Clears any Finished/Error state back to Idle, without touching an active recording. */
    fun reset()
}

/** Creates (and remembers) the platform recognizer, wiring up permission requests. */
@Composable
expect fun rememberVoiceRecognizer(config: VoiceSearchConfig = VoiceSearchConfig()): VoiceRecognizer
