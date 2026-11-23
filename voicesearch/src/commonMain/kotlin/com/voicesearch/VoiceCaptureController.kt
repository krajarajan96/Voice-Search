package com.voicesearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/** Everything the voice bottom sheet needs to render, in one place. */
sealed interface VoiceSheetState {
    data object Hidden : VoiceSheetState
    data class Listening(val partialTranscript: String) : VoiceSheetState
    data class Review(val transcript: String) : VoiceSheetState
    data class Editing(val transcript: String) : VoiceSheetState
    data class Error(val message: String) : VoiceSheetState
}

/**
 * Orchestrates the press-and-hold-to-talk flow: Listening -> Review (Cancel/Edit/Send).
 * Owns no platform APIs; capture comes from [VoiceRecognizer], and a confirmed transcript is
 * handed to [onQueryReady] — the controller has no opinion on what happens with it next.
 */
class VoiceCaptureController(
    private val recognizer: VoiceRecognizer,
    private val config: VoiceSearchConfig = VoiceSearchConfig(),
    private val onQueryReady: (String) -> Unit,
) {
    var sheetState: VoiceSheetState by mutableStateOf(VoiceSheetState.Hidden)
        private set

    val isListening: Boolean get() = sheetState is VoiceSheetState.Listening

    fun onFabPressStart() {
        sheetState = VoiceSheetState.Listening(partialTranscript = "")
        recognizer.startListening()
    }

    fun onFabPressEnd() = onStopListening()

    /** Tap-to-toggle: starts listening, or stops if already listening. */
    fun onFabTap() {
        if (isListening) onStopListening() else onFabPressStart()
    }

    fun onStopListening() {
        recognizer.stopListening()
    }

    fun onCancel() {
        recognizer.stopListening()
        recognizer.reset()
        sheetState = VoiceSheetState.Hidden
    }

    fun onEdit() {
        val current = sheetState
        if (current is VoiceSheetState.Review && config.allowEdit) {
            sheetState = VoiceSheetState.Editing(current.transcript)
        }
    }

    fun onEditedTranscriptChanged(newTranscript: String) {
        val current = sheetState
        if (current is VoiceSheetState.Editing) sheetState = current.copy(transcript = newTranscript)
    }

    fun onConfirmEdit() {
        val current = sheetState
        if (current is VoiceSheetState.Editing) sheetState = VoiceSheetState.Review(current.transcript)
    }

    fun onSend() {
        val transcript = when (val current = sheetState) {
            is VoiceSheetState.Review -> current.transcript
            is VoiceSheetState.Editing -> current.transcript
            else -> return
        }
        recognizer.reset()
        sheetState = VoiceSheetState.Hidden
        onQueryReady(transcript)
    }

    /** Stops any active recording without touching the sheet state. */
    fun onAppBackgrounded() {
        if (sheetState is VoiceSheetState.Listening) recognizer.stopListening()
    }

    fun onDismissError() {
        recognizer.reset()
        sheetState = VoiceSheetState.Hidden
    }

    fun onRetry() {
        recognizer.reset()
        onFabPressStart()
    }

    /** Call once per composition to keep [sheetState] mirroring [recognizer]'s state. */
    @Composable
    fun ObserveRecognizer() {
        val recognizerState = recognizer.state.value
        LaunchedEffect(recognizerState) { onRecognizerStateChanged(recognizerState) }
    }

    /** Testable without a Compose runtime, unlike [ObserveRecognizer]. */
    internal fun onRecognizerStateChanged(recognizerState: RecognizerState) {
        when (recognizerState) {
            is RecognizerState.Listening -> {
                if (sheetState !is VoiceSheetState.Hidden) {
                    sheetState = VoiceSheetState.Listening(recognizerState.partialTranscript)
                }
            }
            is RecognizerState.Finished -> {
                if (config.autoSend && recognizerState.transcript.isNotBlank() && sheetState !is VoiceSheetState.Hidden) {
                    sheetState = VoiceSheetState.Review(recognizerState.transcript)
                    onSend()
                } else {
                    sheetState = VoiceSheetState.Review(recognizerState.transcript)
                }
            }
            is RecognizerState.Error -> {
                // stopListening() can deliver onError asynchronously, after the user already
                // cancelled (onCancel hides the sheet synchronously). Don't resurrect the sheet.
                if (sheetState !is VoiceSheetState.Hidden) {
                    sheetState = VoiceSheetState.Error(recognizerState.message)
                }
            }
            RecognizerState.Idle -> Unit
        }
    }
}

/**
 * One-call setup: remembers a platform recognizer and a controller wired to [onResult].
 * Pair with [VoiceFab] and [VoiceCaptureSheet], or drive the controller from your own UI.
 */
@Composable
fun rememberVoiceCaptureController(
    config: VoiceSearchConfig = VoiceSearchConfig(),
    onResult: (String) -> Unit,
): VoiceCaptureController {
    val recognizer = rememberVoiceRecognizer(config)
    val latestOnResult = rememberUpdatedState(onResult)
    return remember(recognizer, config) {
        VoiceCaptureController(recognizer, config) { latestOnResult.value(it) }
    }
}
