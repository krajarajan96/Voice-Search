package com.voicesearch

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeVoiceRecognizer : VoiceRecognizer {
    private val _state = mutableStateOf<RecognizerState>(RecognizerState.Idle)
    override val state: State<RecognizerState> = _state

    var startListeningCalls = 0
        private set
    var stopListeningCalls = 0
        private set
    var resetCalls = 0
        private set

    override fun startListening() {
        startListeningCalls++
    }

    override fun stopListening() {
        stopListeningCalls++
    }

    override fun reset() {
        resetCalls++
        _state.value = RecognizerState.Idle
    }

    fun emit(newState: RecognizerState) {
        _state.value = newState
    }
}

private fun newController(
    recognizer: FakeVoiceRecognizer = FakeVoiceRecognizer(),
    config: VoiceSearchConfig = VoiceSearchConfig(),
    onQueryReady: (String) -> Unit = {},
): Pair<VoiceCaptureController, FakeVoiceRecognizer> {
    val controller = VoiceCaptureController(recognizer, config, onQueryReady)
    return controller to recognizer
}

class VoiceCaptureControllerTest {

    @Test
    fun initialState_isHidden() {
        val (controller, _) = newController()
        assertEquals(VoiceSheetState.Hidden, controller.sheetState)
    }

    @Test
    fun onFabPressStart_movesToListening_andStartsRecognizer() {
        val (controller, recognizer) = newController()

        controller.onFabPressStart()

        assertEquals(VoiceSheetState.Listening(""), controller.sheetState)
        assertEquals(1, recognizer.startListeningCalls)
    }

    @Test
    fun recognizerPartialResults_updateListeningTranscript_whileSheetIsOpen() {
        val (controller, _) = newController()
        controller.onFabPressStart()

        controller.onRecognizerStateChanged(RecognizerState.Listening("hello"))

        assertEquals(VoiceSheetState.Listening("hello"), controller.sheetState)
    }

    @Test
    fun onFabPressEnd_stopsListening() {
        val (controller, recognizer) = newController()
        controller.onFabPressStart()

        controller.onFabPressEnd()

        assertEquals(1, recognizer.stopListeningCalls)
    }

    @Test
    fun recognizerFinished_movesToReview() {
        val (controller, _) = newController()
        controller.onFabPressStart()
        controller.onFabPressEnd()

        controller.onRecognizerStateChanged(RecognizerState.Finished("add a note"))

        assertEquals(VoiceSheetState.Review("add a note"), controller.sheetState)
    }

    @Test
    fun onSend_fromReview_hidesSheet_andHandsOffTheQuery() {
        var readyQuery: String? = null
        val (controller, recognizer) = newController(onQueryReady = { readyQuery = it })
        controller.onFabPressStart()
        controller.onFabPressEnd()
        controller.onRecognizerStateChanged(RecognizerState.Finished("add a note"))

        controller.onSend()

        assertEquals(VoiceSheetState.Hidden, controller.sheetState)
        assertEquals("add a note", readyQuery)
        assertEquals(1, recognizer.resetCalls)
    }

    @Test
    fun onSend_fromEditedTranscript_sendsTheEditedText() {
        var readyQuery: String? = null
        val (controller, _) = newController(onQueryReady = { readyQuery = it })
        controller.onFabPressStart()
        controller.onFabPressEnd()
        controller.onRecognizerStateChanged(RecognizerState.Finished("original"))
        controller.onEdit()
        controller.onEditedTranscriptChanged("edited text")
        controller.onConfirmEdit()

        controller.onSend()

        assertEquals("edited text", readyQuery)
    }

    @Test
    fun onSend_whenNotInReviewOrEditing_doesNothing() {
        var readyQuery: String? = null
        val (controller, _) = newController(onQueryReady = { readyQuery = it })

        controller.onSend()

        assertNull(readyQuery)
        assertEquals(VoiceSheetState.Hidden, controller.sheetState)
    }

    @Test
    fun onCancel_fromListening_hidesSheet_andResetsRecognizer() {
        val (controller, recognizer) = newController()
        controller.onFabPressStart()

        controller.onCancel()

        assertEquals(VoiceSheetState.Hidden, controller.sheetState)
        assertEquals(1, recognizer.stopListeningCalls)
        assertEquals(1, recognizer.resetCalls)
    }

    @Test
    fun recognizerError_permissionDenied_movesToErrorState() {
        val (controller, _) = newController()
        controller.onFabPressStart()

        controller.onRecognizerStateChanged(RecognizerState.Error("Microphone permission denied"))

        assertEquals(VoiceSheetState.Error("Microphone permission denied"), controller.sheetState)
    }

    @Test
    fun onDismissError_hidesSheet_andResetsRecognizer() {
        val (controller, recognizer) = newController()
        controller.onFabPressStart()
        controller.onRecognizerStateChanged(RecognizerState.Error("Something went wrong"))

        controller.onDismissError()

        assertEquals(VoiceSheetState.Hidden, controller.sheetState)
        assertEquals(1, recognizer.resetCalls)
    }

    @Test
    fun onAppBackgrounded_whileListening_stopsTheRecognizer() {
        val (controller, recognizer) = newController()
        controller.onFabPressStart()

        controller.onAppBackgrounded()

        assertEquals(1, recognizer.stopListeningCalls)
    }

    @Test
    fun onAppBackgrounded_whileNotListening_doesNothing() {
        val (controller, recognizer) = newController()

        controller.onAppBackgrounded()

        assertEquals(0, recognizer.stopListeningCalls)
    }

    @Test
    fun autoSend_deliversNonBlankTranscript_withoutReview() {
        var ready: String? = null
        val (controller, _) = newController(config = VoiceSearchConfig(autoSend = true), onQueryReady = { ready = it })
        controller.onFabPressStart()

        controller.onRecognizerStateChanged(RecognizerState.Finished("hello there"))

        assertEquals("hello there", ready)
        assertEquals(VoiceSheetState.Hidden, controller.sheetState)
    }

    @Test
    fun allowEditFalse_ignoresEdit() {
        val (controller, _) = newController(config = VoiceSearchConfig(allowEdit = false))
        controller.onFabPressStart()
        controller.onRecognizerStateChanged(RecognizerState.Finished("hello"))

        controller.onEdit()

        assertEquals(VoiceSheetState.Review("hello"), controller.sheetState)
    }

    @Test
    fun onFabTap_togglesListening() {
        val (controller, recognizer) = newController()

        controller.onFabTap()
        assertEquals(1, recognizer.startListeningCalls)
        assertEquals(VoiceSheetState.Listening(""), controller.sheetState)

        controller.onFabTap()
        assertEquals(1, recognizer.stopListeningCalls)
    }
}
