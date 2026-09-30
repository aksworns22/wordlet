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
import io.github.aksworns22.add.AddWordSheet
import io.github.aksworns22.home.HomeScreen
import io.github.aksworns22.home.sampleWords
import io.github.aksworns22.ui.theme.WordletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
