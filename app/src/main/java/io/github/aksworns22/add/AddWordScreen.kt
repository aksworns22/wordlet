package io.github.aksworns22.add

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aksworns22.R
import io.github.aksworns22.home.Word
import io.github.aksworns22.ui.highlight
import io.github.aksworns22.ui.theme.WordletTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddWordScreen(
    onAdd: (Word) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var term by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    val canAdd = term.isNotBlank() && meaning.isNotBlank()
    val add = { onAdd(Word(term.trim(), meaning.trim(), example.trim())) }

    val termFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { termFocusRequester.requestFocus() }
    BackHandler(onBack = onClose)

    val scrollState = rememberScrollState()
    // 위쪽 닫기 버튼(위 여백 8dp + 16dp, 크기 56dp)이 절반 넘게 스크롤되어 가려졌는지.
    val closeHiddenOffset = with(LocalDensity.current) { (8 + 16 + 28).dp.roundToPx() }
    val closeHidden by remember { derivedStateOf { scrollState.value > closeHiddenOffset } }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AddBar(
                enabled = canAdd,
                onClick = add,
                showClose = closeHidden,
                onClose = onClose
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(scrollState)
                    // 버튼 뒤에 배경이 없으므로 내용이 버튼 뒤로 스크롤되어 지나가게 한다.
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 닫기 버튼은 홈의 추가 버튼이 있던 오른쪽 위 자리에 겹쳐 둔다.
            Box {
                TermField(
                    value = term,
                    onValueChange = { term = it },
                    modifier = Modifier.focusRequester(termFocusRequester)
                )
                CloseButton(
                    onClick = onClose,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            // 뜻과 예문은 단어를 설명하는 한 묶음이라 segmented 그룹으로 붙인다.
            DetailField(
                label = "뜻",
                placeholder = "어떤 뜻인가요?",
                value = meaning,
                onValueChange = { meaning = it },
                accent = MaterialTheme.colorScheme.secondary,
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                textStyle = MaterialTheme.typography.titleLargeEmphasized,
                firstInGroup = true,
                singleLine = true
            )
            Spacer(Modifier.height(4.dp))
            DetailField(
                label = "예문",
                placeholder = "예문을 적어 두면 기억에 오래 남아요 (선택)",
                value = example,
                onValueChange = { example = it },
                accent = MaterialTheme.colorScheme.tertiary,
                focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                textStyle = MaterialTheme.typography.bodyLarge,
                firstInGroup = false,
                singleLine = false,
                // 예문 속 단어가 눈에 띄도록 입력 중인 단어를 강조한다.
                visualTransformation =
                    HighlightTransformation(
                        keyword = term.trim(),
                        style =
                            SpanStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.ExtraBold
                            )
                    )
            )
        }
    }
}

/**
 * 홈의 추가 버튼과 같은 쿠키 모양 닫기 버튼.
 * 화면이 열리면 스프링으로 45° 돌아 +가 ×가 되고, 누르면 추가 버튼처럼 회전한다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressRotation by animateFloatAsState(
        targetValue = if (pressed) 90f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val openRotation = remember { Animatable(0f) }
    val openSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) { openRotation.animateTo(45f, openSpec) }
    val rotation = openRotation.value + pressRotation
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation }
                .clip(shape)
                // 뜻 칸(surfaceContainerHigh, 포커스 시 secondaryContainer) 위를 지나가도 묻히지 않는 짙은 회색빛.
                .background(MaterialTheme.colorScheme.secondary)
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
            contentDescription = "닫기",
            tint = MaterialTheme.colorScheme.onSecondary
        )
    }
}

/**
 * 화면의 주인공인 단어 입력 카드.
 * 글자를 칠 때마다 오른쪽 아래 해 모양 장식이 스프링으로 돈다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TermField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (focused) 28.dp else 48.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val decoRotation by animateFloatAsState(
        targetValue = value.length * 30f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val textStyle = MaterialTheme.typography.displaySmallEmphasized.copy(color = contentColor)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(contentColor),
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 40.dp, y = 40.dp)
                            .size(160.dp)
                            .graphicsLayer { rotationZ = decoRotation }
                            .clip(MaterialShapes.Sunny.toShape())
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                )
                Text(
                    text = "단어",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = contentColor,
                    modifier = Modifier.padding(start = 28.dp, top = 28.dp)
                )
                // 에디토리얼처럼 큰 단어를 카드 아래쪽에 앉힌다.
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 28.dp, vertical = 28.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "word",
                            style = textStyle,
                            color = contentColor.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

/**
 * 뜻, 예문처럼 단어를 설명하는 입력칸.
 * 포커스를 받으면 그룹에서 떨어져 나오듯 모서리가 고르게 morph되고 [focusedContainerColor]로 물든다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    focusedContainerColor: Color,
    textStyle: TextStyle,
    firstInGroup: Boolean,
    singleLine: Boolean,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val outerRadius by animateDpAsState(
        targetValue = if (focused) 20.dp else 28.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val innerRadius by animateDpAsState(
        targetValue = if (focused) 20.dp else 6.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val containerColor by animateColorAsState(
        targetValue = if (focused) focusedContainerColor else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val shape =
        if (firstInGroup) {
            RoundedCornerShape(outerRadius, outerRadius, innerRadius, innerRadius)
        } else {
            RoundedCornerShape(innerRadius, innerRadius, outerRadius, outerRadius)
        }
    val contentColor = MaterialTheme.colorScheme.onSurface
    val style = textStyle.copy(color = contentColor)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = style,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(containerColor)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = accent
                )
                Spacer(Modifier.height(8.dp))
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = style,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

/** 입력 중인 글자는 그대로 두고 [keyword]와 일치하는 부분만 강조한다. */
private class HighlightTransformation(
    private val keyword: String,
    private val style: SpanStyle
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(text.text.highlight(keyword, style), OffsetMapping.Identity)

    override fun equals(other: Any?): Boolean = other is HighlightTransformation && other.keyword == keyword && other.style == style

    override fun hashCode(): Int = 31 * keyword.hashCode() + style.hashCode()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddBar(
    enabled: Boolean,
    onClick: () -> Unit,
    showClose: Boolean,
    onClose: () -> Unit
) {
    val height = 64.dp
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shapes = ButtonDefaults.shapesFor(height),
            // 버튼 줄에 배경이 없으니 비활성일 때도 뒤 내용이 비치지 않도록 불투명한 색을 쓴다.
            // 회색 입력칸과 섞이지 않게 primary를 옅게 한 색으로 칠한다.
            colors =
                ButtonDefaults.buttonColors(
                    disabledContainerColor =
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.38f)
                            .compositeOver(MaterialTheme.colorScheme.surface),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                ),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = height)
        ) {
            Text(
                text = "추가하기",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                fontWeight = FontWeight.Bold
            )
        }
        // 위쪽 닫기 버튼이 가려지면 추가하기 오른쪽으로 내려온다. 나타날 때 +가 ×로 돈다.
        AnimatedVisibility(
            visible = showClose,
            enter =
                expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeIn(),
            exit =
                shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeOut()
        ) {
            CloseButton(
                onClick = onClose,
                size = height,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddWordScreenPreview() {
    WordletTheme(dynamicColor = false) {
        AddWordScreen(onAdd = {}, onClose = {})
    }
}
