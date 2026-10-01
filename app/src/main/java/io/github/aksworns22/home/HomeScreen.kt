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
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
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
 * 단어장을 고르는 탭. 고른 단어장은 primary 색의 split button으로 부풀고,
 * 나머지는 덜 둥근 사각형으로 물러나 지금 보고 있는 단어장이 한눈에 보인다.
 * 고른 단어장의 ▾를 누르면 관리 메뉴가 열린다.
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
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(decks, key = { it.id }) { deck ->
            val itemModifier = Modifier.animateItem()
            if (deck.id == selected.id) {
                SelectedDeckTab(
                    deck = deck,
                    onAction = onAction,
                    // 기본 단어장은 단어를 넣을 곳으로 늘 남겨 둔다.
                    canDelete = deck.id != Deck.BASIC_ID,
                    modifier = itemModifier
                )
            } else {
                Button(
                    onClick = { onSelect(deck) },
                    shapes = ButtonShapes(MaterialTheme.shapes.medium, MaterialTheme.shapes.small),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                    modifier = itemModifier.heightIn(min = DeckTabHeight)
                ) {
                    Text(deck.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                }
            }
        }
    }
}

private val DeckTabHeight = 48.dp

/**
 * 고른 단어장 탭. 이름과 ▾가 나뉜 split button으로, ▾를 누르면 알약이 둥글게 morph되고
 * 화살표가 뒤집히며 이름 바꾸기·삭제 메뉴가 열린다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectedDeckTab(
    deck: Deck,
    onAction: (DeckAction) -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val colors =
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    SplitButtonLayout(
        modifier = modifier,
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                // 이미 고른 단어장이라 이름을 눌러도 할 일이 없다.
                onClick = {},
                shapes = SplitButtonDefaults.leadingButtonShapesFor(DeckTabHeight),
                colors = colors,
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(DeckTabHeight),
                modifier = Modifier.heightIn(min = DeckTabHeight)
            ) {
                Text(deck.name, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1)
            }
        },
        trailingButton = {
            Box {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (menuExpanded) 180f else 0f,
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                )
                SplitButtonDefaults.TrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(DeckTabHeight),
                    colors = colors,
                    contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(DeckTabHeight),
                    modifier = Modifier.heightIn(min = DeckTabHeight)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = "단어장 관리",
                        modifier =
                            Modifier
                                .size(SplitButtonDefaults.trailingButtonIconSizeFor(DeckTabHeight))
                                .graphicsLayer { rotationZ = arrowRotation }
                    )
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
    )
}

/** 단어장 관리 메뉴. 되돌릴 수 없는 삭제는 error 색으로 구분한다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeckMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (DeckAction) -> Unit,
    canDelete: Boolean
) {
    val count = if (canDelete) 2 else 1
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
            DropdownMenuItem(
                onClick = { onAction(DeckAction.Rename) },
                text = { Text("이름 바꾸기") },
                shape = MenuDefaults.itemShape(0, count).shape,
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) }
            )
            if (canDelete) {
                val error = MaterialTheme.colorScheme.error
                DropdownMenuItem(
                    onClick = { onAction(DeckAction.Delete) },
                    text = { Text("삭제") },
                    shape = MenuDefaults.itemShape(1, count).shape,
                    colors = MenuDefaults.itemColors(textColor = error, leadingIconColor = error),
                    leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) }
                )
            }
        }
    }
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
        AddMenuPopup(
            expanded = expanded,
            onDismiss = { expanded = false },
            items =
                listOf(
                    AddMenuItem("단어 추가", R.drawable.ic_add, onAddClick),
                    AddMenuItem("단어장 가져오기", R.drawable.ic_download, onImportClick)
                )
        )
    }
}

private class AddMenuItem(
    val label: String,
    @param:DrawableRes val icon: Int,
    val onClick: () -> Unit
)

/** 추가 버튼 바로 아래에 오른쪽 끝을 맞춰 항목들이 차례로 튀어나온다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddMenuPopup(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<AddMenuItem>
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = expanded
    // 닫히는 애니메이션이 끝날 때까지 팝업을 남겨 둔다.
    if (!visibleState.currentState && !visibleState.targetState) return
    val offsetY = with(LocalDensity.current) { (56.dp + 8.dp).roundToPx() }
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, item ->
                val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
                val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter =
                        slideInVertically(spatialSpec) { -it * (index + 1) } +
                            scaleIn(scaleSpec, transformOrigin = TransformOrigin(1f, 0f)) +
                            fadeIn(),
                    exit =
                        slideOutVertically(spatialSpec) { -it * (index + 1) } +
                            scaleOut(scaleSpec, transformOrigin = TransformOrigin(1f, 0f)) +
                            fadeOut()
                ) {
                    Surface(
                        onClick = {
                            onDismiss()
                            item.onClick()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shadowElevation = 3.dp
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
