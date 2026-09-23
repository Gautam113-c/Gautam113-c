package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameViewModel
import com.example.game.ScreenState
import com.example.ui.MainGameScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkSurface
                ) {
                    val viewModel: GameViewModel = viewModel()
                    PoliticalRunApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun PoliticalRunApp(viewModel: GameViewModel) {
    val screenState by viewModel.screenState.collectAsState()

    // Handle back button behavior
    BackHandler(enabled = screenState != ScreenState.HOME) {
        when (screenState) {
            ScreenState.PLAYING -> viewModel.pauseGame()
            ScreenState.PAUSED,
            ScreenState.GAME_OVER,
            ScreenState.CHARACTER,
            ScreenState.SHOP,
            ScreenState.LEADERBOARD,
            ScreenState.SETTINGS -> viewModel.goToHome()
            ScreenState.HOME -> { /* System default */ }
        }
    }

    MainGameScreen(viewModel)
}
