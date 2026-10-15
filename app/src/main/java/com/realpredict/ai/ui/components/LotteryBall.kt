package com.realpredict.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realpredict.ai.ui.theme.DarkBackground
import com.realpredict.ai.ui.theme.EmeraldPrimary
import com.realpredict.ai.ui.theme.GoldAccent

enum class BallVariant {
    EMERALD,
    AMBER,
    DARK
}

@Composable
fun LotteryBall(
    number: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    variant: BallVariant = BallVariant.EMERALD,
    label: String? = null
) {
    // Formateo estricto de dos dígitos ("00" a "99")
    val formatted = if (number.length == 1) "0$number" else number

    // Animación de entrada de escala y luminosidad suave (Sección 19)
    val scaleAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "BallScaleAnimation"
    )

    val sphereBrush = when (variant) {
        BallVariant.EMERALD -> Brush.radialGradient(
            colors = listOf(
                Color(0xFF4EFEAB),
                Color(0xFF19C37D),
                Color(0xFF0A6B43),
                Color(0xFF043621)
            )
        )
        BallVariant.AMBER -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE89E),
                Color(0xFFD8B45A),
                Color(0xFF8A6D27),
                Color(0xFF4D3A0E)
            )
        )
        BallVariant.DARK -> Brush.radialGradient(
            colors = listOf(
                Color(0xFF334155),
                Color(0xFF1E293B),
                Color(0xFF0F172A),
                Color(0xFF020617)
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = EmeraldPrimary)
                .clip(CircleShape)
                .background(sphereBrush)
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Disco blanco central reflectante para máxima legibilidad según manual de marca
            Box(
                modifier = Modifier
                    .size(size * 0.65f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .border(0.5.dp, Color.Black.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatted,
                    color = Color(0xFF0B0F14),
                    fontSize = (size.value * 0.32f).sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
