package io.github.aksworns22

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.aksworns22.add.AddWordScreen
import io.github.aksworns22.home.HomeScreen
import io.github.aksworns22.home.sampleWords
import io.github.aksworns22.ui.theme.WordletTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordletTheme {
                val words = remember { mutableStateListOf(*sampleWords().toTypedArray()) }
                var adding by rememberSaveable { mutableStateOf(false) }
                val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                // 추가 화면은 아래에서 스프링으로 떠오르고, 홈은 살짝 물러난다.
                AnimatedContent(
                    targetState = adding,
                    transitionSpec = {
                        if (targetState) {
                            (slideInVertically { it / 3 } + fadeIn())
                                .togetherWith(scaleOut(spatialSpec, targetScale = 0.92f) + fadeOut())
                        } else {
                            (scaleIn(spatialSpec, initialScale = 0.92f) + fadeIn())
                                .togetherWith(slideOutVertically { it / 3 } + fadeOut())
                        }
                    }
                ) { isAdding ->
                    if (isAdding) {
                        AddWordScreen(
                            onAdd = {
                                words.add(0, it)
                                adding = false
                            },
                            onClose = { adding = false }
                        )
                    } else {
                        HomeScreen(
                            words = words,
                            onAddClick = { adding = true },
                            onWordClick = {},
                            onStudyClick = {}
                        )
                    }
                }
            }
        }
    }
}
