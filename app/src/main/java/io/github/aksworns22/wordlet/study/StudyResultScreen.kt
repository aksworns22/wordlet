package io.github.aksworns22.wordlet.study

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.aksworns22.wordlet.home.Mastery
import io.github.aksworns22.wordlet.home.MasteryBadge
import io.github.aksworns22.wordlet.home.mastery
import io.github.aksworns22.wordlet.home.sampleWords
import io.github.aksworns22.wordlet.ui.theme.WordletTheme
import kotlinx.coroutines.delay

/** 화면이 열린 뒤 첫 단어가 들어오기 시작할 때까지 기다리는 시간 */
private const val ENTER_DELAY_MILLIS = 300L

/** 단어가 하나씩 차례로 들어오는 간격 */
private const val STAGGER_MILLIS = 90L

/** 단어가 들어온 뒤 링이 차오르기 시작할 때까지 기다리는 시간 */
private const val FILL_DELAY_MILLIS = 350L

/**
 * 학습을 끝내면 이번에 평가한 단어만 모아 보여주는 화면.
 * 단어가 하나씩 들어오며 학습 전 단계에서 지금 단계까지 링이 차오른다.
 * 스크롤해서 나중에 보이는 단어도 처음 보일 때 차오르고, 한 번 보여준 단어는 다시 차오르지 않는다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyResultScreen(
    studied: List<StudiedWord>,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDone)
    val openedAt = remember { SystemClock.uptimeMillis() }
    val played = remember { mutableSetOf<Long>() }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp)
            ) {
                item { ResultHero(count = studied.size) }
                itemsIndexed(studied, key = { _, it -> it.word.card.id }) { index, item ->
                    val first = remember { played.add(item.word.card.id) }
                    // 처음 화면에 있던 단어는 차례로 들어오고, 스크롤해서 보이는 단어는 바로 들어온다.
                    val enterDelay =
                        remember {
                            val elapsed = SystemClock.uptimeMillis() - openedAt
                            (ENTER_DELAY_MILLIS + index * STAGGER_MILLIS - elapsed).coerceAtLeast(0L)
                        }
                    StudiedWordItem(
                        item = item,
                        animate = first,
                        enterDelayMillis = enterDelay,
                        index = index,
                        count = studied.size
                    )
                }
            }
            DoneButton(
                onClick = onDone,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/** 학습한 단어 수를 담은 도형이 돌며 피어나는 결과 화면의 hero */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResultHero(count: Int) {
    val bloom = remember { Animatable(0f) }
    val bloomSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) { bloom.animateTo(1f, bloomSpec) }
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(144.dp)
                    .graphicsLayer {
                        scaleX = bloom.value
                        scaleY = bloom.value
                        rotationZ = (1f - bloom.value) * -90f
                    }.background(colors.tertiary, MaterialShapes.Cookie9Sided.toShape())
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.displayLargeEmphasized,
                color = colors.onTertiary
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "단어를 학습했어요",
            style = MaterialTheme.typography.headlineMediumEmphasized
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudiedWordItem(
    item: StudiedWord,
    animate: Boolean,
    enterDelayMillis: Long,
    index: Int,
    count: Int
) {
    val enter = remember { Animatable(if (animate) 0f else 1f) }
    val enterSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        delay(enterDelayMillis)
        enter.animateTo(1f, enterSpec)
    }
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            Modifier.graphicsLayer {
                alpha = enter.value.coerceIn(0f, 1f)
                translationY = (1f - enter.value) * 48.dp.toPx()
            },
        trailingContent = {
            MasteryBadge(
                mastery = item.word.card.mastery(),
                studiedFrom = item.from.takeIf { animate },
                fillDelayMillis = enterDelayMillis + FILL_DELAY_MILLIS,
                wavy = true
            )
        },
        supportingContent = {
            Text(
                text = item.word.meaning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    ) {
        Text(
            text = item.word.term,
            style = MaterialTheme.typography.titleLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DoneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val height = 64.dp
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(height),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = height)
    ) {
        Text(
            text = "확인",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudyResultScreenPreview() {
    WordletTheme(dynamicColor = false) {
        StudyResultScreen(
            studied = sampleWords().map { StudiedWord(it, Mastery.New) },
            onDone = {}
        )
    }
}
