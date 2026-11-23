@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.voicesearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.AVFAudio.AVAudioEngine
import platform.Foundation.NSLocale
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.setActive
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private class IosVoiceRecognizer(private val config: VoiceSearchConfig) : VoiceRecognizer {

    private val _state = mutableStateOf<RecognizerState>(RecognizerState.Idle)
    override val state: State<RecognizerState> = _state

    // A null languageTag uses the device's current locale.
    private val speechRecognizer = config.languageTag
        ?.let { SFSpeechRecognizer(locale = NSLocale(it)) }
        ?: SFSpeechRecognizer()
    private val audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null

    /**
     * Bumped by every startListening()/stopListening() call and captured by each permission
     * callback below. SFSpeechRecognizer/AVAudioSession authorization is always asynchronous,
     * even once already granted, so a quick press-and-release of the hold-to-talk FAB can
     * complete before authorization resolves. Without this guard, beginRecording() would still
     * fire after the user let go, recording audio nobody is listening for a response to.
     */
    private var sessionId = 0

    override fun startListening() {
        val session = ++sessionId
        SFSpeechRecognizer.requestAuthorization { authStatus ->
            dispatch_async(dispatch_get_main_queue()) {
                if (session != sessionId) return@dispatch_async
                if (authStatus == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    requestMicPermissionAndBegin(session)
                } else {
                    _state.value = RecognizerState.Error(config.strings.errorSpeechPermissionDenied)
                }
            }
        }
    }

    private fun requestMicPermissionAndBegin(session: Int) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            dispatch_async(dispatch_get_main_queue()) {
                if (session != sessionId) return@dispatch_async
                if (granted) {
                    beginRecording()
                } else {
                    _state.value = RecognizerState.Error(config.strings.errorMicPermissionDenied)
                }
            }
        }
    }

    private fun beginRecording() {
        if (speechRecognizer.available.not()) {
            _state.value = RecognizerState.Error(config.strings.errorUnavailable)
            return
        }

        recognitionTask?.cancel()
        recognitionTask = null

        val audioSession = AVAudioSession.sharedInstance()
        val categorySet = audioSession.setCategory(
            category = AVAudioSessionCategoryRecord,
            withOptions = AVAudioSessionCategoryOptionDuckOthers,
            error = null,
        )
        val activated = categorySet && audioSession.setActive(true, error = null)
        if (!activated) {
            _state.value = RecognizerState.Error(config.strings.errorGeneric)
            return
        }

        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = config.partialResults
        if (config.preferOffline && speechRecognizer.supportsOnDeviceRecognition) {
            request.requiresOnDeviceRecognition = true
        }
        recognitionRequest = request

        val inputNode = audioEngine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = 1024u,
            format = recordingFormat,
        ) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }

        audioEngine.prepare()
        if (!audioEngine.startAndReturnError(null)) {
            _state.value = RecognizerState.Error(config.strings.errorGeneric)
            return
        }

        _state.value = RecognizerState.Listening("")

        recognitionTask = speechRecognizer.recognitionTaskWithRequest(request) { result, error ->
            dispatch_async(dispatch_get_main_queue()) {
                if (error != null) {
                    stopAudioEngine()
                    val current = _state.value
                    // The engine reports a benign "no speech" error if stopped with nothing said.
                    if (current is RecognizerState.Listening && current.partialTranscript.isBlank()) {
                        _state.value = RecognizerState.Finished("")
                    } else if (current !is RecognizerState.Finished) {
                        _state.value = RecognizerState.Error(config.strings.errorStoppedUnexpectedly)
                    }
                    return@dispatch_async
                }
                val transcript = result?.bestTranscription?.formattedString.orEmpty()
                if (result?.isFinal() == true) {
                    stopAudioEngine()
                    _state.value = RecognizerState.Finished(transcript)
                } else {
                    _state.value = RecognizerState.Listening(transcript)
                }
            }
        }
    }

    override fun stopListening() {
        sessionId++ // invalidate any in-flight permission callbacks from this session
        if (audioEngine.isRunning()) {
            recognitionRequest?.endAudio()
        }
        stopAudioEngine()
        when (val current = _state.value) {
            is RecognizerState.Listening -> _state.value = RecognizerState.Finished(current.partialTranscript)
            // Permission checks were still pending when the user let go, so recording never
            // started: resolve to "no speech" instead of leaving the sheet stuck on Listening.
            RecognizerState.Idle -> _state.value = RecognizerState.Finished("")
            else -> Unit
        }
    }

    private fun stopAudioEngine() {
        if (audioEngine.isRunning()) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }
        recognitionRequest = null
        AVAudioSession.sharedInstance().setActive(false, error = null)
    }

    override fun reset() {
        _state.value = RecognizerState.Idle
    }
}

@Composable
actual fun rememberVoiceRecognizer(config: VoiceSearchConfig): VoiceRecognizer {
    val recognizer = remember(config) { IosVoiceRecognizer(config) }
    DisposableEffect(Unit) {
        onDispose { recognizer.stopListening() }
    }
    return recognizer
}
