package com.voicesearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.takeOrElse

/**
 * Hosts the capture UI in the container chosen by [VoiceSearchConfig.presentation]: a modal
 * bottom sheet, a full-screen dialog, or inline where you place this call. Renders nothing
 * while the controller is idle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCaptureSheet(
    controller: VoiceCaptureController,
    config: VoiceSearchConfig = VoiceSearchConfig(),
    modifier: Modifier = Modifier,
) {
    controller.ObserveRecognizer()

    val state = controller.sheetState
    if (state is VoiceSheetState.Hidden) return

    val container = config.colors.sheetContainer.takeOrElse { MaterialTheme.colorScheme.surfaceContainerHigh }
    // Recording must run to completion; everything after (review, errors) can be dismissed.
    val canDismiss = rememberUpdatedState(state !is VoiceSheetState.Listening)
    val onDismiss = {
        when (state) {
            is VoiceSheetState.Error -> controller.onDismissError()
            else -> controller.onCancel()
        }
    }

    when (config.presentation) {
        VoicePresentation.BottomSheet -> {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false,
                confirmValueChange = { target -> target != SheetValue.Hidden || canDismiss.value },
            )
            ModalBottomSheet(
                onDismissRequest = { onDismiss() },
                sheetState = sheetState,
                containerColor = container,
                dragHandle = if (canDismiss.value) {
                    { BottomSheetDefaults.DragHandle() }
                } else {
                    null
                },
            ) {
                VoiceCaptureContent(controller, config, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
        }

        VoicePresentation.FullScreen -> Dialog(
            onDismissRequest = { if (canDismiss.value) onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = canDismiss.value,
                dismissOnClickOutside = false,
            ),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = container) {
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
                    VoiceCaptureContent(controller, config, Modifier.align(Alignment.Center))
                }
            }
        }

        VoicePresentation.Inline -> Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = container,
        ) {
            VoiceCaptureContent(controller, config, Modifier.padding(16.dp))
        }
    }
}

/**
 * The capture UI itself (listening, review, edit, error) with no container, so you can place it
 * in any screen or layout. Pair with a controller; renders nothing while the controller is idle.
 */
@Composable
fun VoiceCaptureContent(
    controller: VoiceCaptureController,
    config: VoiceSearchConfig = VoiceSearchConfig(),
    modifier: Modifier = Modifier,
) {
    val strings = config.strings
    val accent = config.colors.accent.takeOrElse { MaterialTheme.colorScheme.error }
    Column(modifier = modifier.fillMaxWidth()) {
        when (val state = controller.sheetState) {
            is VoiceSheetState.Listening -> ListeningContent(state, strings, accent, onStop = controller::onStopListening)
            is VoiceSheetState.Review -> ReviewContent(
                transcript = state.transcript,
                strings = strings,
                allowEdit = config.allowEdit,
                onCancel = controller::onCancel,
                onEdit = controller::onEdit,
                onSend = controller::onSend,
            )
            is VoiceSheetState.Editing -> EditingContent(
                transcript = state.transcript,
                strings = strings,
                onTranscriptChange = controller::onEditedTranscriptChanged,
                onCancel = controller::onCancel,
                onConfirm = controller::onConfirmEdit,
            )
            is VoiceSheetState.Error -> ErrorContent(
                message = state.message,
                strings = strings,
                accent = accent,
                onDismiss = controller::onDismissError,
                onRetry = controller::onRetry,
            )
            VoiceSheetState.Hidden -> Unit
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ListeningContent(
    state: VoiceSheetState.Listening,
    strings: VoiceSearchStrings,
    accent: Color,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Filled.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = accent,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(strings.listening, style = MaterialTheme.typography.titleMedium)
        if (state.partialTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.partialTranscript,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Text(strings.stopListening)
        }
    }
}

@Composable
private fun ReviewContent(
    transcript: String,
    strings: VoiceSearchStrings,
    allowEdit: Boolean,
    onCancel: () -> Unit,
    onEdit: () -> Unit,
    onSend: () -> Unit,
) {
    if (transcript.isBlank()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(strings.noSpeechCaptured, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.holdAndTryAgain,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(strings.tryAgain) }
        }
        return
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.heardTitle, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = strings.dismiss)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(transcript, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (allowEdit) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(strings.edit) }
            }
            Button(onClick = onSend, modifier = Modifier.weight(1f)) { Text(strings.send) }
        }
    }
}

@Composable
private fun EditingContent(
    transcript: String,
    strings: VoiceSearchStrings,
    onTranscriptChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column {
        Text(strings.editTranscript, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = transcript,
            onValueChange = onTranscriptChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(strings.cancel) }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text(strings.done) }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    strings: VoiceSearchStrings,
    accent: Color,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(strings.dismiss)
            }
            Button(onClick = onRetry, modifier = Modifier.weight(1f)) {
                Text(strings.tryAgain)
            }
        }
    }
}
