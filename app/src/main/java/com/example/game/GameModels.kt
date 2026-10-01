package com.example.game

import androidx.compose.ui.graphics.Color

enum class GamePhase {
    START_MENU,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class ControlMode(val displayName: String) {
    TOUCH_DRAG("Touch Drag"),
    BUTTONS("Steer Buttons"),
    LANE_TAP("Tap Lanes")
}

data class CarSkin(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val accentColor: Color,
    val stripeColor: Color,
    val speedMultiplier: Float = 1.0f,
    val unlockCost: Int = 0
)

val AVAILABLE_CARS = listOf(
    CarSkin(
        id = "red_comet",
        name = "Red Comet",
        primaryColor = Color(0xFFE53935),
        accentColor = Color(0xFFB71C1C),
        stripeColor = Color(0xFFFFFFFF),
        speedMultiplier = 1.0f,
        unlockCost = 0
    ),
    CarSkin(
        id = "neon_cyber",
        name = "Neon Cyber",
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF0091EA),
        stripeColor = Color(0xFFFF007F),
        speedMultiplier = 1.05f,
        unlockCost = 30
    ),
    CarSkin(
        id = "toxic_viper",
        name = "Toxic Viper",
        primaryColor = Color(0xFF00E676),
        accentColor = Color(0xFF1B5E20),
        stripeColor = Color(0xFF212121),
        speedMultiplier = 1.10f,
        unlockCost = 80
    ),
    CarSkin(
        id = "golden_titan",
        name = "Golden Titan",
        primaryColor = Color(0xFFFFD700),
        accentColor = Color(0xFFFF8F00),
        stripeColor = Color(0xFF1A1A1A),
        speedMultiplier = 1.15f,
        unlockCost = 150
    ),
    CarSkin(
        id = "hyper_pink",
        name = "Hyper Beast",
        primaryColor = Color(0xFFFF1493),
        accentColor = Color(0xFF880E4F),
        stripeColor = Color(0xFF00FFFF),
        speedMultiplier = 1.20f,
        unlockCost = 250
    )
)

enum class ObstacleType {
    SEDAN,
    TRUCK,
    POLICE,
    SPORTS,
    ROAD_BLOCK,
    OIL_SLICK
}

data class Obstacle(
    val id: Long,
    val type: ObstacleType,
    var x: Float, // Normalized track X: 0.0 to 1.0
    var y: Float, // Screen Y in pixels
    val widthDp: Float,
    val heightDp: Float,
    val speedFactor: Float, // relative movement speed
    val color: Color,
    val lane: Int,
    var passed: Boolean = false,
    var nearMissAwarded: Boolean = false,
    var targetX: Float = x
)

enum class CollectibleType {
    COIN,
    NITRO,
    SHIELD,
    MAGNET
}

data class Collectible(
    val id: Long,
    val type: CollectibleType,
    var x: Float, // Normalized track X: 0.0 to 1.0
    var y: Float, // Screen Y in pixels
    val sizeDp: Float = 28f,
    var rotation: Float = 0f,
    var collected: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var life: Float = 1.0f,
    val maxLife: Float = 1.0f
)

data class FloatingNotice(
    val id: Long,
    val text: String,
    val x: Float,
    var y: Float,
    val color: Color,
    var alpha: Float = 1f,
    var scale: Float = 1f,
    var duration: Float = 1.2f
)
