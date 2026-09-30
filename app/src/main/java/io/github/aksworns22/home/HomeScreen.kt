package io.github.aksworns22.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.aksworns22.R
import io.github.aksworns22.ui.theme.WordletTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    words: List<Word>,
    onAddClick: () -> Unit,
    onWordClick: (Word) -> Unit,
    onStudyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleWords =
        remember(words, query) {
            val keyword = query.trim()
            words.filter {
                keyword.isEmpty() ||
                    it.term.contains(keyword, ignoreCase = true) ||
                    it.meaning.contains(keyword)
            }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeTopBar(
                query = query,
                onQueryChange = { query = it },
                onAddClick = onAddClick
            )
        },
        bottomBar = { StudyBar(onClick = onStudyClick) }
    ) { innerPadding ->
        if (visibleWords.isEmpty()) {
            EmptyResult(Modifier.padding(innerPadding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                )
        ) {
            itemsIndexed(visibleWords, key = { _, word -> word.card.id }) { index, word ->
                WordItem(
                    word = word,
                    index = index,
                    count = visibleWords.size,
                    onClick = { onWordClick(word) },
                    modifier =
                        Modifier
                            .animateItem()
                            .padding(bottom = ListItemDefaults.SegmentedGap)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("단어나 뜻 검색") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                        exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
                    ) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "검색어 지우기")
                        }
                    }
                }
            )
        }
        AddButton(onClick = onAddClick)
    }
}

/** 누르면 쿠키 모양 컨테이너가 스프링으로 회전하는 추가 버튼 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val rotation by animateFloatAsState(
        targetValue = if (pressed) 90f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .graphicsLayer { rotationZ = rotation }
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick
                ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = "단어 추가",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.graphicsLayer { rotationZ = -rotation }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WordItem(
    word: Word,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier,
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        supportingContent = {
            Text(word.meaning, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    ) {
        Text(
            text = word.term,
            style = MaterialTheme.typography.titleLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudyBar(onClick: () -> Unit) {
    val background = MaterialTheme.colorScheme.surface
    val height = ButtonDefaults.MediumContainerHeight
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to background.copy(alpha = 0f), 0.35f to background))
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    ) {
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
                text = "학습하기",
                style = ButtonDefaults.textStyleFor(height)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyResult(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(MaterialShapes.Ghostish.toShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer)
        )
        Spacer(Modifier.height(24.dp))
        Text("찾는 단어가 없어요", style = MaterialTheme.typography.headlineSmallEmphasized)
        Spacer(Modifier.height(4.dp))
        Text(
            "다른 검색어를 입력해 보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    WordletTheme(dynamicColor = false) {
        HomeScreen(words = sampleWords(), onAddClick = {}, onWordClick = {}, onStudyClick = {})
    }
}
