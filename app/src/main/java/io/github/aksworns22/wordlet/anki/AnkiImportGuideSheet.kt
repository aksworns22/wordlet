package io.github.aksworns22.wordlet.anki

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aksworns22.wordlet.R
import kotlinx.coroutines.launch

/**
 * 단어장을 가져오기 전에 Anki 덱으로 단어장을 만들 수 있다는 것과 덱을 구하는 두 방법을 알려주는 시트.
 * 덱이 없는 사람이 더 많을 것이라 AnkiWeb에서 찾기를 크고 진한 카드로 앞에 두고, 내 파일은 작게 뒤에 둔다.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnkiImportGuideSheet(
    onBrowse: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // 시트가 내려간 뒤에 다음 화면이나 파일 선택기를 띄운다.
    val hideThen = { action: () -> Unit ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Text(
                text = "단어장 가져오기",
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Anki 덱 파일(.apkg)로 단어장을 만들 수 있어요. AnkiWeb에는 다른 사람들이 만들어 공유한 덱이 많아요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(20.dp))
            // 두 방법을 한 그룹으로 붙이고, 맞닿는 모서리만 좁혀 묶음으로 보이게 한다.
            SourceCard(
                icon = R.drawable.ic_public,
                title = "AnkiWeb에서 찾기",
                description = "다운로드만 받으면 바로 가져와요",
                onClick = { hideThen(onBrowse) },
                shape = RoundedCornerShape(32.dp, 32.dp, 8.dp, 8.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                iconContainerColor = MaterialTheme.colorScheme.onPrimary,
                iconColor = MaterialTheme.colorScheme.primary,
                titleStyle = MaterialTheme.typography.titleLargeEmphasized,
                iconSize = 56.dp,
                verticalPadding = 28.dp
            )
            Spacer(Modifier.height(4.dp))
            SourceCard(
                icon = R.drawable.ic_folder_open,
                title = "내 파일에서 고르기",
                description = "이미 받아둔 .apkg 파일",
                onClick = { hideThen(onPickFile) },
                shape = RoundedCornerShape(8.dp, 8.dp, 32.dp, 32.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                titleStyle = MaterialTheme.typography.titleMedium,
                iconSize = 40.dp,
                verticalPadding = 16.dp
            )
        }
    }
}

/** 덱을 구하는 방법 하나. 아이콘은 홈의 추가 버튼과 같은 쿠키 모양에 담는다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceCard(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    iconContainerColor: Color,
    iconColor: Color,
    titleStyle: TextStyle,
    iconSize: Dp,
    verticalPadding: Dp
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = iconContainerColor,
                contentColor = iconColor,
                modifier = Modifier.size(iconSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(icon), contentDescription = null)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(text = title, style = titleStyle)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
