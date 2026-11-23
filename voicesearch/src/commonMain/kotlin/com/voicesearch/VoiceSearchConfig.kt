package com.voicesearch

import androidx.compose.ui.graphics.Color

/** User-facing copy. Every string the library shows or reports as an error is overridable. */
data class VoiceSearchStrings(
    val listening: String = "Listening…",
    val stopListening: String = "Stop listening",
    val heardTitle: String = "Here's what I heard",
    val noSpeechCaptured: String = "No speech captured",
    val holdAndTryAgain: String = "Hold the mic button and try again.",
    val editTranscript: String = "Edit transcript",
    val edit: String = "Edit",
    val send: String = "Send",
    val done: String = "Done",
    val cancel: String = "Cancel",
    val dismiss: String = "Dismiss",
    val tryAgain: String = "Try again",
    val fabHoldToTalk: String = "Hold to talk",
    val fabListening: String = "Listening",
    val fabTapToTalk: String = "Tap to talk",
    val fabTapToStop: String = "Tap to stop",
    // Errors reported by the platform recognizers.
    val errorNoMatch: String = "Didn't catch that, please try again!",
    val errorSpeechTimeout: String = "No speech detected",
    val errorMicPermissionRequired: String = "Microphone permission is required",
    val errorMicPermissionDenied: String = "Microphone permission denied",
    val errorSpeechPermissionDenied: String = "Speech recognition permission denied",
    val errorNetwork: String = "Network error — check your connection",
    val errorUnavailable: String = "Speech recognition isn't available on this device",
    val errorStoppedUnexpectedly: String = "Voice recognition stopped unexpectedly",
    val errorGeneric: String = "Something went wrong with voice recognition",
) {
    companion object {
        fun defaults(): VoiceSearchStrings = VoiceSearchStrings()
    }
}

/**
 * Colors for the hold-to-talk FAB and the capture sheet. [Color.Unspecified] falls back to the
 * host's Material 3 theme.
 */
data class VoiceSearchColors(
    val fabIdleContainer: Color = Color.Unspecified,
    val fabIdleContent: Color = Color.Unspecified,
    val fabListeningContainer: Color = Color.Unspecified,
    val fabListeningContent: Color = Color.Unspecified,
    val sheetContainer: Color = Color.Unspecified,
    /** Tint of the waveform/error icons on the sheet. */
    val accent: Color = Color.Unspecified,
) {
    companion object {
        fun defaults(): VoiceSearchColors = VoiceSearchColors()
    }
}

/** How the mic button behaves. */
enum class VoiceInteractionMode {
    /** Record while the button is held; releasing stops. */
    HoldToTalk,

    /** Tap once to start recording, tap again to stop. */
    TapToToggle,
}

/** Where the capture UI (listening, review, edit, error) is shown. */
enum class VoicePresentation {
    /** Modal bottom sheet. */
    BottomSheet,

    /** Full-screen dialog. */
    FullScreen,

    /** In place, wherever [VoiceCaptureSheet] is called (or use [VoiceCaptureContent] directly). */
    Inline,
}

/**
 * Everything a host can customize about voice search.
 *
 * @property languageTag BCP-47 tag (e.g. "en-US"); null uses the device locale.
 * @property partialResults Stream interim transcripts while the user is still speaking.
 * @property preferOffline Ask the engine to stay on-device when it can (best effort; falls back
 * to the network engine if no offline model is installed).
 * @property allowEdit Offer an "Edit" step between review and send.
 * @property interactionMode Hold-to-talk or tap-to-toggle behavior for [VoiceFab].
 * @property presentation Container for the capture UI.
 * @property autoSend Skip the review step and deliver a non-blank transcript immediately.
 */
data class VoiceSearchConfig(
    val strings: VoiceSearchStrings = VoiceSearchStrings.defaults(),
    val colors: VoiceSearchColors = VoiceSearchColors.defaults(),
    val languageTag: String? = null,
    val partialResults: Boolean = true,
    val preferOffline: Boolean = false,
    val allowEdit: Boolean = true,
    val autoSend: Boolean = false,
    val interactionMode: VoiceInteractionMode = VoiceInteractionMode.HoldToTalk,
    val presentation: VoicePresentation = VoicePresentation.BottomSheet,
)
