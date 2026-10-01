package io.github.aksworns22.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.aksworns22.R

/**
 * 시트 아래의 버튼 묶음. 누른 버튼은 넓어지고 옆 버튼은 좁아지며 모양이 바뀐다.
 * 삭제는 되돌릴 수 없어 error 색으로 저장과 구분한다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SheetButtons(
    submitLabel: String,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val height = ButtonDefaults.MediumContainerHeight
    val deleteInteraction = remember { MutableInteractionSource() }
    val submitInteraction = remember { MutableInteractionSource() }
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
    ButtonGroup(
        overflowIndicator = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        if (onDelete != null) {
            customItem(
                buttonGroupContent = {
                    Button(
                        onClick = onDelete,
                        shapes = ButtonDefaults.shapesFor(height),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = errorContainer,
                                contentColor = onErrorContainer
                            ),
                        contentPadding = ButtonDefaults.contentPaddingFor(height),
                        interactionSource = deleteInteraction,
                        modifier =
                            Modifier
                                .heightIn(min = height)
                                .animateWidth(deleteInteraction)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "삭제",
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
                        )
                    }
                },
                menuContent = {}
            )
        }
        customItem(
            buttonGroupContent = {
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    shapes = ButtonDefaults.shapesFor(height),
                    contentPadding = ButtonDefaults.contentPaddingFor(height),
                    interactionSource = submitInteraction,
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = height)
                            .animateWidth(submitInteraction)
                ) {
                    Text(
                        text = submitLabel,
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            },
            menuContent = {}
        )
    }
}

/** 시트의 입력칸. */
@Composable
fun SheetField(
    state: TextFieldState,
    placeholder: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    singleLine: Boolean = true,
    autoCorrect: Boolean = true,
    outputTransformation: OutputTransformation? = null
) {
    val style = textStyle.copy(color = contentColor)
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        lineLimits =
            if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(minHeightInLines = 2),
        cursorBrush = SolidColor(contentColor),
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = autoCorrect,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
            ),
        outputTransformation = outputTransformation,
        decorator =
            TextFieldDecorator { innerTextField ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(containerColor, shape)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = style,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            }
    )
}
