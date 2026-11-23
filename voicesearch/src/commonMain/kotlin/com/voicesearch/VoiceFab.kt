package com.voicesearch

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** A FAB that records while held, like a messaging app's voice-note button. */
@Composable
fun VoiceFab(
    isListening: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
    config: VoiceSearchConfig = VoiceSearchConfig(),
) {
    val colors = config.colors
    val containerColor = if (isListening) {
        colors.fabListeningContainer.takeOrElse { MaterialTheme.colorScheme.error }
    } else {
        colors.fabIdleContainer.takeOrElse { MaterialTheme.colorScheme.primaryContainer }
    }
    val contentColor = if (isListening) {
        colors.fabListeningContent.takeOrElse { MaterialTheme.colorScheme.onError }
    } else {
        colors.fabIdleContent.takeOrElse { MaterialTheme.colorScheme.onPrimaryContainer }
    }
    Surface(
        modifier = modifier
            .size(56.dp)
            .pointerInput(onPressStart, onPressEnd) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressStart()
                    waitForUpOrCancellation()
                    onPressEnd()
                }
            },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Crossfade(targetState = isListening, label = "voiceFabIcon") { listening ->
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = if (listening) config.strings.fabListening else config.strings.fabHoldToTalk,
                    tint = contentColor,
                )
            }
        }
    }
}
