package io.github.aksworns22.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Mastery.label
    get() =
        when (this) {
            Mastery.New -> "새 단어"
            Mastery.Learning -> "익히는 중"
            Mastery.Familiar -> "눈에 익음"
            Mastery.Strong -> "잘 기억함"
            Mastery.Mastered -> "완전히 외움"
        }

/** 링이 얼마나 찼는지. 다 차면 링 대신 꽉 찬 도형을 보여준다. */
private val Mastery.fill get() = ordinal.toFloat() / Mastery.Mastered.ordinal

/**
 * 단어의 [Mastery]를 단계 이름과 차오르는 링으로 보여준다.
 * [studiedFrom]이 있으면 [fillDelayMillis] 뒤에 그 단계에서 [mastery]까지 링이 차오르고,
 * 단계가 그대로여도 통통 튀어 방금 학습했음을 알린다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MasteryBadge(
    mastery: Mastery,
    modifier: Modifier = Modifier,
    studiedFrom: Mastery? = null,
    fillDelayMillis: Long = 0L
) {
    var shown by remember { mutableStateOf(studiedFrom ?: mastery) }
    val fill = remember { Animatable(shown.fill) }
    val pop = remember { Animatable(1f) }
    val fillSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    LaunchedEffect(mastery, studiedFrom) {
        if (studiedFrom == null && shown == mastery) return@LaunchedEffect
        if (studiedFrom != null) delay(fillDelayMillis)
        launch {
            pop.animateTo(1.3f, popSpec)
            pop.animateTo(1f, fillSpec)
        }
        if (shown != mastery) {
            fill.animateTo(mastery.fill, fillSpec)
            shown = mastery
        }
    }

    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedContent(
            targetState = shown,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "masteryLabel"
        ) { level ->
            Text(
                text = level.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (level == Mastery.Mastered) FontWeight.Bold else FontWeight.Medium,
                color = if (level == Mastery.Mastered) colors.tertiary else colors.onSurfaceVariant
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
        ) {
            // 링이 다 차면 꽉 찬 도형으로 피어난다.
            AnimatedContent(
                targetState = shown == Mastery.Mastered,
                transitionSpec = { scaleIn(fillSpec).togetherWith(scaleOut() + fadeOut()) },
                label = "masteryMark"
            ) { mastered ->
                if (mastered) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(colors.tertiary, MaterialShapes.Sunny.toShape())
                    )
                } else {
                    val stroke = with(LocalDensity.current) { Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round) }
                    CircularWavyProgressIndicator(
                        progress = { fill.value },
                        modifier = Modifier.size(28.dp),
                        color = colors.primary,
                        trackColor = colors.surfaceContainerHighest,
                        stroke = stroke,
                        trackStroke = stroke,
                        // 멈춰 있을 때 물결이 치면 로딩처럼 보이므로 차오르는 동안에만 물결친다.
                        amplitude = { if (fill.isRunning) 1f else 0f },
                        wavelength = 8.dp
                    )
                }
            }
        }
    }
}
