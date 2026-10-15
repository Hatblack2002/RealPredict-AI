package com.realpredict.ai.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.IntOffset

/**
 * Sistema de Diseño de Movimiento y Animación (Sección 19)
 * - Duraciones canónicas y curvas de aceleración (Easing) Material 3
 * - Soporte para Accesibilidad y Reducción de Movimiento
 */
object RealPredictMotion {
    // 1. Duraciones consistentes
    val DurationFast = 150       // Microinteracciones, toggles, hover/press
    val DurationMedium = 250     // Transición de tarjetas, cambio de pestaña
    val DurationLong = 400       // Revelado de modales, rotación orbital
    val DurationProcessing = 600 // Pulso y barras de progreso deterministas

    // 2. Curvas de Aceleración (Easing)
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val AccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    // 3. Animación de Navegación de Pantalla (Slide + Fade)
    fun screenEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(DurationMedium, easing = DecelerateEasing)
        ) + slideIn(
            initialOffset = { IntOffset(0, (it.height * 0.05f).toInt()) },
            animationSpec = tween(DurationMedium, easing = EmphasizedEasing)
        )
    }

    fun screenExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(DurationFast, easing = AccelerateEasing)
        )
    }

    // 4. Transición para esferas de lotería y números
    val NumberRollSpec = tween<Float>(
        durationMillis = DurationMedium,
        easing = EmphasizedEasing
    )
}

/**
 * Composición local para respetar la configuración de reducción de movimiento del sistema
 */
val LocalReducedMotion = compositionLocalOf { false }
