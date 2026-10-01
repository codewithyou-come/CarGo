package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameCanvas
import com.example.game.GamePhase
import com.example.game.GameViewModel

@Composable
fun RacingGameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isGarageOpen by remember { mutableStateOf(false) }

    // Android System Back Button Handling
    BackHandler(enabled = true) {
        when {
            isGarageOpen -> isGarageOpen = false
            uiState.phase == GamePhase.PLAYING -> viewModel.pauseGame()
            uiState.phase == GamePhase.PAUSED -> viewModel.quitToMenu()
            uiState.phase == GamePhase.GAME_OVER -> viewModel.quitToMenu()
            uiState.phase == GamePhase.COUNTDOWN -> viewModel.quitToMenu()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E17)),
        contentAlignment = Alignment.Center
    ) {
        // Tablet / Foldable constraint container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            when (uiState.phase) {
                GamePhase.START_MENU -> {
                    StartScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onOpenGarage = { isGarageOpen = true }
                    )
                }

                GamePhase.COUNTDOWN -> {
                    // Show canvas track + live countdown
                    GameCanvas(viewModel = viewModel, uiState = uiState)
                    CountdownOverlay(countdownNumber = uiState.countdownNumber)
                }

                GamePhase.PLAYING -> {
                    // Full game track canvas + live HUD with controls
                    GameCanvas(viewModel = viewModel, uiState = uiState)
                    GameHUD(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier.safeDrawingPadding()
                    )
                }

                GamePhase.PAUSED -> {
                    // Keep canvas in background + pause card overlay
                    GameCanvas(viewModel = viewModel, uiState = uiState)
                    PauseOverlay(viewModel = viewModel, uiState = uiState)
                }

                GamePhase.GAME_OVER -> {
                    // Keep canvas in background + game over card overlay
                    GameCanvas(viewModel = viewModel, uiState = uiState)
                    GameOverOverlay(
                        viewModel = viewModel,
                        uiState = uiState,
                        onOpenGarage = { isGarageOpen = true }
                    )
                }
            }

            // Garage Dialog Overlay
            if (isGarageOpen) {
                GarageDialog(
                    viewModel = viewModel,
                    uiState = uiState,
                    onDismiss = { isGarageOpen = false }
                )
            }
        }
    }
}
