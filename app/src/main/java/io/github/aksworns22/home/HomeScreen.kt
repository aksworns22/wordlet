package io.github.aksworns22.home

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.aksworns22.R
import io.github.aksworns22.deck.DeckAction
import io.github.aksworns22.ui.highlight
import io.github.aksworns22.ui.theme.WordletTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    decks: List<Deck>,
    deck: Deck,
    words: List<Word>,
    onDeckSelect: (Deck) -> Unit,
    onDeckAction: (DeckAction) -> Unit,
    onImportClick: () -> Unit,
    onAddClick: () -> Unit,
    onWordClick: (Word) -> Unit,
    onStudyClick: () -> Unit,
    modifier: Modifier = Modifier,
    studied: Map<Long, Mastery> = emptyMap(),
    onStudiedShown: () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searchInteractionSource = remember { MutableInteractionSource() }
    val searchFocused by searchInteractionSource.collectIsFocusedAsState()
    // 키보드의 검색 키로 포커스가 빠져도 검색어가 남아 있으면 검색 중이다.
    val searching = searchFocused || query.isNotEmpty()
    val visibleWords =
        remember(words, query) {
            val keyword = query.trim()
            words.filter {
                keyword.isEmpty() ||
                    it.term.contains(keyword, ignoreCase = true) ||
                    it.meaning.contains(keyword)
            }
        }
    val listState = rememberLazyListState()
    // 아래로 스크롤하면 단어장 탭이 손가락을 따라 접히고, 리스트 맨 위로 돌아와야 다시 펼쳐진다.
    val tabsScroll =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            canScroll = { listState.canScrollForward || listState.canScrollBackward }
        )
    // 리스트가 사라지면 탭을 다시 꺼낼 방법이 없으니 펼쳐 둔다.
    LaunchedEffect(visibleWords.isEmpty()) {
        if (visibleWords.isEmpty()) tabsScroll.state.heightOffset = 0f
    }
    // 탭이 접혀 사라지면 학습하기 옆에 맨 위로 돌아가 탭을 다시 꺼내는 버튼이 나온다.
    val tabsCollapsed by remember { derivedStateOf { tabsScroll.state.collapsedFraction > 0.5f } }
    val scope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = {
        scope.launch { listState.animateScrollToItem(0) }
        // 코드로 하는 스크롤은 탭에 전달되지 않으니 탭도 함께 펼친다.
        scope.launch {
            animate(tabsScroll.state.heightOffset, 0f) { value, _ ->
                tabsScroll.state.heightOffset = value
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(tabsScroll.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                decks = decks,
                deck = deck,
                onDeckSelect = onDeckSelect,
                onDeckAction = onDeckAction,
                onImportClick = onImportClick,
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                interactionSource = searchInteractionSource,
                onAddClick = onAddClick,
                nudgeAddButton = words.isEmpty(),
                tabsState = tabsScroll.state
            )
        },
        bottomBar = {
            // 검색에 집중하도록 학습하기 버튼은 숨기고, 맨 위로 버튼만 키보드 위에 남긴다.
            AnimatedVisibility(
                visible = words.isNotEmpty() && (!searching || tabsCollapsed),
                enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeIn(),
                exit = slideOutVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeOut()
            ) {
                StudyBar(
                    showStudy = !searching,
                    showScrollToTop = tabsCollapsed,
                    onStudyClick = onStudyClick,
                    onScrollToTopClick = scrollToTop
                )
            }
        }
    ) { innerPadding ->
        if (words.isEmpty()) {
            NoWords(Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (visibleWords.isEmpty()) {
            // 검색 중엔 키보드가 올라와 있으니 그 위 영역의 가운데에 둔다.
            EmptyResult(
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            )
            return@Scaffold
        }
        // 학습에서 돌아오면 방금 학습한 단어가 보이도록 옮기고, 보여준 뒤에는 표시를 지운다.
        LaunchedEffect(studied) {
            if (studied.isEmpty()) return@LaunchedEffect
            val first = visibleWords.indexOfFirst { it.card.id in studied }
            val shown = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (first >= 0 && first !in shown) listState.animateScrollToItem(first)
            delay(STUDIED_SHOWN_MILLIS)
            onStudiedShown()
        }
        LazyColumn(
            state = listState,
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
                    keyword = query.trim(),
                    studiedFrom = studied[word.card.id],
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
    decks: List<Deck>,
    deck: Deck,
    onDeckSelect: (Deck) -> Unit,
    onDeckAction: (DeckAction) -> Unit,
    onImportClick: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    interactionSource: MutableInteractionSource,
    onAddClick: () -> Unit,
    nudgeAddButton: Boolean,
    tabsState: TopAppBarState
) {
    val focusManager = LocalFocusManager.current
    val closeSearch = {
        onQueryChange("")
        focusManager.clearFocus()
    }
    BackHandler(enabled = searching, onBack = closeSearch)

    // 검색 중에는 알약 모양이 덜 둥근 사각형으로 스프링 morph되며 색이 짙어진다.
    val cornerRadius by animateDpAsState(
        targetValue = if (searching) 16.dp else 28.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val containerColor by animateColorAsState(
        targetValue =
            if (searching) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor
            ) {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { focusManager.clearFocus() },
                    expanded = false,
                    onExpandedChange = {},
                    interactionSource = interactionSource,
                    placeholder = { Text("단어나 뜻 검색") },
                    leadingIcon = {
                        val iconSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                        AnimatedContent(
                            targetState = searching,
                            transitionSpec = {
                                (scaleIn(iconSpec) + fadeIn()).togetherWith(scaleOut(iconSpec) + fadeOut())
                            }
                        ) { active ->
                            if (active) {
                                IconButton(onClick = closeSearch) {
                                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "검색 닫기")
                                }
                            } else {
                                Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                            }
                        }
                    },
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
            // 검색에 집중하도록 추가 버튼은 자리를 비켜주고, 검색창이 그 폭까지 넓어진다.
            AnimatedVisibility(
                visible = !searching,
                enter =
                    expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeIn(),
                exit =
                    shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeOut()
            ) {
                AddMenu(
                    onAddClick = onAddClick,
                    onImportClick = onImportClick,
                    nudge = nudgeAddButton,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
        DeckTabs(
            decks = decks,
            selected = deck,
            onSelect = onDeckSelect,
            onAction = onDeckAction,
            modifier =
                Modifier
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        tabsState.heightOffsetLimit = -placeable.height.toFloat()
                        val height = (placeable.height + tabsState.heightOffset).roundToInt().coerceAtLeast(0)
                        // 접히는 만큼 위로 밀려 올라가 검색창 뒤로 들어가는 것처럼 보인다.
                        layout(placeable.width, height) { placeable.placeRelative(0, height - placeable.height) }
                    }.graphicsLayer { alpha = 1f - tabsState.collapsedFraction }
                    .padding(top = 12.dp)
        )
    }
}

/**
 * 단어장을 고르는 탭. 고른 단어장은 키가 커지며 primary 색 알약으로 부풀고 ▾ 쿠키가 튀어나오며,
 * 나머지는 덜 둥근 사각형으로 물러나 지금 보고 있는 단어장이 한눈에 보인다.
 * 고른 단어장을 누르면 관리 메뉴가 열린다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeckTabs(
    decks: List<Deck>,
    selected: Deck,
    onSelect: (Deck) -> Unit,
    onAction: (DeckAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    // 새로 가져온 단어장처럼 화면 밖의 단어장이 골라지면 보이도록 스크롤한다.
    LaunchedEffect(selected.id, decks.size) {
        val index = decks.indexOfFirst { it.id == selected.id }
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == index && it.offset >= 0 }
        if (index >= 0 && !visible) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().heightIn(min = DeckTabSelectedHeight),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(decks, key = { it.id }) { deck ->
            DeckTab(
                deck = deck,
                selected = deck.id == selected.id,
                onSelect = { onSelect(deck) },
                onAction = onAction,
                // 기본 단어장은 단어를 넣을 곳으로 늘 남겨 둔다.
                canDelete = deck.id != Deck.BASIC_ID,
                modifier = Modifier.animateItem()
            )
        }
    }
}

// 검색창(56dp)보다 확실히 작아 검색창이 주인공으로 남는다.
private val DeckTabHeight = 32.dp
private val DeckTabSelectedHeight = 40.dp

/**
 * 단어장 탭 하나. 골라지면 모서리, 키, 색이 스프링으로 함께 변해 사각형이 알약으로 부풀고
 * ▾ 쿠키가 옆에서 굴러 나온다. 누르면 모서리가 오므라들며 살짝 눌린다.
 * 메뉴가 열리면 쿠키가 반 바퀴 돌아 ▴를 가리킨다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeckTab(
    deck: Deck,
    selected: Boolean,
    onSelect: () -> Unit,
    onAction: (DeckAction) -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    if (!selected) menuExpanded = false
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val height by animateDpAsState(if (selected) DeckTabSelectedHeight else DeckTabHeight, spatial)
    val corner by animateDpAsState(
        targetValue =
            when {
                pressed -> 6.dp
                selected -> DeckTabSelectedHeight / 2
                else -> 10.dp
            },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .height(height)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.clip(RoundedCornerShape(corner))
                    .background(containerColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        role = Role.Tab,
                        onClick = { if (selected) menuExpanded = !menuExpanded else onSelect() }
                    ).padding(start = 14.dp, end = if (selected) 6.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = deck.name,
                style =
                    if (selected) {
                        MaterialTheme.typography.titleSmallEmphasized
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                color = contentColor,
                maxLines = 1
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter =
                    expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit =
                    shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        scaleOut(MaterialTheme.motionScheme.fastSpatialSpec())
            ) {
                // 쿠키가 굴러 들어오듯 돌며 나타나고, 메뉴가 열리면 반 바퀴 더 돈다.
                val enterSpinSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                val enterSpin by transition.animateFloat(transitionSpec = { enterSpinSpec }) {
                    if (it == EnterExitState.Visible) 0f else -180f
                }
                val menuSpin by animateFloatAsState(
                    targetValue = if (menuExpanded) 180f else 0f,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                )
                Box(
                    modifier =
                        Modifier
                            .padding(start = 6.dp)
                            .size(28.dp)
                            .graphicsLayer { rotationZ = enterSpin + menuSpin }
                            .clip(MaterialShapes.Cookie4Sided.toShape())
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = "단어장 관리",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        DeckMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onAction = {
                menuExpanded = false
                onAction(it)
            },
            canDelete = canDelete
        )
    }
}

/** 단어장 관리 메뉴. 탭 아래로 알약 항목이 튀어나오고, 되돌릴 수 없는 삭제는 error 색으로 구분한다. */
@Composable
private fun DeckMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (DeckAction) -> Unit,
    canDelete: Boolean
) {
    val items =
        buildList {
            add(PillMenuItem("이름 바꾸기", R.drawable.ic_edit, { onAction(DeckAction.Rename) }))
            if (canDelete) {
                add(
                    PillMenuItem(
                        label = "삭제",
                        icon = R.drawable.ic_delete,
                        onClick = { onAction(DeckAction.Delete) },
                        // errorContainer는 분홍 계열 테마에서 단어 카드와 섞여 진한 error 색을 쓴다.
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                )
            }
        }
    PillMenuPopup(
        expanded = expanded,
        onDismiss = onDismiss,
        items = items,
        anchorHeight = DeckTabSelectedHeight,
        alignStart = true
    )
}

/**
 * 쿠키 모양 추가 버튼과, 누르면 그 아래로 펼쳐지는 메뉴.
 * 펼치면 쿠키가 스프링으로 돌며 primary 색으로 짙어지고 +가 ×로 바뀐다.
 * [nudge]가 true면 주기적으로 흔들려 시선을 끈다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddMenu(
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    nudge: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressRotation by animateFloatAsState(
        targetValue = if (pressed) 90f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val expandRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val containerColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val contentColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val nudgeRotation = remember { Animatable(0f) }
    val nudgeSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(nudge, expanded) {
        if (!nudge || expanded) {
            nudgeRotation.animateTo(0f, nudgeSpec)
            return@LaunchedEffect
        }
        while (true) {
            delay(1600)
            nudgeRotation.animateTo(45f, nudgeSpec)
            nudgeRotation.animateTo(0f, nudgeSpec)
        }
    }
    val rotation = pressRotation + expandRotation + nudgeRotation.value
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .clip(shape)
                    .background(containerColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        role = Role.Button,
                        onClick = { expanded = !expanded }
                    ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = if (expanded) "메뉴 닫기" else "추가",
                tint = contentColor,
                modifier = Modifier.graphicsLayer { rotationZ = -rotation + iconRotation }
            )
        }
        PillMenuPopup(
            expanded = expanded,
            onDismiss = { expanded = false },
            items =
                listOf(
                    PillMenuItem("단어 추가", R.drawable.ic_add, onAddClick),
                    PillMenuItem("단어장 가져오기", R.drawable.ic_download, onImportClick)
                ),
            anchorHeight = 56.dp
        )
    }
}

private class PillMenuItem(
    val label: String,
    @param:DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val containerColor: Color? = null,
    val contentColor: Color? = null
)

/**
 * 높이가 [anchorHeight]인 버튼 바로 아래에서 알약 항목들이 차례로 튀어나오는 메뉴.
 * [alignStart]면 버튼의 왼쪽 끝에, 아니면 오른쪽 끝에 맞춘다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PillMenuPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<PillMenuItem>,
    anchorHeight: Dp,
    alignStart: Boolean = false
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = expanded
    // 닫히는 애니메이션이 끝날 때까지 팝업을 남겨 둔다.
    if (!visibleState.currentState && !visibleState.targetState) return
    // 스프링이 목표를 넘어 커질 때 팝업 창 밖으로 나가 잘리지 않도록 버튼 반대쪽에 두는 여유 공간
    val overshootMargin = 32.dp
    val offsetY = with(LocalDensity.current) { (anchorHeight + 8.dp).roundToPx() }
    val origin = TransformOrigin(if (alignStart) 0f else 1f, 0f)
    Popup(
        alignment = if (alignStart) Alignment.TopStart else Alignment.TopEnd,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = if (alignStart) 0.dp else overshootMargin,
                    end = if (alignStart) overshootMargin else 0.dp,
                    bottom = overshootMargin
                ),
            horizontalAlignment = if (alignStart) Alignment.Start else Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, item ->
                val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
                val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter =
                        slideInVertically(spatialSpec) { -it * (index + 1) } +
                            scaleIn(scaleSpec, transformOrigin = origin) +
                            fadeIn(),
                    exit =
                        slideOutVertically(spatialSpec) { -it * (index + 1) } +
                            scaleOut(scaleSpec, transformOrigin = origin) +
                            fadeOut()
                ) {
                    Surface(
                        onClick = {
                            onDismiss()
                            item.onClick()
                        },
                        shape = CircleShape,
                        color = item.containerColor ?: MaterialTheme.colorScheme.primaryContainer,
                        contentColor = item.contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .heightIn(min = 56.dp)
                                    .padding(start = 20.dp, end = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(item.icon), contentDescription = null)
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 방금 학습한 단어를 강조해 보여주는 시간 */
private const val STUDIED_SHOWN_MILLIS = 2500L

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WordItem(
    word: Word,
    keyword: String,
    studiedFrom: Mastery?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlight =
        SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
    // 방금 학습한 단어는 학습하기 버튼 색으로 물들었다가 천천히 원래 색으로 돌아온다.
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val studiedColor = MaterialTheme.colorScheme.tertiaryContainer
    val container = remember { Animatable(if (studiedFrom != null) studiedColor else containerColor) }
    LaunchedEffect(containerColor) {
        container.animateTo(containerColor, tween(durationMillis = 1200, delayMillis = 800))
    }
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier,
        colors = ListItemDefaults.segmentedColors(containerColor = container.value),
        trailingContent = { MasteryBadge(mastery = word.card.mastery(), studiedFrom = studiedFrom) },
        supportingContent = {
            Text(
                text = word.meaning.highlight(keyword, highlight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    ) {
        Text(
            text = word.term.highlight(keyword, highlight),
            style = MaterialTheme.typography.titleLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudyBar(
    showStudy: Boolean,
    showScrollToTop: Boolean,
    onStudyClick: () -> Unit,
    onScrollToTopClick: () -> Unit
) {
    val background = MaterialTheme.colorScheme.surface
    val height = 64.dp
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to background.copy(alpha = 0f), 0.35f to background))
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 맨 위로 버튼이 자라나는 만큼 학습하기 버튼이 줄어든다.
        Box(Modifier.weight(1f)) {
            // Row 안이라 RowScope 버전과 겹치지 않도록 이름을 모두 쓴다.
            androidx.compose.animation.AnimatedVisibility(
                visible = showStudy,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = onStudyClick,
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
                        text = "학습하기",
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showScrollToTop,
            enter =
                expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit =
                shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec())
        ) {
            // 화살표 도형이 반 바퀴 돌며 튀어나와 위를 가리킨다.
            val spinSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            val spin by transition.animateFloat(transitionSpec = { spinSpec }) {
                if (it == EnterExitState.Visible) 0f else -180f
            }
            ScrollToTopButton(
                onClick = onScrollToTopClick,
                size = height,
                modifier =
                    Modifier
                        .padding(start = 12.dp)
                        .graphicsLayer { rotationZ = spin }
            )
        }
    }
}

/** 위를 가리키는 화살표 도형의 맨 위로 버튼. 누르면 위로 쏘아 올리듯 납작해졌다 튄다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScrollToTopButton(
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val squash by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    scaleY = squash
                    scaleX = 2f - squash
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }.clip(MaterialShapes.Arrow.toShape())
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick
                ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_upward),
            contentDescription = "맨 위로",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/** 저장된 단어가 하나도 없을 때 보여주는 화면 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoWords(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(MaterialShapes.Flower.toShape())
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
        )
        Spacer(Modifier.height(24.dp))
        Text("아직 단어가 없어요", style = MaterialTheme.typography.headlineSmallEmphasized)
        Spacer(Modifier.height(4.dp))
        Text(
            "외우고 싶은 첫 단어를 추가해 보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                    .background(MaterialTheme.colorScheme.primary)
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
        HomeScreen(
            decks = sampleDecks,
            deck = sampleDecks.first(),
            words = sampleWords(),
            onDeckSelect = {},
            onDeckAction = {},
            onImportClick = {},
            onAddClick = {},
            onWordClick = {},
            onStudyClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenNoWordsPreview() {
    WordletTheme(dynamicColor = false) {
        HomeScreen(
            decks = sampleDecks.take(1),
            deck = sampleDecks.first(),
            words = emptyList(),
            onDeckSelect = {},
            onDeckAction = {},
            onImportClick = {},
            onAddClick = {},
            onWordClick = {},
            onStudyClick = {}
        )
    }
}
