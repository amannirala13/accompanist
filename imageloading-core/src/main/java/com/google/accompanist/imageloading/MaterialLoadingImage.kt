/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")
@file:JvmName("MaterialLoadingImage")

package com.google.accompanist.imageloading

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorMatrix

@Composable
internal fun updateFadeInTransition(key: Any, durationMs: Int): FadeInTransition {
    // Create our transition state, which allow us to control the state and target states
    val transitionState = remember(key) {
        MutableTransitionState(ImageLoadTransitionState.Empty).apply {
            targetState = ImageLoadTransitionState.Loaded
        }
    }

    // Our actual transition, which reads our transitionState
    val transition = updateTransition(
        transitionState = transitionState,
        label = "Image fadeIn"
    )

    // Alpha animates over the first 50%
    val alpha = transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMs / 2) },
        targetValueByState = { if (it == ImageLoadTransitionState.Loaded) 1f else 0f },
        label = "Image fadeIn alpha",
    )

    // Brightness animates over the first 75%
    val brightness = transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMs * 3 / 4) },
        targetValueByState = { if (it == ImageLoadTransitionState.Loaded) 1f else 0.8f },
        label = "Image fadeIn brightness",
    )

    // Saturation animates over whole duration
    val saturation = transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMs) },
        targetValueByState = { if (it == ImageLoadTransitionState.Loaded) 1f else 0f },
        label = "Image fadeIn saturation",
    )

    return remember(transition) {
        FadeInTransition(alpha, brightness, saturation)
    }.apply {
        this.isFinished = transitionState.currentState == ImageLoadTransitionState.Loaded
    }
}

@Stable
internal class FadeInTransition(
    alpha: State<Float> = mutableStateOf(0f),
    brightness: State<Float> = mutableStateOf(0f),
    saturation: State<Float> = mutableStateOf(0f),
) {
    val alpha by alpha
    val brightness by brightness
    val saturation by saturation
    var isFinished by mutableStateOf(false)
}

private enum class ImageLoadTransitionState { Loaded, Empty }

private fun ImageLoadState.Success.isFromMemory(): Boolean = source == DataSource.MEMORY

/**
 * Ideally we'd use setToSaturation. We can't use that though since it
 * resets the matrix before applying the values
 */
internal fun ColorMatrix.updateSaturation(saturation: Float) {
    val invSat = 1 - saturation
    val R = 0.213f * invSat
    val G = 0.715f * invSat
    val B = 0.072f * invSat
    this[0, 0] = R + saturation
    this[0, 1] = G
    this[0, 2] = B
    this[1, 0] = R
    this[1, 1] = G + saturation
    this[1, 2] = B
    this[2, 0] = R
    this[2, 1] = G
    this[2, 2] = B + saturation
}

internal fun ColorMatrix.updateBrightness(brightness: Float) {
    val darkening = (1f - brightness) * 255
    this[0, 4] = darkening
    this[1, 4] = darkening
    this[2, 4] = darkening
}

internal fun ColorMatrix.updateAlpha(alpha: Float) = set(row = 3, column = 3, v = alpha)
