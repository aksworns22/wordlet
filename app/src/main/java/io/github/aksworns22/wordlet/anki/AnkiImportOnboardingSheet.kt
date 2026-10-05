package io.github.aksworns22.wordlet.anki

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import io.github.aksworns22.wordlet.R
import io.github.aksworns22.wordlet.ui.RotatedShape
import kotlinx.coroutines.launch

/** 온보딩의 한 단계. [screenshot]은 그 단계의 실제 앱 화면이다. */
private data class OnboardingStep(
    @DrawableRes val screenshot: Int,
    val title: String,
    val description: String
)

private val steps =
    listOf(
        OnboardingStep(
            screenshot = R.drawable.onboarding_browse,
            title = "AnkiWeb에서 단어장 찾기",
            description = "다른 사람들이 만들어 공유한 덱을 둘러봐요"
        ),
        OnboardingStep(
            screenshot = R.drawable.onboarding_import,
            title = "이 단어장 가져오기 누르기",
            description = "덱 페이지 아래에 뜨는 버튼을 누르면 바로 받아와요"
        ),
        OnboardingStep(
            screenshot = R.drawable.onboarding_fields,
            title = "단어와 뜻 고르기",
            description = "덱의 어떤 칸을 단어와 뜻으로 쓸지 고르면 단어장이 돼요"
        )
    )

/**
 * 단어장을 처음 가져올 때 AnkiWeb에서 찾고, 가져오고, 필드를 고르는 흐름을 실제 화면으로 보여주는 시트.
 * 마지막 단계에서 [onFinish]로 덱을 구하는 방법을 고르는 안내 시트로 넘어간다.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnkiImportOnboardingSheet(
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == steps.lastIndex

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "단어장 가져오는 방법",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(16.dp))
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top
            ) { page ->
                StepPage(
                    step = steps[page],
                    number = page + 1,
                    badgeShape = badgeShapes[page],
                    badgeColor = badgeColors()[page],
                    pagerState = pagerState,
                    page = page
                )
            }
            Spacer(Modifier.height(16.dp))
            PageIndicator(pagerState)
            Spacer(Modifier.height(16.dp))
            val height = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = {
                    if (isLast) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onFinish() }
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = height)
            ) {
                Text(
                    text = if (isLast) "시작하기" else "다음",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val badgeShapes =
    listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf)

/** 단계 번호 도형의 색. 단계마다 primary, secondary, tertiary를 차례로 쓴다. */
@Composable
private fun badgeColors(): List<Pair<Color, Color>> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        scheme.primary to scheme.onPrimary,
        scheme.secondary to scheme.onSecondary,
        scheme.tertiary to scheme.onTertiary
    )
}

/** 실제 화면 위 모서리에 단계 번호를 겹쳐 두고, 그 아래에 제목과 설명을 둔다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepPage(
    step: OnboardingStep,
    number: Int,
    badgeShape: RoundedPolygon,
    badgeColor: Pair<Color, Color>,
    pagerState: PagerState,
    page: Int
) {
    // 페이지를 넘기는 만큼 번호 도형이 돌아간다.
    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val rotation = offset * 90f
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier =
                    Modifier
                        .padding(top = 12.dp, start = 12.dp)
                        .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(step.screenshot),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                            .clip(RoundedCornerShape(22.dp))
                )
            }
            NumberBadge(
                number = number,
                shape = RotatedShape(badgeShape.toShape(), rotation),
                containerColor = badgeColor.first,
                contentColor = badgeColor.second
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleLargeEmphasized,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NumberBadge(
    number: Int,
    shape: Shape,
    containerColor: Color,
    contentColor: Color
) {
    // 돌린 도형이 경계를 벗어날 수 있어 clip 대신 background로 채운다.
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .background(containerColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = contentColor
        )
    }
}

/** 지금 페이지의 점은 길게 늘이고 primary로 칠한다. */
@Composable
private fun PageIndicator(pagerState: PagerState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        repeat(pagerState.pageCount) { index ->
            val selected = pagerState.currentPage == index
            val width by animateDpAsState(
                targetValue = if (selected) 24.dp else 8.dp,
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
            )
            val color by animateColorAsState(
                targetValue =
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
            )
            Box(
                Modifier
                    .width(width)
                    .height(8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
