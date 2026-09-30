package io.github.aksworns22

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import io.github.aksworns22.add.AddWordSheet
import io.github.aksworns22.home.HomeScreen
import io.github.aksworns22.home.sampleWords
import io.github.aksworns22.ui.theme.WordletTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 기본 스케줄러는 입력칸을 옮길 때 이전 칸의 키보드 숨김과 새 칸의 키보드 표시를 따로 처리해
        // 키보드가 잠깐 내려갔다 올라온다(삼성 키보드에서 확인). 프레임 단위로 묶어 처리하도록 끈다.
        AndroidComposeUiFlags.isOutOfFrameSchedulerForTextInputEventsEnabled = false
        enableEdgeToEdge()
        setContent {
            WordletTheme {
                val words = remember { mutableStateListOf(*sampleWords().toTypedArray()) }
                var adding by rememberSaveable { mutableStateOf(false) }
                HomeScreen(
                    words = words,
                    onAddClick = { adding = true },
                    onWordClick = {},
                    onStudyClick = {}
                )
                if (adding) {
                    AddWordSheet(
                        onAdd = {
                            words.add(0, it)
                            adding = false
                        },
                        onDismiss = { adding = false }
                    )
                }
            }
        }
    }
}
