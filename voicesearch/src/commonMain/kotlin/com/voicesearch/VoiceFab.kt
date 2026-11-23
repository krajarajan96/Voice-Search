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

/** Mic button that follows [VoiceSearchConfig.interactionMode], wired straight to [controller]. */
@Composable
fun VoiceFab(
    controller: VoiceCaptureController,
    modifier: Modifier = Modifier,
    config: VoiceSearchConfig = VoiceSearchConfig(),
) {
    VoiceFab(
        isListening = controller.isListening,
        onPressStart = controller::onFabPressStart,
        onPressEnd = controller::onFabPressEnd,
        onTap = controller::onFabTap,
        modifier = modifier,
        config = config,
    )
}

/**
 * A FAB whose gesture follows [VoiceSearchConfig.interactionMode]: hold-to-talk calls
 * [onPressStart] on press and [onPressEnd] on release; tap-to-toggle calls [onTap] on each tap.
 */
@Composable
fun VoiceFab(
    isListening: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
    config: VoiceSearchConfig = VoiceSearchConfig(),
    onTap: () -> Unit = { if (isListening) onPressEnd() else onPressStart() },
) {
    val colors = config.colors
    val toggle = config.interactionMode == VoiceInteractionMode.TapToToggle
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
    val gesture = if (toggle) {
        Modifier.pointerInput(onTap) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                // A completed tap toggles; a cancelled gesture (drag off the button) does not.
                if (waitForUpOrCancellation() != null) onTap()
            }
        }
    } else {
        Modifier.pointerInput(onPressStart, onPressEnd) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                onPressStart()
                waitForUpOrCancellation()
                onPressEnd()
            }
        }
    }
    val description = when {
        isListening && toggle -> config.strings.fabTapToStop
        isListening -> config.strings.fabListening
        toggle -> config.strings.fabTapToTalk
        else -> config.strings.fabHoldToTalk
    }
    Surface(
        modifier = modifier.size(56.dp).then(gesture),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Crossfade(targetState = description, label = "voiceFabIcon") { label ->
                Icon(imageVector = Icons.Filled.Mic, contentDescription = label, tint = contentColor)
            }
        }
    }
}
