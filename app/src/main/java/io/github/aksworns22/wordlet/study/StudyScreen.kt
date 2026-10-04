package io.github.aksworns22.wordlet.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.aksworns22.fsrs.Rating
import io.github.aksworns22.wordlet.home.sampleWords
import io.github.aksworns22.wordlet.ui.theme.WordletTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

/** 단어를 하나씩 보여주고 평가받는 학습 화면. 목표만큼 평가하거나, 끝내기를 누르거나, 뒤로 나가면 학습이 끝난다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyScreen(
    state: StudyState?,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onFinish)
    // 진행 표시가 끝까지 차는 모습을 보여준 뒤 결과로 넘어간다.
    val finished = state?.finished == true
    LaunchedEffect(finished) {
        if (!finished) return@LaunchedEffect
        delay(600)
        onFinish()
    }
    Surface(modifier = modifier.fillMaxSize()) {
        StudyContent(state, onReveal, onRate, onFinish)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudyContent(
    state: StudyState?,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
    ) {
        StudyTopBar(studied = state?.studied ?: 0, goal = state?.goal ?: StudySessionSize, onFinish = onFinish)
        if (state == null) return@Column
        val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        // 카드와 버튼을 한 장으로 묶어, 다음 단어는 오른쪽에서 통째로 밀려 들어오고
        // 평가한 단어는 버튼까지 그대로 왼쪽으로 빠진다.
        AnimatedContent(
            targetState = state,
            contentKey = { it.word.card.id },
            transitionSpec = {
                slideInHorizontally(slideSpec) { it }
                    .togetherWith(slideOutHorizontally(slideSpec) { -it })
            },
            modifier = Modifier.weight(1f)
        ) { current ->
            StudyPage(state = current, onReveal = onReveal, onRate = onRate)
        }
    }
}

/** 단어 하나를 학습하는 한 장. 위엔 단어 카드를, 아래엔 정답 보기나 평가 버튼을 둔다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudyPage(
    state: StudyState,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            WordCard(state = state)
        }
        val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        val sizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
        val fadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        // 높이가 바뀌는 동안 버튼들이 아래 끝에 붙어 있도록 한다.
        AnimatedContent(
            targetState = state.revealed,
            contentAlignment = Alignment.BottomCenter,
            transitionSpec = {
                (fadeIn(fadeSpec) + scaleIn(popSpec, initialScale = 0.9f))
                    .togetherWith(fadeOut(fadeSpec))
                    .using(SizeTransform(clip = false) { _, _ -> sizeSpec })
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) { revealed ->
            if (revealed) {
                RatingButtons(previews = state.previews, onRate = onRate)
            } else {
                RevealButton(onClick = onReveal)
            }
        }
    }
}

/**
 * 윗줄 왼쪽엔 이번에 학습한 단어 수를, 오른쪽엔 학습을 끝내는 버튼을 두고
 * 그 아래에 이번 학습의 진행 상황을 물결치는 progress로 보여준다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudyTopBar(
    studied: Int,
    goal: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudiedCountBadge(studied)
            Spacer(Modifier.weight(1f))
            val height = 40.dp
            FilledTonalButton(
                onClick = onFinish,
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.heightIn(min = height)
            ) {
                Text(text = "끝내기", style = MaterialTheme.typography.titleSmallEmphasized)
            }
        }
        val progress by animateFloatAsState(
            targetValue = if (goal == 0) 0f else studied.toFloat() / goal,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
        )
        LinearWavyProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = "${goal}개 중 ${studied}개 학습" }
        )
    }
}

/** 단어를 평가할 때마다 결과 화면 hero와 같은 도형이 돌며 톡 튀고 숫자가 올라간다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudiedCountBadge(count: Int) {
    val colors = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(
        targetValue = count * 40f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
    )
    val pop = remember { Animatable(1f) }
    val popUp = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val settle = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(count) {
        if (count == 0) return@LaunchedEffect
        pop.animateTo(1.2f, popUp)
        pop.animateTo(1f, settle)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }.semantics(mergeDescendants = true) { contentDescription = "학습한 단어 ${count}개" }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation }
                .background(colors.tertiary, MaterialShapes.Cookie9Sided.toShape())
        )
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
            }
        ) {
            Text(
                text = "$it",
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = colors.onTertiary,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics {}
            )
        }
    }
}


/**
 * 학습 화면의 hero. 정답을 보면 카드가 스프링으로 살짝 부풀며
 * 모서리가 비대칭으로 morph되고 primary 계열 색으로 물든다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WordCard(state: StudyState) {
    val revealed = state.revealed
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val corner by animateDpAsState(if (revealed) 16.dp else 48.dp, spatial)
    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.96f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val container by animateColorAsState(
        targetValue =
            if (revealed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val content by animateColorAsState(
        targetValue =
            if (revealed) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    Surface(
        shape = RoundedCornerShape(topStart = 48.dp, topEnd = corner, bottomEnd = 48.dp, bottomStart = corner),
        color = container,
        contentColor = content,
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Term(state.word.term)
            AnimatedVisibility(
                visible = revealed,
                enter =
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = state.word.meaning,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    if (state.word.example.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.word.example,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = content.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/** 단어는 한 줄에 들어가는 가장 큰 크기로 보여준다. */
private val TermMaxFontSize = 45.sp
private val TermMinFontSize = 28.sp

/**
 * 단어를 줄바꿈 없이 한 줄에 들어가는 가장 큰 글자로 보여준다.
 * 가장 작은 크기로도 넘치는 긴 구절만 그 크기로 줄바꿈한다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Term(term: String) {
    var wrap by remember(term) { mutableStateOf(false) }
    val style = MaterialTheme.typography.displayMediumEmphasized.copy(lineHeight = 1.15.em)
    if (wrap) {
        Text(
            text = term,
            style = style.copy(fontSize = TermMinFontSize),
            textAlign = TextAlign.Center
        )
    } else {
        Text(
            text = term,
            style = style,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(TermMinFontSize, TermMaxFontSize, stepSize = 2.sp),
            onTextLayout = { if (it.hasVisualOverflow) wrap = true }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RevealButton(onClick: () -> Unit) {
    val height = 64.dp
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(height),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = height)
    ) {
        Text(
            text = "정답 보기",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            fontWeight = FontWeight.Bold
        )
    }
}

private val Rating.label: String
    get() =
        when (this) {
            Rating.Again -> "몰라요"
            Rating.Hard -> "어려워요"
            Rating.Good -> "알아요"
            Rating.Easy -> "쉬워요"
        }

/** 평가 버튼 색. 가장 자주 누를 "알아요"만 primary로 채우고 나머지는 container 톤으로 물러나게 한다. */
@Composable
private fun ratingColors(rating: Rating): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (rating) {
        Rating.Again -> colors.errorContainer to colors.onErrorContainer
        Rating.Hard -> colors.tertiaryContainer to colors.onTertiaryContainer
        Rating.Good -> colors.primary to colors.onPrimary
        Rating.Easy -> colors.secondaryContainer to colors.onSecondaryContainer
    }
}

/** 평가 버튼을 놓는 2×2 자리. 윗줄은 쉬워요·알아요, 아랫줄은 어려워요·몰라요. */
private val RatingRows = listOf(listOf(Rating.Easy, Rating.Good), listOf(Rating.Hard, Rating.Again))

private val RatingButtonHeight = 80.dp
private val RatingRoundCorner = 40.dp
private val RatingInnerCorner = 12.dp

/**
 * 네 버튼이 가운데를 향한 모서리만 각지게 해 하나의 꽃잎 묶음처럼 모이게 한다.
 * 누르면 각진 모서리까지 둥글게 morph되어 묶음에서 떨어져 나오는 느낌을 준다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ratingShapes(rating: Rating): ButtonShapes {
    val round = RatingRoundCorner
    val inner = RatingInnerCorner
    val shape =
        when (rating) {
            Rating.Easy -> RoundedCornerShape(round, round, inner, round)
            Rating.Good -> RoundedCornerShape(round, round, round, inner)
            Rating.Hard -> RoundedCornerShape(round, inner, round, round)
            Rating.Again -> RoundedCornerShape(inner, round, round, round)
        }
    return ButtonShapes(shape = shape, pressedShape = RoundedCornerShape(round))
}

/**
 * 앱의 hero moment인 평가 버튼 묶음.
 * 정답을 보면 네 버튼이 차례로 스프링을 타고 튀어나와 가운데로 모인 꽃잎 모양을 이룬다.
 * 각 줄은 ButtonGroup이라 누른 버튼이 넓어지며 옆 버튼을 민다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RatingButtons(
    previews: Map<Rating, RatingPreview>,
    onRate: (Rating) -> Unit
) {
    val interactions = remember { Rating.entries.associateWith { MutableInteractionSource() } }
    val entrances = remember { Rating.entries.associateWith { Animatable(0f) } }
    val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        RatingRows.flatten().forEachIndexed { index, rating ->
            launch {
                delay(index * 50L)
                entrances.getValue(rating).animateTo(1f, popSpec)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RatingRows.forEach { row ->
            ButtonGroup(
                overflowIndicator = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { rating ->
                    val interaction = interactions.getValue(rating)
                    val entrance = entrances.getValue(rating)
                    customItem(
                        buttonGroupContent = {
                            val (container, content) = ratingColors(rating)
                            Button(
                                onClick = { onRate(rating) },
                                shapes = ratingShapes(rating),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = container,
                                        contentColor = content
                                    ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                interactionSource = interaction,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .heightIn(min = RatingButtonHeight)
                                        .animateWidth(interaction)
                                        .graphicsLayer {
                                            val progress = entrance.value
                                            scaleX = 0.6f + 0.4f * progress
                                            scaleY = 0.6f + 0.4f * progress
                                            alpha = progress.coerceIn(0f, 1f)
                                        }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = rating.label,
                                        style = MaterialTheme.typography.titleLargeEmphasized,
                                        maxLines = 1
                                    )
                                    previews[rating]?.let {
                                        Text(
                                            text = formatInterval(it.interval),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = content.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        },
                        menuContent = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudyScreenPreview() {
    WordletTheme {
        StudyScreen(
            state = StudyState(sampleWords().first(), goal = StudySessionSize, studied = 4),
            onReveal = {},
            onRate = {},
            onFinish = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudyScreenRevealedPreview() {
    val word = sampleWords().first()
    val intervals = listOf(Duration.ofMinutes(1), Duration.ofMinutes(6), Duration.ofMinutes(10), Duration.ofDays(8))
    WordletTheme {
        StudyScreen(
            state =
                StudyState(
                    word = word,
                    goal = StudySessionSize,
                    previews = Rating.entries.zip(intervals).associate { (r, d) -> r to RatingPreview(word.card, d) }
                ),
            onReveal = {},
            onRate = {},
            onFinish = {}
        )
    }
}
