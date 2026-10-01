package io.github.aksworns22.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.aksworns22.R
import io.github.aksworns22.fsrs.Rating
import io.github.aksworns22.home.Deck
import io.github.aksworns22.home.sampleDecks
import io.github.aksworns22.home.sampleWords
import io.github.aksworns22.ui.theme.WordletTheme
import java.time.Duration

/** 단어를 하나씩 보여주고 평가받는 학습 화면. 뒤로 나가면 학습이 끝난다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyScreen(
    deck: Deck,
    state: StudyState?,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    Surface(modifier = modifier.fillMaxSize()) {
        StudyContent(deck, state, onReveal, onRate, onBack)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudyContent(
    deck: Deck,
    state: StudyState?,
    onReveal: () -> Unit,
    onRate: (Rating) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "학습 끝내기")
            }
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleLargeEmphasized,
                maxLines = 1
            )
        }
        if (state == null) return@Column
        val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        // 다음 단어는 오른쪽에서 밀려 들어오고 평가한 단어는 왼쪽으로 빠진다.
        AnimatedContent(
            targetState = state,
            contentKey = { it.word.card.id },
            transitionSpec = {
                (slideInHorizontally(slideSpec) { it } + fadeIn())
                    .togetherWith(slideOutHorizontally(slideSpec) { -it / 2 } + fadeOut())
            },
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
        ) { current ->
            WordCard(state = current, onReveal = onReveal)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {
            AnimatedContent(
                targetState = state.revealed,
                transitionSpec = {
                    (fadeIn() + scaleIn(popSpec, initialScale = 0.9f))
                        .togetherWith(fadeOut())
                }
            ) { revealed ->
                if (revealed) {
                    RatingButtons(previews = state.previews, onRate = onRate)
                } else {
                    RevealButton(onClick = onReveal)
                }
            }
        }
    }
}

/**
 * 학습 화면의 hero. 정답을 보면 카드가 스프링으로 살짝 부풀며
 * 모서리가 비대칭으로 morph되고 primary 계열 색으로 물든다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WordCard(
    state: StudyState,
    onReveal: () -> Unit
) {
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
        onClick = onReveal,
        enabled = !revealed,
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
                        style = MaterialTheme.typography.headlineSmallEmphasized,
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
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ),
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
            Rating.Again -> "모름"
            Rating.Hard -> "어려움"
            Rating.Good -> "알맞음"
            Rating.Easy -> "쉬움"
        }

/**
 * 평가 버튼 묶음. 가장 자주 누를 "알맞음"은 위에 한 줄을 다 차지하고,
 * 나머지 셋은 아래 한 줄에 나눠 앉아 큰 글꼴에서도 잘리지 않는다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RatingButtons(
    previews: Map<Rating, RatingPreview>,
    onRate: (Rating) -> Unit
) {
    val height = 64.dp
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onRate(Rating.Good) },
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
        ) {
            Text(
                text = Rating.Good.label,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                fontWeight = FontWeight.Bold
            )
            previews[Rating.Good]?.let {
                Text(
                    text = formatInterval(it.interval),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
        val others =
            listOf(
                Rating.Again to (colors.errorContainer to colors.onErrorContainer),
                Rating.Hard to (colors.secondaryContainer to colors.onSecondaryContainer),
                Rating.Easy to (colors.tertiaryContainer to colors.onTertiaryContainer)
            )
        val interactions = remember { others.associate { (rating, _) -> rating to MutableInteractionSource() } }
        ButtonGroup(
            overflowIndicator = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            others.forEach { (rating, palette) ->
                val (container: Color, content: Color) = palette
                val interaction = interactions.getValue(rating)
                customItem(
                    buttonGroupContent = {
                        Button(
                            onClick = { onRate(rating) },
                            shapes = ButtonDefaults.shapesFor(height),
                            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            interactionSource = interaction,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = height)
                                    .animateWidth(interaction)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = rating.label,
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    maxLines = 1
                                )
                                previews[rating]?.let {
                                    Text(
                                        text = formatInterval(it.interval),
                                        style = MaterialTheme.typography.labelMedium,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudyScreenPreview() {
    WordletTheme(dynamicColor = false) {
        StudyScreen(
            deck = sampleDecks.first(),
            state = StudyState(sampleWords().first()),
            onReveal = {},
            onRate = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StudyScreenRevealedPreview() {
    val word = sampleWords().first()
    val intervals = listOf(Duration.ofMinutes(1), Duration.ofMinutes(6), Duration.ofMinutes(10), Duration.ofDays(8))
    WordletTheme(dynamicColor = false) {
        StudyScreen(
            deck = sampleDecks.first(),
            state =
                StudyState(
                    word = word,
                    previews = Rating.entries.zip(intervals).associate { (r, d) -> r to RatingPreview(word.card, d) }
                ),
            onReveal = {},
            onRate = {},
            onBack = {}
        )
    }
}
