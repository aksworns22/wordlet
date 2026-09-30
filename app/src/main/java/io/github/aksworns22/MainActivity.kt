package io.github.aksworns22

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import io.github.aksworns22.home.HomeScreen
import io.github.aksworns22.home.sampleWords
import io.github.aksworns22.ui.theme.WordletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordletTheme {
                val words = remember { sampleWords() }
                HomeScreen(
                    words = words,
                    onAddClick = {},
                    onWordClick = {},
                    onStudyClick = {}
                )
            }
        }
    }
}
