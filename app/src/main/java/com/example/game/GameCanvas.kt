package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    viewModel: GameViewModel,
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    val soundSystem = viewModel.soundSystem

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.controlMode, uiState.phase) {
                if (uiState.phase == GamePhase.PLAYING) {
                    when (uiState.controlMode) {
                        ControlMode.TOUCH_DRAG -> {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val normX = (offset.x / size.width).coerceIn(0.12f, 0.88f)
                                    viewModel.setPlayerTargetNormalizedX(normX)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val normX = (change.position.x / size.width).coerceIn(0.12f, 0.88f)
                                    viewModel.setPlayerTargetNormalizedX(normX)
                                }
                            )
                        }
                        ControlMode.LANE_TAP -> {
                            detectTapGestures { offset ->
                                val normX = offset.x / size.width
                                val lane = when {
                                    normX < 0.25f -> 0
                                    normX < 0.50f -> 1
                                    normX < 0.75f -> 2
                                    else -> 3
                                }
                                viewModel.tapLane(lane)
                            }
                        }
                        ControlMode.BUTTONS -> {
                            // Touch drag still supported as fallback, plus on-screen buttons
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    val normX = (change.position.x / size.width).coerceIn(0.12f, 0.88f)
                                    viewModel.setPlayerTargetNormalizedX(normX)
                                }
                            )
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        viewModel.onScreenSizeChanged(width, height)

        // 1. Draw Road and borders
        drawRoad(width, height, viewModel.trackOffset, uiState.turboActive)

        // 2. Draw Collectibles
        drawCollectibles(viewModel.collectibles, width)

        // 3. Draw Obstacles (Traffic & Roadblocks)
        drawObstacles(viewModel.obstacles, width)

        // 4. Draw Particles
        drawParticles(viewModel.particles)

        // 5. Draw Player Car
        drawPlayerCar(
            playerNormX = viewModel.playerNormalizedX,
            screenW = width,
            screenH = height,
            car = uiState.selectedCar,
            isTurbo = uiState.turboActive,
            isShield = uiState.shieldActive,
            isMagnet = uiState.magnetActive,
            invulnTimer = uiState.invulnerabilityTimer
        )

        // 6. Draw Speed streaks during Turbo
        if (uiState.turboActive) {
            drawSpeedStreaks(width, height, viewModel.trackOffset)
        }

        // 7. Draw Floating notices (+50, NEAR MISS, etc.)
        drawFloatingNotices(viewModel.floatingNotices)
    }
}

private fun DrawScope.drawRoad(width: Float, height: Float, trackOffset: Float, isTurbo: Boolean) {
    // Sidewalk / Grass sides
    drawRect(
        color = Color(0xFF0F1E16),
        topLeft = Offset(0f, 0f),
        size = Size(width, height)
    )

    // Main asphalt road (middle 92% of screen width)
    val roadLeft = width * 0.04f
    val roadRight = width * 0.96f
    val roadWidth = roadRight - roadLeft

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF141924),
                Color(0xFF1A2130),
                Color(0xFF1E2638),
                Color(0xFF1A2130),
                Color(0xFF141924)
            ),
            startX = roadLeft,
            endX = roadRight
        ),
        topLeft = Offset(roadLeft, 0f),
        size = Size(roadWidth, height)
    )

    // Curbs on edges (alternating red and white curb pattern)
    val curbWidth = width * 0.02f
    val curbSegmentHeight = 40f
    val curbOffset = trackOffset % (curbSegmentHeight * 2)

    var curY = -curbSegmentHeight * 2 + curbOffset
    while (curY < height + curbSegmentHeight) {
        val isRed = ((curY / curbSegmentHeight).toInt() % 2 == 0)
        val curbColor = if (isRed) Color(0xFFD32F2F) else Color(0xFFFFFFFF)

        // Left curb
        drawRect(
            color = curbColor,
            topLeft = Offset(roadLeft - curbWidth, curY),
            size = Size(curbWidth, curbSegmentHeight)
        )
        // Right curb
        drawRect(
            color = curbColor,
            topLeft = Offset(roadRight, curY),
            size = Size(curbWidth, curbSegmentHeight)
        )
        curY += curbSegmentHeight
    }

    // Outer solid yellow shoulder lines
    drawLine(
        color = if (isTurbo) Color(0xFF00E5FF) else Color(0xFFFFD600),
        start = Offset(roadLeft + 6f, 0f),
        end = Offset(roadLeft + 6f, height),
        strokeWidth = 4f
    )
    drawLine(
        color = if (isTurbo) Color(0xFF00E5FF) else Color(0xFFFFD600),
        start = Offset(roadRight - 6f, 0f),
        end = Offset(roadRight - 6f, height),
        strokeWidth = 4f
    )

    // Dashed white lane dividers (for 4 lanes: 3 divider lines at 25%, 50%, 75%)
    val dashHeight = 55f
    val dashGap = 55f
    val totalDashPeriod = dashHeight + dashGap
    val dashOffset = trackOffset % totalDashPeriod

    val dividerPositions = floatArrayOf(
        roadLeft + roadWidth * 0.25f,
        roadLeft + roadWidth * 0.50f,
        roadLeft + roadWidth * 0.75f
    )

    for (divX in dividerPositions) {
        var dashY = -totalDashPeriod + dashOffset
        while (dashY < height + totalDashPeriod) {
            drawRoundRect(
                color = if (isTurbo) Color(0xCC00E5FF) else Color(0xB3FFFFFF),
                topLeft = Offset(divX - 2.5f, dashY),
                size = Size(5f, dashHeight),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
            dashY += totalDashPeriod
        }
    }
}

private fun DrawScope.drawPlayerCar(
    playerNormX: Float,
    screenW: Float,
    screenH: Float,
    car: CarSkin,
    isTurbo: Boolean,
    isShield: Boolean,
    isMagnet: Boolean,
    invulnTimer: Float
) {
    val carW = 54f * (screenW / 400f).coerceIn(0.9f, 1.25f)
    val carH = 92f * (screenH / 800f).coerceIn(0.9f, 1.25f)
    val centerX = playerNormX * screenW
    val centerY = screenH * 0.78f

    // Invulnerability blink
    if (invulnTimer > 0f && ((invulnTimer * 10).toInt() % 2 == 0)) {
        return
    }

    // 1. Headlight beam cones onto road
    val beamBrush = Brush.verticalGradient(
        colors = listOf(
            if (isTurbo) Color(0x6600E5FF) else Color(0x55FFF9C4),
            Color(0x00FFFFFF)
        ),
        startY = centerY - carH * 0.5f,
        endY = centerY - carH * 3.5f
    )
    val beamPath = Path().apply {
        moveTo(centerX - carW * 0.35f, centerY - carH * 0.4f)
        lineTo(centerX - carW * 1.5f, centerY - carH * 3.2f)
        lineTo(centerX + carW * 1.5f, centerY - carH * 3.2f)
        lineTo(centerX + carW * 0.35f, centerY - carH * 0.4f)
        close()
    }
    drawPath(beamPath, beamBrush)

    // 2. Car Soft Drop-Shadow
    drawRoundRect(
        color = Color(0x66000000),
        topLeft = Offset(centerX - carW * 0.52f, centerY - carH * 0.45f + 8f),
        size = Size(carW * 1.04f, carH * 1.02f),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // 3. Wheels / Tires (4 corners)
    val wheelW = carW * 0.22f
    val wheelH = carH * 0.24f
    val wheelColor = Color(0xFF111111)
    val rimColor = Color(0xFFBDBDBD)

    val wheelOffsets = listOf(
        Offset(centerX - carW * 0.56f, centerY - carH * 0.40f), // Front Left
        Offset(centerX + carW * 0.34f, centerY - carH * 0.40f), // Front Right
        Offset(centerX - carW * 0.56f, centerY + carH * 0.16f), // Rear Left
        Offset(centerX + carW * 0.34f, centerY + carH * 0.16f)  // Rear Right
    )
    for (pos in wheelOffsets) {
        drawRoundRect(
            color = wheelColor,
            topLeft = pos,
            size = Size(wheelW, wheelH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = rimColor,
            topLeft = Offset(pos.x + wheelW * 0.25f, pos.y + wheelH * 0.25f),
            size = Size(wheelW * 0.5f, wheelH * 0.5f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }

    // 4. Main Aerodynamic Car Body
    val bodyLeft = centerX - carW * 0.5f
    val bodyTop = centerY - carH * 0.5f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(car.primaryColor, car.accentColor),
            startY = bodyTop,
            endY = bodyTop + carH
        ),
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(carW, carH),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // 5. Racing Stripes down center
    val stripeW = carW * 0.18f
    drawRect(
        color = car.stripeColor.copy(alpha = 0.85f),
        topLeft = Offset(centerX - stripeW * 0.5f, bodyTop + 4f),
        size = Size(stripeW, carH - 8f)
    )

    // 6. Windshield and Rear Windows
    // Front Windshield
    val windTop = bodyTop + carH * 0.22f
    val windH = carH * 0.22f
    val windW = carW * 0.76f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B)),
            startY = windTop,
            endY = windTop + windH
        ),
        topLeft = Offset(centerX - windW * 0.5f, windTop),
        size = Size(windW, windH),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Windshield glass reflection glare
    drawLine(
        color = Color(0x80E0F7FA),
        start = Offset(centerX - windW * 0.35f, windTop + 4f),
        end = Offset(centerX - windW * 0.1f, windTop + windH - 4f),
        strokeWidth = 3f
    )

    // Rear Window
    val rearWinTop = bodyTop + carH * 0.58f
    val rearWinH = carH * 0.15f
    val rearWinW = carW * 0.68f
    drawRoundRect(
        color = Color(0xFF0D1B2A),
        topLeft = Offset(centerX - rearWinW * 0.5f, rearWinTop),
        size = Size(rearWinW, rearWinH),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // 7. Headlights
    val lightSize = carW * 0.18f
    drawCircle(
        color = if (isTurbo) Color(0xFF00E5FF) else Color(0xFFFFF9C4),
        radius = lightSize * 0.5f,
        center = Offset(bodyLeft + carW * 0.20f, bodyTop + 6f)
    )
    drawCircle(
        color = if (isTurbo) Color(0xFF00E5FF) else Color(0xFFFFF9C4),
        radius = lightSize * 0.5f,
        center = Offset(bodyLeft + carW * 0.80f, bodyTop + 6f)
    )

    // 8. Taillights & Exhaust
    val tailW = carW * 0.22f
    val tailH = 6f
    drawRoundRect(
        color = Color(0xFFFF1744),
        topLeft = Offset(bodyLeft + carW * 0.12f, bodyTop + carH - 8f),
        size = Size(tailW, tailH),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFFFF1744),
        topLeft = Offset(bodyLeft + carW * 0.66f, bodyTop + carH - 8f),
        size = Size(tailW, tailH),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // 9. Rear Spoiler Wing
    val spoilerW = carW * 0.95f
    val spoilerH = 8f
    drawRoundRect(
        color = Color(0xFF212121),
        topLeft = Offset(centerX - spoilerW * 0.5f, bodyTop + carH - 3f),
        size = Size(spoilerW, spoilerH),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // 10. Turbo Exhaust Flames
    if (isTurbo) {
        val flameLen = 28f + (sin(System.currentTimeMillis() * 0.05) * 8f).toFloat()
        // Left flame
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF00E5FF), Color(0xFFFF9100), Color(0x00FF3D00)),
                startY = bodyTop + carH,
                endY = bodyTop + carH + flameLen
            ),
            topLeft = Offset(bodyLeft + carW * 0.24f, bodyTop + carH + 2f),
            size = Size(carW * 0.16f, flameLen),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Right flame
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF00E5FF), Color(0xFFFF9100), Color(0x00FF3D00)),
                startY = bodyTop + carH,
                endY = bodyTop + carH + flameLen
            ),
            topLeft = Offset(bodyLeft + carW * 0.60f, bodyTop + carH + 2f),
            size = Size(carW * 0.16f, flameLen),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    // 11. Shield Forcefield Bubble
    if (isShield) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x0000E5FF), Color(0x4400E5FF), Color(0xBB76FF03)),
                center = Offset(centerX, centerY),
                radius = carH * 0.72f
            ),
            radius = carH * 0.72f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF76FF03),
            radius = carH * 0.72f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3.5f)
        )
    }

    // 12. Magnet Indicator Sparks
    if (isMagnet) {
        val pulse = (sin(System.currentTimeMillis() * 0.01) * 6f).toFloat()
        drawCircle(
            color = Color(0x66FF4081),
            radius = carH * 0.65f + pulse,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5f)
        )
    }
}

private fun DrawScope.drawObstacles(obstacles: List<Obstacle>, screenW: Float) {
    for (obs in obstacles) {
        val obsScreenX = obs.x * screenW
        val obsW = obs.widthDp * (screenW / 400f).coerceIn(0.9f, 1.25f)
        val obsH = obs.heightDp * (screenW / 400f).coerceIn(0.9f, 1.25f)
        val left = obsScreenX - obsW * 0.5f
        val top = obs.y - obsH * 0.5f

        when (obs.type) {
            ObstacleType.SEDAN, ObstacleType.SPORTS -> {
                // Drop shadow
                drawRoundRect(
                    color = Color(0x55000000),
                    topLeft = Offset(left, top + 6f),
                    size = Size(obsW, obsH),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Car body
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(obs.color, obs.color.copy(alpha = 0.8f)),
                        startY = top,
                        endY = top + obsH
                    ),
                    topLeft = Offset(left, top),
                    size = Size(obsW, obsH),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Windshield
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(left + obsW * 0.15f, top + obsH * 0.25f),
                    size = Size(obsW * 0.70f, obsH * 0.22f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Rear window
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(left + obsW * 0.20f, top + obsH * 0.62f),
                    size = Size(obsW * 0.60f, obsH * 0.14f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Red Taillights facing player
                drawCircle(
                    color = Color(0xFFFF1744),
                    radius = obsW * 0.10f,
                    center = Offset(left + obsW * 0.22f, top + obsH - 5f)
                )
                drawCircle(
                    color = Color(0xFFFF1744),
                    radius = obsW * 0.10f,
                    center = Offset(left + obsW * 0.78f, top + obsH - 5f)
                )
            }
            ObstacleType.TRUCK -> {
                // Large Cargo Semi-Truck
                // Drop shadow
                drawRoundRect(
                    color = Color(0x55000000),
                    topLeft = Offset(left, top + 8f),
                    size = Size(obsW, obsH),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Cab (front top portion)
                val cabH = obsH * 0.32f
                drawRoundRect(
                    color = Color(0xFF455A64),
                    topLeft = Offset(left, top),
                    size = Size(obsW, cabH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Cab windshield
                drawRoundRect(
                    color = Color(0xFF90A4AE),
                    topLeft = Offset(left + obsW * 0.12f, top + cabH * 0.2f),
                    size = Size(obsW * 0.76f, cabH * 0.35f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Trailer (long cargo section)
                val trailerTop = top + cabH + 4f
                val trailerH = obsH - cabH - 4f
                drawRoundRect(
                    color = obs.color,
                    topLeft = Offset(left, trailerTop),
                    size = Size(obsW, trailerH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Trailer corrugated ribs
                for (r in 1..4) {
                    val ribY = trailerTop + (trailerH * r / 5f)
                    drawLine(
                        color = Color(0x33000000),
                        start = Offset(left + 4f, ribY),
                        end = Offset(left + obsW - 4f, ribY),
                        strokeWidth = 2f
                    )
                }
                // Rear warning chevron bumper
                drawRect(
                    color = Color(0xFFFFD600),
                    topLeft = Offset(left + 4f, top + obsH - 8f),
                    size = Size(obsW - 8f, 6f)
                )
            }
            ObstacleType.POLICE -> {
                // Police Cruiser with flashing strobe siren
                drawRoundRect(
                    color = Color(0x55000000),
                    topLeft = Offset(left, top + 6f),
                    size = Size(obsW, obsH),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Body (black & white)
                drawRoundRect(
                    color = Color(0xFF212121),
                    topLeft = Offset(left, top),
                    size = Size(obsW, obsH),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // White roof and doors
                drawRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset(left + obsW * 0.08f, top + obsH * 0.28f),
                    size = Size(obsW * 0.84f, obsH * 0.44f)
                )
                // Windshield
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(left + obsW * 0.15f, top + obsH * 0.22f),
                    size = Size(obsW * 0.70f, obsH * 0.20f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Flashing Siren lightbar on roof
                val flashRed = (System.currentTimeMillis() / 120 % 2 == 0L)
                val sirenW = obsW * 0.50f
                val sirenH = 8f
                val sirenLeft = left + obsW * 0.25f
                val sirenTop = top + obsH * 0.46f
                drawRoundRect(
                    color = if (flashRed) Color(0xFFFF1744) else Color(0xFF00E5FF),
                    topLeft = Offset(sirenLeft, sirenTop),
                    size = Size(sirenW * 0.5f, sirenH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = if (!flashRed) Color(0xFFFF1744) else Color(0xFF00E5FF),
                    topLeft = Offset(sirenLeft + sirenW * 0.5f, sirenTop),
                    size = Size(sirenW * 0.5f, sirenH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            ObstacleType.ROAD_BLOCK -> {
                // Construction barrier / cones
                drawRoundRect(
                    color = Color(0xFFFF6D00),
                    topLeft = Offset(left, top),
                    size = Size(obsW, obsH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // White hazard stripes
                drawRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset(left + obsW * 0.25f, top + 4f),
                    size = Size(obsW * 0.2f, obsH - 8f)
                )
                drawRect(
                    color = Color(0xFFFFFFFF),
                    topLeft = Offset(left + obsW * 0.65f, top + 4f),
                    size = Size(obsW * 0.2f, obsH - 8f)
                )
            }
            ObstacleType.OIL_SLICK -> {
                // Dark iridescent oil puddle
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF263238), Color(0xFF4527A0), Color(0x00000000)),
                        center = Offset(obsScreenX, obs.y),
                        radius = obsW * 0.55f
                    ),
                    topLeft = Offset(left, top),
                    size = Size(obsW, obsH)
                )
            }
        }
    }
}

private fun DrawScope.drawCollectibles(collectibles: List<Collectible>, screenW: Float) {
    for (item in collectibles) {
        val itemScreenX = item.x * screenW
        val sizePx = item.sizeDp * (screenW / 400f).coerceIn(0.9f, 1.2f)
        val radius = sizePx * 0.5f

        when (item.type) {
            CollectibleType.COIN -> {
                // 3D Spinning Golden Coin
                val spinScale = kotlin.math.abs(cos(Math.toRadians(item.rotation.toDouble()))).toFloat().coerceAtLeast(0.2f)
                // Outer glow
                drawCircle(
                    color = Color(0x44FFD700),
                    radius = radius * 1.3f,
                    center = Offset(itemScreenX, item.y)
                )
                // Coin ellipse
                drawOval(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFEA00), Color(0xFFFFB300), Color(0xFFFF8F00)),
                        startX = itemScreenX - radius * spinScale,
                        endX = itemScreenX + radius * spinScale
                    ),
                    topLeft = Offset(itemScreenX - radius * spinScale, item.y - radius),
                    size = Size(radius * 2 * spinScale, radius * 2)
                )
                // Inner rim
                drawOval(
                    color = Color(0xFFFFF176),
                    topLeft = Offset(itemScreenX - radius * 0.6f * spinScale, item.y - radius * 0.6f),
                    size = Size(radius * 1.2f * spinScale, radius * 1.2f),
                    style = Stroke(width = 2f)
                )
            }
            CollectibleType.NITRO -> {
                // Blue Nitro Canister
                drawCircle(
                    color = Color(0x5500E5FF),
                    radius = radius * 1.3f,
                    center = Offset(itemScreenX, item.y)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF00E5FF), Color(0xFF0091EA)),
                        startY = item.y - radius,
                        endY = item.y + radius
                    ),
                    topLeft = Offset(itemScreenX - radius * 0.65f, item.y - radius),
                    size = Size(radius * 1.3f, radius * 2f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Lightning bolt or "N" symbol
                drawLine(
                    color = Color.White,
                    start = Offset(itemScreenX + 3f, item.y - radius * 0.6f),
                    end = Offset(itemScreenX - 3f, item.y),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(itemScreenX - 3f, item.y),
                    end = Offset(itemScreenX + 3f, item.y + radius * 0.6f),
                    strokeWidth = 3f
                )
            }
            CollectibleType.SHIELD -> {
                // Green Shield Orb
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFB2FF59), Color(0xFF76FF03), Color(0x3364DD17)),
                        center = Offset(itemScreenX, item.y),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(itemScreenX, item.y)
                )
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.7f,
                    center = Offset(itemScreenX, item.y),
                    style = Stroke(width = 2f)
                )
            }
            CollectibleType.MAGNET -> {
                // Red/Blue Horseshoe Magnet
                drawCircle(
                    color = Color(0x55FF4081),
                    radius = radius * 1.25f,
                    center = Offset(itemScreenX, item.y)
                )
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFF1744), Color(0xFF2979FF)),
                        startX = itemScreenX - radius,
                        endX = itemScreenX + radius
                    ),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(itemScreenX - radius * 0.8f, item.y - radius * 0.8f),
                    size = Size(radius * 1.6f, radius * 1.6f),
                    style = Stroke(width = 6f)
                )
            }
        }
    }
}

private fun DrawScope.drawParticles(particles: List<Particle>) {
    for (p in particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.size * alpha,
            center = Offset(p.x, p.y)
        )
    }
}

private fun DrawScope.drawSpeedStreaks(screenW: Float, screenH: Float, trackOffset: Float) {
    // Neon wind lines during Turbo
    val numLines = 8
    for (i in 0 until numLines) {
        val lineX = (screenW * 0.08f) + (i * screenW * 0.11f)
        val lineY = ((trackOffset * 2.2f + i * 180f) % (screenH + 200f)) - 100f
        val lineLen = 90f + (i % 3) * 40f
        drawLine(
            brush = Brush.verticalGradient(
                listOf(Color(0x0000E5FF), Color(0xCC00E5FF), Color(0x0000E5FF)),
                startY = lineY,
                endY = lineY + lineLen
            ),
            start = Offset(lineX, lineY),
            end = Offset(lineX, lineY + lineLen),
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawFloatingNotices(notices: List<FloatingNotice>) {
    val paint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = 42f
    }

    drawIntoCanvas { canvas ->
        for (notice in notices) {
            paint.color = android.graphics.Color.argb(
                (notice.alpha * 255).toInt(),
                (notice.color.red * 255).toInt(),
                (notice.color.green * 255).toInt(),
                (notice.color.blue * 255).toInt()
            )
            // Stroke shadow for visibility
            val strokePaint = Paint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = android.graphics.Color.argb((notice.alpha * 200).toInt(), 0, 0, 0)
            }
            canvas.nativeCanvas.drawText(notice.text, notice.x, notice.y, strokePaint)
            canvas.nativeCanvas.drawText(notice.text, notice.x, notice.y, paint)
        }
    }
}
