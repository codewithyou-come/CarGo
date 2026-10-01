package com.example.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSystem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GameUiState(
    val phase: GamePhase = GamePhase.START_MENU,
    val countdownNumber: Int = 3,
    val score: Int = 0,
    val distanceMeters: Float = 0f,
    val speedKmh: Float = 110f,
    val coinsThisRun: Int = 0,
    val totalCoins: Int = 0,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val nearMissCount: Int = 0,
    val turboActive: Boolean = false,
    val turboTimeRemaining: Float = 0f,
    val shieldActive: Boolean = false,
    val magnetActive: Boolean = false,
    val magnetTimeRemaining: Float = 0f,
    val invulnerabilityTimer: Float = 0f,
    val controlMode: ControlMode = ControlMode.TOUCH_DRAG,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val selectedCar: CarSkin = AVAILABLE_CARS[0],
    val unlockedCarIds: Set<String> = setOf("red_comet")
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = GamePreferences(application.applicationContext)
    val soundSystem = SoundSystem(preferences.soundEnabled)
    val hapticsManager = HapticsManager(application.applicationContext, preferences)

    private val _uiState = MutableStateFlow(
        GameUiState(
            totalCoins = preferences.totalCoins,
            highScore = preferences.highScore,
            soundEnabled = preferences.soundEnabled,
            hapticsEnabled = preferences.hapticsEnabled,
            controlMode = preferences.controlMode,
            selectedCar = AVAILABLE_CARS.find { it.id == preferences.selectedCarId } ?: AVAILABLE_CARS[0],
            unlockedCarIds = buildSet {
                add("red_comet")
                AVAILABLE_CARS.forEach {
                    if (preferences.isCarUnlocked(it.id)) add(it.id)
                }
            }
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Real-time game entities (accessed and updated on frame tick)
    var playerNormalizedX = 0.5f // 0.0 to 1.0
    var playerTargetX = 0.5f
    var playerSteerDirection = 0f // -1f for left, 1f for right, 0f for idle
    var screenWidthPx = 1080f
    var screenHeightPx = 1920f
    var trackOffset = 0f // road scrolling offset

    val obstacles = mutableListOf<Obstacle>()
    val collectibles = mutableListOf<Collectible>()
    val particles = mutableListOf<Particle>()
    val floatingNotices = mutableListOf<FloatingNotice>()

    private var gameLoopJob: Job? = null
    private var lastFrameTimeNanos = 0L
    private var nextObstacleSpawnDist = 20f
    private var nextCollectibleSpawnDist = 12f
    private var idCounter = 1L

    private val laneXs = floatArrayOf(0.14f, 0.38f, 0.62f, 0.86f)

    init {
        soundSystem.setSoundEnabled(preferences.soundEnabled)
    }

    fun onScreenSizeChanged(width: Float, height: Float) {
        screenWidthPx = width
        screenHeightPx = height
    }

    fun startCountdown() {
        soundSystem.playClickSound()
        hapticsManager.vibrateShort()

        resetGameplayState()
        _uiState.value = _uiState.value.copy(
            phase = GamePhase.COUNTDOWN,
            countdownNumber = 3
        )

        viewModelScope.launch {
            soundSystem.playCountdownBeep(isFinal = false)
            for (i in 3 downTo 1) {
                _uiState.value = _uiState.value.copy(countdownNumber = i)
                soundSystem.playCountdownBeep(isFinal = false)
                hapticsManager.vibrateShort()
                delay(800)
            }
            _uiState.value = _uiState.value.copy(countdownNumber = 0) // GO!
            soundSystem.playCountdownBeep(isFinal = true)
            hapticsManager.vibrateTurbo()
            delay(400)

            _uiState.value = _uiState.value.copy(phase = GamePhase.PLAYING)
            startGameLoop()
        }
    }

    fun pauseGame() {
        if (_uiState.value.phase == GamePhase.PLAYING) {
            soundSystem.playClickSound()
            hapticsManager.vibrateShort()
            _uiState.value = _uiState.value.copy(phase = GamePhase.PAUSED)
            gameLoopJob?.cancel()
            gameLoopJob = null
        }
    }

    fun resumeGame() {
        if (_uiState.value.phase == GamePhase.PAUSED) {
            soundSystem.playClickSound()
            hapticsManager.vibrateShort()
            _uiState.value = _uiState.value.copy(phase = GamePhase.PLAYING)
            startGameLoop()
        }
    }

    fun restartGame() {
        soundSystem.playClickSound()
        gameLoopJob?.cancel()
        gameLoopJob = null
        startCountdown()
    }

    fun quitToMenu() {
        soundSystem.playClickSound()
        gameLoopJob?.cancel()
        gameLoopJob = null
        _uiState.value = _uiState.value.copy(
            phase = GamePhase.START_MENU,
            totalCoins = preferences.totalCoins,
            highScore = preferences.highScore
        )
    }

    fun setPlayerTargetNormalizedX(targetX: Float) {
        playerTargetX = targetX.coerceIn(0.12f, 0.88f)
    }

    fun setSteerDirection(dir: Float) {
        playerSteerDirection = dir
    }

    fun tapLane(laneIndex: Int) {
        if (laneIndex in 0..3) {
            playerTargetX = laneXs[laneIndex]
            soundSystem.playClickSound()
            hapticsManager.vibrateShort()
        }
    }

    fun toggleSound() {
        val newSound = !_uiState.value.soundEnabled
        preferences.soundEnabled = newSound
        soundSystem.setSoundEnabled(newSound)
        _uiState.value = _uiState.value.copy(soundEnabled = newSound)
        if (newSound) soundSystem.playClickSound()
    }

    fun toggleHaptics() {
        val newHaptics = !_uiState.value.hapticsEnabled
        preferences.hapticsEnabled = newHaptics
        _uiState.value = _uiState.value.copy(hapticsEnabled = newHaptics)
        if (newHaptics) hapticsManager.vibrateShort()
    }

    fun setControlMode(mode: ControlMode) {
        preferences.controlMode = mode
        _uiState.value = _uiState.value.copy(controlMode = mode)
        soundSystem.playClickSound()
    }

    fun selectCar(car: CarSkin) {
        if (_uiState.value.unlockedCarIds.contains(car.id)) {
            preferences.selectedCarId = car.id
            _uiState.value = _uiState.value.copy(selectedCar = car)
            soundSystem.playClickSound()
            hapticsManager.vibrateShort()
        }
    }

    fun unlockCar(car: CarSkin) {
        if (preferences.unlockCar(car.id, car.unlockCost)) {
            preferences.selectedCarId = car.id
            val updated = _uiState.value.unlockedCarIds + car.id
            _uiState.value = _uiState.value.copy(
                totalCoins = preferences.totalCoins,
                unlockedCarIds = updated,
                selectedCar = car
            )
            soundSystem.playTurboSound()
            hapticsManager.vibrateTurbo()
        }
    }

    private fun resetGameplayState() {
        playerNormalizedX = 0.5f
        playerTargetX = 0.5f
        playerSteerDirection = 0f
        trackOffset = 0f
        obstacles.clear()
        collectibles.clear()
        particles.clear()
        floatingNotices.clear()
        nextObstacleSpawnDist = 25f
        nextCollectibleSpawnDist = 10f
        lastFrameTimeNanos = 0L

        _uiState.value = _uiState.value.copy(
            score = 0,
            distanceMeters = 0f,
            speedKmh = 110f * _uiState.value.selectedCar.speedMultiplier,
            coinsThisRun = 0,
            nearMissCount = 0,
            turboActive = false,
            turboTimeRemaining = 0f,
            shieldActive = false,
            magnetActive = false,
            magnetTimeRemaining = 0f,
            invulnerabilityTimer = 0f,
            isNewHighScore = false
        )
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        lastFrameTimeNanos = System.nanoTime()
        gameLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.phase == GamePhase.PLAYING) {
                val now = System.nanoTime()
                if (lastFrameTimeNanos == 0L) lastFrameTimeNanos = now
                val deltaSeconds = ((now - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastFrameTimeNanos = now

                updatePhysics(deltaSeconds)
                delay(16) // ~60fps target
            }
        }
    }

    private fun updatePhysics(dt: Float) {
        val state = _uiState.value
        val car = state.selectedCar

        // Base speed ramps up with distance
        val baseSpeed = (110f + (state.distanceMeters / 12f).coerceAtMost(160f)) * car.speedMultiplier
        val actualSpeed = if (state.turboActive) baseSpeed * 1.55f else baseSpeed
        val speedPxPerSec = actualSpeed * 7.5f

        // Distance covered
        val distCovered = (actualSpeed * 1000f / 3600f) * dt
        val newDistance = state.distanceMeters + distCovered
        val newScore = state.score + (distCovered * (if (state.turboActive) 3f else 1.5f)).toInt()

        // Track scroll offset
        trackOffset = (trackOffset + speedPxPerSec * dt) % 2000f

        // Player steering interpolation
        if (playerSteerDirection != 0f) {
            playerTargetX = (playerTargetX + playerSteerDirection * 0.95f * dt).coerceIn(0.12f, 0.88f)
        }
        val lerpFactor = (15f * dt).coerceIn(0f, 1f)
        playerNormalizedX += (playerTargetX - playerNormalizedX) * lerpFactor

        // Turbo timer
        var turboRemaining = state.turboTimeRemaining
        var isTurbo = state.turboActive
        if (isTurbo) {
            turboRemaining -= dt
            if (turboRemaining <= 0f) {
                isTurbo = false
                turboRemaining = 0f
            }
        }

        // Magnet timer
        var magnetRemaining = state.magnetTimeRemaining
        var isMagnet = state.magnetActive
        if (isMagnet) {
            magnetRemaining -= dt
            if (magnetRemaining <= 0f) {
                isMagnet = false
                magnetRemaining = 0f
            }
        }

        // Invulnerability timer
        var invulnTimer = (state.invulnerabilityTimer - dt).coerceAtLeast(0f)

        // Spawn engine/exhaust particles
        val playerScreenY = screenHeightPx * 0.78f
        val playerScreenX = playerNormalizedX * screenWidthPx
        if (Random.nextFloat() < (if (isTurbo) 0.9f else 0.45f)) {
            particles.add(
                Particle(
                    x = playerScreenX + (Random.nextFloat() - 0.5f) * 20f,
                    y = playerScreenY + 45f,
                    vx = (Random.nextFloat() - 0.5f) * 40f,
                    vy = 120f + (if (isTurbo) 250f else 60f),
                    color = if (isTurbo) Color(0xFF00E5FF) else Color(0xFFFF5722),
                    size = if (isTurbo) 9f else 6f,
                    life = 1f,
                    maxLife = if (isTurbo) 0.35f else 0.25f
                )
            )
        }

        // Update existing particles
        val particleIter = particles.iterator()
        while (particleIter.hasNext()) {
            val p = particleIter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt / p.maxLife
            if (p.life <= 0f) particleIter.remove()
        }

        // Update floating notices
        val noticeIter = floatingNotices.iterator()
        while (noticeIter.hasNext()) {
            val n = noticeIter.next()
            n.y -= 70f * dt
            n.alpha = (n.duration / 1.2f).coerceIn(0f, 1f)
            n.duration -= dt
            if (n.duration <= 0f) noticeIter.remove()
        }

        // Spawning obstacles
        nextObstacleSpawnDist -= distCovered
        if (nextObstacleSpawnDist <= 0f) {
            spawnObstacleWave()
            // Next spawn spacing depends on speed to guarantee playable gaps
            val minSpacing = (24f - (actualSpeed / 40f)).coerceAtLeast(14f)
            nextObstacleSpawnDist = minSpacing + Random.nextFloat() * 12f
        }

        // Spawning collectibles (coins & powerups)
        nextCollectibleSpawnDist -= distCovered
        if (nextCollectibleSpawnDist <= 0f) {
            spawnCollectible()
            nextCollectibleSpawnDist = 8f + Random.nextFloat() * 10f
        }

        // Player hitbox in screen coordinates
        val pCarWidth = 52f * (screenWidthPx / 400f).coerceIn(0.9f, 1.3f)
        val pCarHeight = 88f * (screenHeightPx / 800f).coerceIn(0.9f, 1.3f)
        val pLeft = playerScreenX - pCarWidth * 0.42f
        val pRight = playerScreenX + pCarWidth * 0.42f
        val pTop = playerScreenY - pCarHeight * 0.42f
        val pBottom = playerScreenY + pCarHeight * 0.42f

        var newCoins = state.coinsThisRun
        var additionalScore = 0
        var nearMisses = state.nearMissCount
        var shieldState = state.shieldActive
        var crashed = false

        // Update obstacles
        val obstacleIter = obstacles.iterator()
        while (obstacleIter.hasNext()) {
            val obs = obstacleIter.next()

            // Obstacle relative movement
            val obsDownSpeed = speedPxPerSec * (1f - obs.speedFactor * 0.5f)
            obs.y += obsDownSpeed * dt

            // Sports cars slowly change lanes
            if (obs.type == ObstacleType.SPORTS) {
                obs.x += (obs.targetX - obs.x) * 2f * dt
            }

            val obsScreenX = obs.x * screenWidthPx
            val obsW = obs.widthDp * (screenWidthPx / 400f).coerceIn(0.9f, 1.3f)
            val obsH = obs.heightDp * (screenHeightPx / 800f).coerceIn(0.9f, 1.3f)
            val obsLeft = obsScreenX - obsW * 0.42f
            val obsRight = obsScreenX + obsW * 0.42f
            val obsTop = obs.y - obsH * 0.42f
            val obsBottom = obs.y + obsH * 0.42f

            // Collision check
            val isColliding = pLeft < obsRight && pRight > obsLeft && pTop < obsBottom && pBottom > obsTop

            if (isColliding && invulnTimer <= 0f) {
                if (isTurbo) {
                    // Turbo destroys obstacles
                    spawnExplosion(obsScreenX, obs.y, Color(0xFF00E5FF))
                    additionalScore += 200
                    addFloatingNotice("+200 SMASH!", obsScreenX, obs.y, Color(0xFF00E5FF))
                    soundSystem.playCrashSound()
                    hapticsManager.vibrateCrash()
                    obstacleIter.remove()
                    continue
                } else if (shieldState) {
                    // Shield protects once
                    shieldState = false
                    invulnTimer = 1.5f
                    spawnExplosion(obsScreenX, obs.y, Color(0xFFFFD700))
                    addFloatingNotice("SHIELD BROKEN!", playerScreenX, playerScreenY - 60f, Color(0xFFFFD700))
                    soundSystem.playShieldSound()
                    hapticsManager.vibrateCrash()
                    obstacleIter.remove()
                    continue
                } else if (obs.type == ObstacleType.OIL_SLICK) {
                    // Oil slick spins car, doesn't kill immediately
                    playerTargetX = (playerTargetX + (if (Random.nextBoolean()) 0.22f else -0.22f)).coerceIn(0.12f, 0.88f)
                    soundSystem.playNearMissSound()
                    hapticsManager.vibrateShort()
                    addFloatingNotice("OIL SLICK!", obsScreenX, obs.y, Color(0xFFFF9800))
                    obstacleIter.remove()
                    continue
                } else {
                    // Game Over Crash!
                    crashed = true
                    spawnExplosion(playerScreenX, playerScreenY, Color(0xFFFF3D00))
                    soundSystem.playCrashSound()
                    hapticsManager.vibrateCrash()
                    break
                }
            }

            // Near miss detection (overtaking within close horizontal distance)
            if (!obs.nearMissAwarded && !obs.passed && obs.y > playerScreenY && obs.y - obsDownSpeed * dt <= playerScreenY) {
                val horizontalDiff = abs(playerNormalizedX - obs.x)
                if (horizontalDiff in 0.08f..0.22f) {
                    obs.nearMissAwarded = true
                    nearMisses++
                    additionalScore += 150
                    addFloatingNotice("+150 NEAR MISS!", playerScreenX, playerScreenY - 50f, Color(0xFFFFD700))
                    soundSystem.playNearMissSound()
                    hapticsManager.vibrateNearMiss()
                }
            }

            if (obs.y > screenHeightPx + 150f) {
                obstacleIter.remove()
            }
        }

        if (crashed) {
            handleGameOver(newScore + additionalScore, newDistance, newCoins, nearMisses)
            return
        }

        // Update collectibles
        val collectIter = collectibles.iterator()
        while (collectIter.hasNext()) {
            val item = collectIter.next()
            item.rotation += 180f * dt

            // Magnet effect: pull toward player
            if (isMagnet && item.type == CollectibleType.COIN) {
                val dx = playerNormalizedX - item.x
                val dy = playerScreenY - item.y
                val dist = kotlin.math.sqrt(dx * dx * screenWidthPx * screenWidthPx + dy * dy)
                if (dist < screenHeightPx * 0.45f) {
                    item.x += dx * 6f * dt
                    item.y += dy * 6f * dt
                }
            }

            // Fall down screen with road speed
            item.y += speedPxPerSec * dt

            val itemScreenX = item.x * screenWidthPx
            val itemScreenY = item.y
            val distToCar = kotlin.math.hypot(itemScreenX - playerScreenX, itemScreenY - playerScreenY)

            if (distToCar < (pCarWidth * 0.65f)) {
                // Collect item!
                when (item.type) {
                    CollectibleType.COIN -> {
                        newCoins++
                        additionalScore += 50
                        addFloatingNotice("+50", itemScreenX, itemScreenY, Color(0xFFFFD700))
                        soundSystem.playCoinSound()
                        hapticsManager.vibrateCoin()
                        spawnCoinSparkles(itemScreenX, itemScreenY)
                    }
                    CollectibleType.NITRO -> {
                        isTurbo = true
                        turboRemaining = 4.5f
                        additionalScore += 100
                        addFloatingNotice("TURBO BOOST!", itemScreenX, itemScreenY, Color(0xFF00E5FF))
                        soundSystem.playTurboSound()
                        hapticsManager.vibrateTurbo()
                    }
                    CollectibleType.SHIELD -> {
                        shieldState = true
                        addFloatingNotice("SHIELD ACTIVATED!", itemScreenX, itemScreenY, Color(0xFF76FF03))
                        soundSystem.playShieldSound()
                        hapticsManager.vibrateShort()
                    }
                    CollectibleType.MAGNET -> {
                        isMagnet = true
                        magnetRemaining = 6.0f
                        addFloatingNotice("MAGNET ON!", itemScreenX, itemScreenY, Color(0xFFFF4081))
                        soundSystem.playTurboSound()
                        hapticsManager.vibrateShort()
                    }
                }
                collectIter.remove()
            } else if (item.y > screenHeightPx + 100f) {
                collectIter.remove()
            }
        }

        _uiState.value = _uiState.value.copy(
            score = newScore + additionalScore,
            distanceMeters = newDistance,
            speedKmh = actualSpeed,
            coinsThisRun = newCoins,
            nearMissCount = nearMisses,
            turboActive = isTurbo,
            turboTimeRemaining = turboRemaining,
            shieldActive = shieldState,
            magnetActive = isMagnet,
            magnetTimeRemaining = magnetRemaining,
            invulnerabilityTimer = invulnTimer
        )
    }

    private fun handleGameOver(finalScore: Int, finalDist: Float, coins: Int, nearMisses: Int) {
        gameLoopJob?.cancel()
        gameLoopJob = null

        val isNewHigh = finalScore > preferences.highScore
        if (isNewHigh) {
            preferences.highScore = finalScore
        }
        preferences.totalCoins += coins

        soundSystem.playGameOverSound()

        _uiState.value = _uiState.value.copy(
            phase = GamePhase.GAME_OVER,
            score = finalScore,
            distanceMeters = finalDist,
            coinsThisRun = coins,
            totalCoins = preferences.totalCoins,
            highScore = preferences.highScore,
            isNewHighScore = isNewHigh,
            nearMissCount = nearMisses,
            turboActive = false,
            shieldActive = false,
            magnetActive = false
        )
    }

    private fun spawnObstacleWave() {
        val freeLane = Random.nextInt(4) // ensure at least 1 lane is open
        val numObstacles = if (Random.nextFloat() < 0.65f) 2 else 1

        val availableLanes = (0..3).filter { it != freeLane }.shuffled()
        for (i in 0 until minOf(numObstacles, availableLanes.size)) {
            val lane = availableLanes[i]
            val randType = Random.nextFloat()
            val (type, w, h, speedFactor, color) = when {
                randType < 0.35f -> ObstacleTuple(ObstacleType.SEDAN, 50f, 85f, 0.45f, Color(0xFF1E88E5))
                randType < 0.58f -> ObstacleTuple(ObstacleType.TRUCK, 60f, 150f, 0.30f, Color(0xFF795548))
                randType < 0.76f -> ObstacleTuple(ObstacleType.POLICE, 50f, 88f, 0.65f, Color(0xFF263238))
                randType < 0.88f -> ObstacleTuple(ObstacleType.SPORTS, 48f, 82f, 0.55f, Color(0xFFE91E63))
                randType < 0.95f -> ObstacleTuple(ObstacleType.ROAD_BLOCK, 48f, 32f, 0f, Color(0xFFFF9800))
                else -> ObstacleTuple(ObstacleType.OIL_SLICK, 52f, 40f, 0f, Color(0xFF212121))
            }

            val targetLaneX = if (type == ObstacleType.SPORTS && Random.nextBoolean()) {
                val shift = if (lane > 0) lane - 1 else lane + 1
                laneXs[shift]
            } else {
                laneXs[lane]
            }

            obstacles.add(
                Obstacle(
                    id = idCounter++,
                    type = type,
                    x = laneXs[lane],
                    y = -100f - (i * 90f),
                    widthDp = w,
                    heightDp = h,
                    speedFactor = speedFactor,
                    color = color,
                    lane = lane,
                    targetX = targetLaneX
                )
            )
        }
    }

    private data class ObstacleTuple(
        val type: ObstacleType,
        val width: Float,
        val height: Float,
        val speedFactor: Float,
        val color: Color
    )

    private fun spawnCollectible() {
        val lane = Random.nextInt(4)
        val rand = Random.nextFloat()
        val type = when {
            rand < 0.72f -> CollectibleType.COIN
            rand < 0.84f -> CollectibleType.NITRO
            rand < 0.93f -> CollectibleType.SHIELD
            else -> CollectibleType.MAGNET
        }

        if (type == CollectibleType.COIN && Random.nextFloat() < 0.5f) {
            // Spawn coin trail of 3
            for (i in 0 until 3) {
                collectibles.add(
                    Collectible(
                        id = idCounter++,
                        type = CollectibleType.COIN,
                        x = laneXs[lane],
                        y = -60f - (i * 65f)
                    )
                )
            }
        } else {
            collectibles.add(
                Collectible(
                    id = idCounter++,
                    type = type,
                    x = laneXs[lane],
                    y = -60f
                )
            )
        }
    }

    private fun spawnExplosion(x: Float, y: Float, color: Color) {
        for (i in 0 until 28) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 80f + Random.nextFloat() * 260f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = if (i % 2 == 0) color else Color(0xFFFFEB3B),
                    size = 5f + Random.nextFloat() * 8f,
                    life = 1f,
                    maxLife = 0.5f + Random.nextFloat() * 0.4f
                )
            )
        }
    }

    private fun spawnCoinSparkles(x: Float, y: Float) {
        for (i in 0 until 10) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 40f + Random.nextFloat() * 120f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = Color(0xFFFFD700),
                    size = 4f + Random.nextFloat() * 4f,
                    life = 1f,
                    maxLife = 0.35f
                )
            )
        }
    }

    private fun addFloatingNotice(text: String, x: Float, y: Float, color: Color) {
        floatingNotices.add(
            FloatingNotice(
                id = idCounter++,
                text = text,
                x = x,
                y = y,
                color = color
            )
        )
    }
}
