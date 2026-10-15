package com.realpredict.ai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.realpredict.ai.ui.theme.EmeraldPrimary
import com.realpredict.ai.ui.theme.RealPredictMotion

/**
 * Componentes de Animación Oficiales (Sección 19)
 * 1. Jetpack Compose Animation
 * 2. Lottie for Compose
 */

/**
 * Animación de progreso real para operaciones deterministas (Importación, Backtest, APIs).
 * Prohibido usar animaciones falsas o bucles de progreso infinito no vinculados a trabajo real.
 */
@Composable
fun RealProgressIndicator(
    progress: Float, // 0.0f a 1.0f (progreso medible real)
    statusText: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = RealPredictMotion.DurationFast,
            easing = RealPredictMotion.EmphasizedEasing
        ),
        label = "RealProgressAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141B24), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                color = EmeraldPrimary,
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = EmeraldPrimary,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}

/**
 * Transición de estados (Carga, Éxito, Error, Cancelación) con AnimatedContent
 */
enum class OperationState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR,
    CANCELLED
}

@Composable
fun StateAnimatedContainer(
    state: OperationState,
    modifier: Modifier = Modifier,
    content: @Composable (OperationState) -> Unit
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(animationSpec = tween(RealPredictMotion.DurationFast)) +
             scaleIn(initialScale = 0.95f, animationSpec = tween(RealPredictMotion.DurationFast)))
                .togetherWith(
                    fadeOut(animationSpec = tween(RealPredictMotion.DurationFast)) +
                    scaleOut(targetScale = 1.05f, animationSpec = tween(RealPredictMotion.DurationFast))
                )
        },
        label = "StateAnimatedContainer",
        modifier = modifier
    ) { targetState ->
        content(targetState)
    }
}

/**
 * Lottie for Compose: Reproductor de animaciones vectoriales preparadas
 * con soporte para fallback seguro.
 */
@Composable
fun RealPredictLottie(
    rawResId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawResId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = isPlaying,
        speed = 1.0f
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}
