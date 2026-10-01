package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CarSkin
import com.example.game.ControlMode
import com.example.game.GameUiState
import com.example.game.GameViewModel

@Composable
fun StartScreen(
    viewModel: GameViewModel,
    uiState: GameUiState,
    onOpenGarage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090E17),
                        Color(0xFF0F172A),
                        Color(0xFF0A0F1D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title Banner
            Surface(
                color = Color(0x3300E5FF),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
            ) {
                Text(
                    text = "ARCADE HIGHWAY",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TURBO RACER",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Dodge Traffic • Collect Coins • Master Speed",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Car Canvas Presentation
            HeroCarShowcase(car = uiState.selectedCar)

            Spacer(modifier = Modifier.height(20.dp))

            // Player Stats Card (High Score & Total Coins)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // High Score Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF131D31),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223456))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33FFD700),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "BEST SCORE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${uiState.highScore}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Total Coins Card
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF131D31),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223456))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x33FFB300),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "TOTAL COINS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${uiState.totalCoins}", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRIMARY ACTION: START RACE BUTTON
            Button(
                onClick = { viewModel.startCountdown() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .scale(buttonScale)
                    .testTag("start_game_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3D00)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START RACE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // GARAGE BUTTON
            OutlinedButton(
                onClick = onOpenGarage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("garage_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
            ) {
                Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GARAGE (${uiState.selectedCar.name})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CONTROL MODE SELECTOR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF131D31),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223456))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "STEERING CONTROLS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ControlMode.values().forEach { mode ->
                            val isSelected = uiState.controlMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setControlMode(mode) },
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1A263E),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSelected) Color(0xFF00363A) else Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AUDIO & HAPTICS SETTINGS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sound toggle card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.toggleSound() },
                    color = Color(0xFF131D31),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223456))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = null,
                            tint = if (uiState.soundEnabled) Color(0xFF00E5FF) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.soundEnabled) "Sound ON" else "Muted",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Haptics toggle card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.toggleHaptics() },
                    color = Color(0xFF131D31),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223456))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (uiState.hapticsEnabled) Color(0xFF00E5FF) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.hapticsEnabled) "Vibe ON" else "Vibe OFF",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // HOW TO PLAY TIPS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0x661E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "RACING TIPS", color = Color(0xFFFFD54F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• Drag finger or tap buttons to steer between 4 highway lanes.", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    Text(text = "• Near-miss overtakes give +150 bonus points.", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    Text(text = "• Collect Turbo ⚡ for invincibility and massive speed!", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    Text(text = "• Collect Shield 🛡️ to absorb a fatal collision.", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroCarShowcase(car: CarSkin) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF223555))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Road patch under hero car
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF162135), Color(0xFF0F172A))
                    ),
                    topLeft = Offset(w * 0.15f, 0f),
                    size = Size(w * 0.70f, h)
                )

                // Neon road dividers
                drawLine(
                    color = Color(0x4400E5FF),
                    start = Offset(w * 0.38f, 0f),
                    end = Offset(w * 0.38f, h),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0x4400E5FF),
                    start = Offset(w * 0.62f, 0f),
                    end = Offset(w * 0.62f, h),
                    strokeWidth = 2f
                )

                // Headlight rays
                val cx = w * 0.5f
                val cy = h * 0.55f
                val carW = 68f
                val carH = 112f

                // Light beam
                val beam = Path().apply {
                    moveTo(cx - carW * 0.35f, cy - carH * 0.4f)
                    lineTo(cx - carW * 1.6f, 0f)
                    lineTo(cx + carW * 1.6f, 0f)
                    lineTo(cx + carW * 0.35f, cy - carH * 0.4f)
                    close()
                }
                drawPath(
                    beam,
                    Brush.verticalGradient(
                        listOf(Color(0x00FFFFFF), Color(0x3300E5FF)),
                        startY = 0f,
                        endY = cy
                    )
                )

                // Drop shadow
                drawRoundRect(
                    color = Color(0x66000000),
                    topLeft = Offset(cx - carW * 0.52f, cy - carH * 0.45f + 10f),
                    size = Size(carW * 1.04f, carH * 1.02f),
                    cornerRadius = CornerRadius(18f, 18f)
                )

                // Wheels
                val wheelW = carW * 0.22f
                val wheelH = carH * 0.24f
                val wheels = listOf(
                    Offset(cx - carW * 0.56f, cy - carH * 0.40f),
                    Offset(cx + carW * 0.34f, cy - carH * 0.40f),
                    Offset(cx - carW * 0.56f, cy + carH * 0.16f),
                    Offset(cx + carW * 0.34f, cy + carH * 0.16f)
                )
                for (pos in wheels) {
                    drawRoundRect(
                        color = Color(0xFF111111),
                        topLeft = pos,
                        size = Size(wheelW, wheelH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // Car Body
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(car.primaryColor, car.accentColor),
                        startY = cy - carH * 0.5f,
                        endY = cy + carH * 0.5f
                    ),
                    topLeft = Offset(cx - carW * 0.5f, cy - carH * 0.5f),
                    size = Size(carW, carH),
                    cornerRadius = CornerRadius(18f, 18f)
                )

                // Stripes
                drawRect(
                    color = car.stripeColor,
                    topLeft = Offset(cx - carW * 0.10f, cy - carH * 0.5f + 4f),
                    size = Size(carW * 0.20f, carH - 8f)
                )

                // Windshield
                drawRoundRect(
                    color = Color(0xFF0D1B2A),
                    topLeft = Offset(cx - carW * 0.38f, cy - carH * 0.25f),
                    size = Size(carW * 0.76f, carH * 0.22f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Headlights
                drawCircle(color = Color(0xFFFFF9C4), radius = 6f, center = Offset(cx - carW * 0.32f, cy - carH * 0.44f))
                drawCircle(color = Color(0xFFFFF9C4), radius = 6f, center = Offset(cx + carW * 0.32f, cy - carH * 0.44f))

                // Taillights
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(cx - carW * 0.38f, cy + carH * 0.42f),
                    size = Size(carW * 0.22f, 6f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(cx + carW * 0.16f, cy + carH * 0.42f),
                    size = Size(carW * 0.22f, 6f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}
