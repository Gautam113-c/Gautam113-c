package com.example.game

import com.example.audio.SoundManager
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(
    private val soundManager: SoundManager? = null,
    var outfitColor: Long = 0xFFFF9933,
    var accessoryType: String = "acc_glasses"
) {
    // Game loop state
    var isRunning: Boolean = false
    var isPaused: Boolean = false
    var isGameOver: Boolean = false

    // Scoring & Progression
    var score: Int = 0
    var coinsCollected: Int = 0
    var distanceMeters: Float = 0f
    var baseSpeed: Float = 14.0f // World units per second
    var currentSpeed: Float = 14.0f
    var scoreMultiplier: Int = 1

    // Environment
    var currentEnvironment: GameEnvironment = GameEnvironment.INDIAN_CITY

    // Player State
    var targetLane: Int = 0 // -1: Left, 0: Center, 1: Right
    var playerLaneX: Float = 0f // Smooth interpolated X position (-1.0 to 1.0)
    var playerY: Float = 0f // Vertical height (0 = ground, > 0 = jumping)
    var playerVelocityY: Float = 0f
    var isJumping: Boolean = false
    var isSliding: Boolean = false
    var slideRemainingTime: Float = 0f
    var stumbleRemainingTime: Float = 0f

    // Animations & Visuals
    var runCycleTime: Float = 0f
    var playerRotationZ: Float = 0f // Slight bank when switching lanes

    // Chaser State (Cartoon Rahul-inspired character)
    var chaserDistance: Float = 9.0f // Behind player in units
    var chaserLaneX: Float = 0f
    var chaserWavingStickAngle: Float = 0f
    var chaserReactionText: String? = null

    // Power-ups active map: type -> remaining time
    val activePowerUps = mutableMapOf<PowerUpType, Float>()

    // Entities in world space
    val obstacles = mutableListOf<ObstacleInstance>()
    val coins = mutableListOf<CoinInstance>()
    val powerUps = mutableListOf<PowerUpInstance>()

    // Active Speech Bubble
    var activeSpeech: SpeechBubble? = null

    // Spawning markers
    private var nextSpawnZ: Float = 25f
    private var entityIdCounter: Long = 1L
    private val random = Random(System.currentTimeMillis())

    // Cartoon Voice Lines Pool
    private val startVoiceLines = listOf(
        "Mitron! Full speed sprint!",
        "3... 2... 1... Vikas Express chali!",
        "Mitron, catch me if you can!",
        "Chalo chalo, no stopping today!"
    )
    private val jumpVoiceLines = listOf(
        "Hup! Clear!",
        "Super Jump! Wah!",
        "Mitron, jumping over roadblocks!",
        "56-inch jump!",
        "Flying high!"
    )
    private val slideVoiceLines = listOf(
        "Duck and slide! Smooth!",
        "Under the banner! Shhh!",
        "Arey bhai, sliding like butter!",
        "Down we go! Perfect slide!"
    )
    private val nearMissVoiceLines = listOf(
        "Arey baap re! That was close!",
        "Bach gaye! Narrow escape!",
        "Reflexes at 100%!",
        "Phew! Not today!",
        "Arey sambhal ke!"
    )
    private val coinMilestoneLines = listOf(
        "Paisa hi paisa hoga!",
        "Balle balle! Khazana mil gaya!",
        "Economic growth in full swing!",
        "Cha-ching! 20 coins collected!"
    )
    private val chaserTauntLines = listOf(
        "Arey ruko bhai! Stop running!",
        "Wait for me! Why so fast?!",
        "Khatam... Tata... Bye bye!",
        "Pakad loonga aaj toh!",
        "Just wait one second!",
        "Give me a chance to catch up!"
    )
    private val runnerReplyLines = listOf(
        "Mitron, can't catch me!",
        "Vikas Express at full speed!",
        "Catch me if you can, Bhai!",
        "Too slow! Run faster!"
    )
    private val chaserSurgeLines = listOf(
        "Almost gotcha! Ha-ha!",
        "I'm right behind you!",
        "Nowhere to run now!"
    )
    private val gameOverRunnerLines = listOf(
        "Arey gir gaya! What a roadblock!",
        "Ouch! We need better road work!",
        "Mitron, time to restart the mission!"
    )
    private val gameOverChaserLines = listOf(
        "Khatam! Tata! Bye-bye!",
        "Gotcha! Finally caught you!",
        "That was a hilarious crash!"
    )

    // Banter timers
    private var chaserTauntTimer: Float = 0f
    private var nextChaserTauntInterval: Float = 8.0f
    private var runnerReplyTimer: Float = 0f
    private var pendingRunnerReply: String? = null
    private var gameOverChaserDelayTimer: Float = 0f
    private var isGameOverChaserTriggered: Boolean = false

    fun startNewGame() {
        isRunning = true
        isPaused = false
        isGameOver = false

        score = 0
        coinsCollected = 0
        distanceMeters = 0f
        baseSpeed = 14.0f
        currentSpeed = 14.0f
        scoreMultiplier = 1

        currentEnvironment = GameEnvironment.INDIAN_CITY

        targetLane = 0
        playerLaneX = 0f
        playerY = 0f
        playerVelocityY = 0f
        isJumping = false
        isSliding = false
        slideRemainingTime = 0f
        stumbleRemainingTime = 0f

        runCycleTime = 0f
        playerRotationZ = 0f

        chaserDistance = 8.5f
        chaserLaneX = 0f
        chaserWavingStickAngle = 0f
        chaserReactionText = null

        chaserTauntTimer = 0f
        nextChaserTauntInterval = 7.0f + random.nextFloat() * 4.0f
        runnerReplyTimer = 0f
        pendingRunnerReply = null
        gameOverChaserDelayTimer = 0f
        isGameOverChaserTriggered = false

        activePowerUps.clear()
        obstacles.clear()
        coins.clear()
        powerUps.clear()

        nextSpawnZ = 22f
        entityIdCounter = 1L

        // Initial pre-spawns
        for (i in 0 until 5) {
            spawnTrackSlice(nextSpawnZ)
            nextSpawnZ += random.nextFloat() * 8f + 16f
        }

        triggerSpeech(startVoiceLines.random(random), isChaser = false, vocalType = FunnyVocalType.RUNNER_CHEER)
        soundManager?.startBackgroundMusic()
    }

    fun update(deltaTime: Float) {
        if (!isRunning || isPaused || isGameOver) return

        // Time capping for stability
        val dt = min(deltaTime, 0.05f)

        // Handle Active Power-ups
        updatePowerUps(dt)

        // Speed ramp up smoothly over distance
        val targetSpeed = if (activePowerUps.containsKey(PowerUpType.SPEED_BOOST)) {
            baseSpeed * 1.55f
        } else {
            baseSpeed
        }
        currentSpeed += (targetSpeed - currentSpeed) * 4f * dt

        // Gradually increase base speed up to 32 units/s
        baseSpeed = min(32f, 14.0f + (distanceMeters / 150f))

        // Distance & Score progression
        val distanceThisFrame = currentSpeed * dt
        distanceMeters += distanceThisFrame
        scoreMultiplier = if (activePowerUps.containsKey(PowerUpType.DOUBLE_COINS)) 2 else 1
        score += (distanceThisFrame * 8f * scoreMultiplier).toInt()

        // Update environment based on score
        val newEnv = GameEnvironment.forScore(score)
        if (newEnv != currentEnvironment) {
            currentEnvironment = newEnv
            triggerSpeech("Welcome to ${newEnv.title}!", isChaser = false, vocalType = FunnyVocalType.RUNNER_CHEER)
        }

        // Run cycle animation
        runCycleTime += dt * (currentSpeed * 0.75f)

        // Player Lane smooth interpolation
        val targetX = targetLane.toFloat()
        val laneDiff = targetX - playerLaneX
        playerLaneX += laneDiff * 14f * dt
        playerRotationZ = -laneDiff * 15f // Bank angle

        // Jump physics
        val superJumpActive = activePowerUps.containsKey(PowerUpType.SUPER_JUMP)
        val gravity = if (superJumpActive) 28f else 38f

        if (isJumping) {
            playerY += playerVelocityY * dt
            playerVelocityY -= gravity * dt
            if (playerY <= 0f) {
                playerY = 0f
                playerVelocityY = 0f
                isJumping = false
            }
        }

        // Slide timer
        if (isSliding) {
            slideRemainingTime -= dt
            if (slideRemainingTime <= 0f) {
                isSliding = false
            }
        }

        // Stumble recovery
        if (stumbleRemainingTime > 0f) {
            stumbleRemainingTime -= dt
        }

        // Chaser updates
        updateChaser(dt)

        // Banter updates
        updateBanter(dt)

        // World Objects Update (Obstacles, Coins, PowerUps)
        updateEntities(dt, distanceThisFrame)

        // Speech bubble timers
        activeSpeech?.let { speech ->
            speech.remainingSeconds -= dt
            if (speech.remainingSeconds <= 0f) {
                activeSpeech = null
            }
        }

        // Continuous Spawner
        if (nextSpawnZ < 90f) {
            spawnTrackSlice(nextSpawnZ)
            val gap = max(14f, 24f - (distanceMeters / 250f))
            nextSpawnZ += gap + random.nextFloat() * 6f
        }
    }

    private fun updatePowerUps(dt: Float) {
        val expired = mutableListOf<PowerUpType>()
        for ((type, remaining) in activePowerUps) {
            val newRemaining = remaining - dt
            if (newRemaining <= 0f) {
                expired.add(type)
            } else {
                activePowerUps[type] = newRemaining
            }
        }
        for (exp in expired) {
            activePowerUps.remove(exp)
        }
    }

    private fun updateChaser(dt: Float) {
        // Chaser follows player's lane with a slight lag
        chaserLaneX += (playerLaneX - chaserLaneX) * 7f * dt
        chaserWavingStickAngle = (chaserWavingStickAngle + dt * 10f) % (2f * Math.PI.toFloat())

        // If player stumbles, chaser surges closer!
        val targetChaserDist = when {
            activePowerUps.containsKey(PowerUpType.SPEED_BOOST) -> 13f
            stumbleRemainingTime > 0f -> 4.2f
            else -> 8.2f - min(2.5f, distanceMeters / 600f)
        }
        chaserDistance += (targetChaserDist - chaserDistance) * 3f * dt

        // Chaser surges close reaction
        if (stumbleRemainingTime > 0.85f && random.nextInt(10) < 4) {
            triggerSpeech(chaserSurgeLines.random(random), isChaser = true, vocalType = FunnyVocalType.CHASER_LAUGH)
        }
    }

    private fun updateBanter(dt: Float) {
        if (isGameOver) {
            if (!isGameOverChaserTriggered) {
                gameOverChaserDelayTimer -= dt
                if (gameOverChaserDelayTimer <= 0f) {
                    isGameOverChaserTriggered = true
                    val chaserOver = gameOverChaserLines.random(random)
                    triggerSpeech(chaserOver, isChaser = true, vocalType = FunnyVocalType.CHASER_LAUGH)
                }
            }
            return
        }

        // Chaser periodic funny banter during the run
        chaserTauntTimer += dt
        if (chaserTauntTimer >= nextChaserTauntInterval) {
            chaserTauntTimer = 0f
            nextChaserTauntInterval = 7.5f + random.nextFloat() * 4.5f
            val taunt = chaserTauntLines.random(random)
            triggerSpeech(taunt, isChaser = true, vocalType = FunnyVocalType.CHASER_SHOUT)

            // Runner answers back playfully
            if (random.nextBoolean()) {
                pendingRunnerReply = runnerReplyLines.random(random)
                runnerReplyTimer = 2.1f
            }
        }

        if (pendingRunnerReply != null) {
            runnerReplyTimer -= dt
            if (runnerReplyTimer <= 0f) {
                val reply = pendingRunnerReply!!
                pendingRunnerReply = null
                triggerSpeech(reply, isChaser = false, vocalType = FunnyVocalType.RUNNER_CHEER)
            }
        }
    }

    private fun updateEntities(dt: Float, distanceThisFrame: Float) {
        // Move all objects toward player (Z decreases)
        nextSpawnZ -= distanceThisFrame

        // 1. OBSTACLES
        val obstacleIterator = obstacles.iterator()
        while (obstacleIterator.hasNext()) {
            val obs = obstacleIterator.next()
            obs.z -= distanceThisFrame

            // Check collision when obstacle is at player's Z (approx z between -0.4 and 0.9)
            if (obs.z in -0.4f..0.9f && !obs.isCleared) {
                val laneMatches = kotlin.math.abs(playerLaneX - obs.lane.toFloat()) < 0.65f
                if (laneMatches) {
                    val isSafe = when {
                        // Shield or speed boost protects!
                        activePowerUps.containsKey(PowerUpType.SPEED_BOOST) -> {
                            true // Smash through!
                        }
                        activePowerUps.containsKey(PowerUpType.SHIELD) -> {
                            activePowerUps.remove(PowerUpType.SHIELD)
                            soundManager?.playShieldHitSound()
                            triggerSpeech("Kavach saved me! Bulletproof!", isChaser = false, vocalType = FunnyVocalType.RUNNER_CHEER)
                            stumbleRemainingTime = 1.0f
                            true
                        }
                        obs.type.canJumpOver && (playerY > 0.85f || (obs.type == ObstacleType.POTHOLE && playerY > 0.3f)) -> {
                            true // Successfully jumped!
                        }
                        obs.type.canSlideUnder && isSliding -> {
                            true // Successfully slid under!
                        }
                        else -> {
                            false // COLLISION!
                        }
                    }

                    if (!isSafe) {
                        triggerGameOver()
                        return
                    }
                }
            }

            // Near miss check when obstacle is passed safely
            if (obs.z in -1.2f..-0.2f && !obs.isCleared) {
                val laneDiff = kotlin.math.abs(playerLaneX - obs.lane.toFloat())
                if (laneDiff in 0.5f..1.25f && random.nextInt(10) < 4) {
                    triggerSpeech(nearMissVoiceLines.random(random), isChaser = false, vocalType = FunnyVocalType.RUNNER_TALK)
                }
            }

            // Remove passed obstacles
            if (obs.z < -6f) {
                obstacleIterator.remove()
            }
        }

        // 2. COINS
        val magnetActive = activePowerUps.containsKey(PowerUpType.COIN_MAGNET)
        val coinIterator = coins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            coin.z -= distanceThisFrame

            // Magnet attraction
            if (magnetActive && coin.z in 0f..14f && !coin.isCollected) {
                val dx = playerLaneX - coin.lane.toFloat()
                coin.lane = if (playerLaneX < -0.3f) -1 else if (playerLaneX > 0.3f) 1 else 0
                coin.z -= 8f * dt
                coin.attractProgress = min(1f, coin.attractProgress + dt * 4f)
            }

            // Collection check (player is at z ≈ 0, laneX matches)
            if (coin.z in -0.5f..0.8f && !coin.isCollected) {
                val laneMatches = kotlin.math.abs(playerLaneX - coin.lane.toFloat()) < 0.7f
                val heightMatches = playerY < 2.5f
                if (laneMatches && heightMatches) {
                    coin.isCollected = true
                    val amount = 1 * scoreMultiplier
                    coinsCollected += amount
                    score += 50 * scoreMultiplier
                    soundManager?.playCoinSound()

                    if (coinsCollected > 0 && coinsCollected % 20 == 0) {
                        triggerSpeech(coinMilestoneLines.random(random), isChaser = false, vocalType = FunnyVocalType.CHA_CHING)
                    }
                }
            }

            if (coin.z < -5f || coin.isCollected) {
                coinIterator.remove()
            }
        }

        // 3. POWER-UPS
        val pUpIterator = powerUps.iterator()
        while (pUpIterator.hasNext()) {
            val pUp = pUpIterator.next()
            pUp.z -= distanceThisFrame

            if (pUp.z in -0.5f..0.8f && !pUp.isCollected) {
                val laneMatches = kotlin.math.abs(playerLaneX - pUp.lane.toFloat()) < 0.75f
                if (laneMatches) {
                    pUp.isCollected = true
                    activePowerUps[pUp.type] = pUp.type.durationSeconds
                    soundManager?.playPowerUpSound()
                    val pLine = when (pUp.type) {
                        PowerUpType.SHIELD -> "Kavach activated! Bulletproof!"
                        PowerUpType.COIN_MAGNET -> "Paisa chumbak! Attracting all coins!"
                        PowerUpType.SPEED_BOOST -> "Bullet train turbo speed! Vrooom!"
                        PowerUpType.DOUBLE_COINS -> "Double economy! 2X coins multiplier!"
                        PowerUpType.SUPER_JUMP -> "Rocket jump to the moon!"
                    }
                    triggerSpeech(pLine, isChaser = false, vocalType = FunnyVocalType.POWERUP_FANFARE)
                }
            }

            if (pUp.z < -5f || pUp.isCollected) {
                pUpIterator.remove()
            }
        }
    }

    private fun spawnTrackSlice(zPos: Float) {
        val patternChoice = random.nextInt(100)

        // Decide obstacle layout
        when {
            patternChoice < 25 -> {
                // Single lane obstacle + coins on other lanes
                val obsLane = random.nextInt(3) - 1 // -1, 0, 1
                val obsType = listOf(
                    ObstacleType.TRAFFIC_BARRICADE,
                    ObstacleType.ROAD_CONES,
                    ObstacleType.WOODEN_BOX,
                    ObstacleType.PARK_BENCH
                ).random()
                obstacles.add(ObstacleInstance(entityIdCounter++, obsType, obsLane, zPos))

                // Coins on empty lane
                val coinLane = if (obsLane == 0) (if (random.nextBoolean()) -1 else 1) else 0
                for (k in 0..3) {
                    coins.add(CoinInstance(entityIdCounter++, coinLane, zPos + k * 2.2f))
                }
            }
            patternChoice < 50 -> {
                // Overhead banner (requires slide!)
                val obsLane = random.nextInt(3) - 1
                obstacles.add(ObstacleInstance(entityIdCounter++, ObstacleType.OVERHEAD_BANNER, obsLane, zPos))
                // Coins at low height under the banner
                for (k in 0..2) {
                    coins.add(CoinInstance(entityIdCounter++, obsLane, zPos + k * 1.8f, y = 0.2f))
                }
            }
            patternChoice < 70 -> {
                // Pothole or low wall (jumpable) with high coins
                val obsLane = random.nextInt(3) - 1
                val obsType = if (random.nextBoolean()) ObstacleType.POTHOLE else ObstacleType.SMALL_WALL
                obstacles.add(ObstacleInstance(entityIdCounter++, obsType, obsLane, zPos))
                for (k in 0..3) {
                    val coinY = if (k == 1 || k == 2) 1.5f else 0.4f
                    coins.add(CoinInstance(entityIdCounter++, obsLane, zPos + k * 2.0f, y = coinY))
                }
            }
            patternChoice < 85 -> {
                // Two lanes blocked, one lane clear!
                val openLane = random.nextInt(3) - 1
                for (l in -1..1) {
                    if (l != openLane) {
                        val obsType = if (random.nextBoolean()) ObstacleType.CONSTRUCTION_BARRIER else ObstacleType.TRAFFIC_BARRICADE
                        obstacles.add(ObstacleInstance(entityIdCounter++, obsType, l, zPos))
                    } else {
                        // Guide coins through the safe lane
                        for (k in 0..4) {
                            coins.add(CoinInstance(entityIdCounter++, openLane, zPos + k * 2.0f))
                        }
                    }
                }
            }
            else -> {
                // Cartoon Cow crossing! Or special powerup spawn!
                val cowLane = random.nextInt(3) - 1
                obstacles.add(ObstacleInstance(entityIdCounter++, ObstacleType.CARTOON_COW, cowLane, zPos))

                // Spawn a Power-Up nearby!
                val pLane = if (cowLane != 0) 0 else (if (random.nextBoolean()) -1 else 1)
                val powerType = PowerUpType.values().random()
                powerUps.add(PowerUpInstance(entityIdCounter++, powerType, pLane, zPos + 3.0f))
            }
        }
    }

    fun moveLeft() {
        if (!isRunning || isGameOver || isPaused) return
        if (targetLane > -1) {
            targetLane--
            soundManager?.playSlideSound()
        }
    }

    fun moveRight() {
        if (!isRunning || isGameOver || isPaused) return
        if (targetLane < 1) {
            targetLane++
            soundManager?.playSlideSound()
        }
    }

    fun jump() {
        if (!isRunning || isGameOver || isPaused) return
        if (!isJumping) {
            isJumping = true
            isSliding = false
            val superJumpActive = activePowerUps.containsKey(PowerUpType.SUPER_JUMP)
            playerVelocityY = if (superJumpActive) 17.5f else 13.5f
            soundManager?.playJumpSound()
            if (random.nextInt(10) < 6) {
                triggerSpeech(jumpVoiceLines.random(random), isChaser = false, vocalType = FunnyVocalType.RUNNER_JUMP)
            }
        }
    }

    fun slide() {
        if (!isRunning || isGameOver || isPaused) return
        if (!isSliding) {
            isSliding = true
            slideRemainingTime = 0.85f
            // If in mid-air, fast dive to ground
            if (isJumping) {
                playerVelocityY = -25f
            }
            soundManager?.playSlideSound()
            if (random.nextInt(10) < 6) {
                triggerSpeech(slideVoiceLines.random(random), isChaser = false, vocalType = FunnyVocalType.RUNNER_SLIDE)
            }
        }
    }

    private fun triggerGameOver() {
        isGameOver = true
        isRunning = false
        soundManager?.stopBackgroundMusic()
        soundManager?.playGameOverSound()
        triggerSpeech(gameOverRunnerLines.random(random), isChaser = false, vocalType = FunnyVocalType.SAD_TROMBONE)
        gameOverChaserDelayTimer = 1.5f
        isGameOverChaserTriggered = false
    }

    fun triggerSpeech(text: String, isChaser: Boolean, vocalType: FunnyVocalType? = null) {
        activeSpeech = SpeechBubble(text, isChaser, durationSeconds = 2.4f, remainingSeconds = 2.4f)
        soundManager?.speakVoiceLine(text, isChaser, vocalType)
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }
}
