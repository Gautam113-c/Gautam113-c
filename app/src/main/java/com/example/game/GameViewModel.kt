package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.GameRepository
import com.example.data.LeaderboardEntry
import com.example.data.PlayerProfile
import com.example.data.ShopItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    PLAYING,
    PAUSED,
    GAME_OVER,
    CHARACTER,
    SHOP,
    LEADERBOARD,
    SETTINGS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val repository = GameRepository(application)
    val soundManager = SoundManager(application)
    val engine = GameEngine(soundManager)
    val renderer = Game3DRenderer()

    private val _screenState = MutableStateFlow(ScreenState.HOME)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _playerProfile = MutableStateFlow<PlayerProfile?>(null)
    val playerProfile: StateFlow<PlayerProfile?> = _playerProfile.asStateFlow()

    private val _shopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    private val _dailyLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val dailyLeaderboard: StateFlow<List<LeaderboardEntry>> = _dailyLeaderboard.asStateFlow()

    private val _weeklyLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val weeklyLeaderboard: StateFlow<List<LeaderboardEntry>> = _weeklyLeaderboard.asStateFlow()

    private val _allTimeLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val allTimeLeaderboard: StateFlow<List<LeaderboardEntry>> = _allTimeLeaderboard.asStateFlow()

    // Game loop tick state for UI recomposition
    private val _frameTick = MutableStateFlow(0L)
    val frameTick: StateFlow<Long> = _frameTick.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultData()

            launch {
                repository.profileFlow.collect { profile ->
                    _playerProfile.value = profile
                    profile?.let {
                        soundManager.soundEnabled = it.isSoundEnabled
                        soundManager.musicEnabled = it.isMusicEnabled
                        soundManager.voiceEnabled = it.isVoiceEnabled
                        soundManager.vibrationEnabled = it.isVibrationEnabled
                        engine.accessoryType = it.selectedAccessoryId
                    }
                }
            }

            launch {
                repository.shopItemsFlow.collect { items ->
                    _shopItems.value = items
                    // Update outfit color from equipped outfit
                    val profile = _playerProfile.value
                    val equippedOutfit = items.find { it.id == profile?.selectedOutfitId }
                    if (equippedOutfit != null) {
                        engine.outfitColor = equippedOutfit.primaryColorHex
                    }
                }
            }

            launch {
                repository.dao.getLeaderboardFlow("DAILY").collect {
                    _dailyLeaderboard.value = it
                }
            }
            launch {
                repository.dao.getLeaderboardFlow("WEEKLY").collect {
                    _weeklyLeaderboard.value = it
                }
            }
            launch {
                repository.dao.getLeaderboardFlow("ALL_TIME").collect {
                    _allTimeLeaderboard.value = it
                }
            }
        }
    }

    fun startGame() {
        val profile = _playerProfile.value
        val equippedOutfit = _shopItems.value.find { it.id == profile?.selectedOutfitId }
        if (equippedOutfit != null) {
            engine.outfitColor = equippedOutfit.primaryColorHex
        }

        engine.startNewGame()
        _screenState.value = ScreenState.PLAYING
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        val is60Fps = _playerProfile.value?.is60FpsEnabled ?: true
        val frameIntervalMs = if (is60Fps) 16L else 33L

        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTime = now

                if (_screenState.value == ScreenState.PLAYING) {
                    engine.update(dt)
                    _frameTick.value = now

                    if (engine.isGameOver) {
                        onGameOver()
                    }
                }
                delay(frameIntervalMs)
            }
        }
    }

    private fun onGameOver() {
        _screenState.value = ScreenState.GAME_OVER
        viewModelScope.launch {
            repository.recordRun(
                coinsEarned = engine.coinsCollected,
                score = engine.score,
                levelName = engine.currentEnvironment.title
            )
        }
    }

    fun pauseGame() {
        engine.pause()
        soundManager.stopBackgroundMusic()
        _screenState.value = ScreenState.PAUSED
    }

    fun resumeGame() {
        engine.resume()
        soundManager.startBackgroundMusic()
        _screenState.value = ScreenState.PLAYING
    }

    fun restartGame() {
        startGame()
    }

    fun goToHome() {
        gameLoopJob?.cancel()
        engine.isRunning = false
        soundManager.stopBackgroundMusic()
        _screenState.value = ScreenState.HOME
    }

    fun openCharacterScreen() {
        _screenState.value = ScreenState.CHARACTER
    }

    fun openShopScreen() {
        _screenState.value = ScreenState.SHOP
    }

    fun openLeaderboardScreen() {
        _screenState.value = ScreenState.LEADERBOARD
    }

    fun openSettingsScreen() {
        _screenState.value = ScreenState.SETTINGS
    }

    // Player controls
    fun swipeLeft() {
        engine.moveLeft()
    }

    fun swipeRight() {
        engine.moveRight()
    }

    fun swipeUp() {
        engine.jump()
    }

    fun swipeDown() {
        engine.slide()
    }

    // Shop & Customization
    fun equipItem(item: ShopItem) {
        viewModelScope.launch {
            when (item.category) {
                "OUTFIT" -> {
                    repository.dao.setSelectedOutfit(item.id)
                    engine.outfitColor = item.primaryColorHex
                }
                "ACCESSORY" -> {
                    repository.dao.setSelectedAccessory(item.id)
                    engine.accessoryType = item.id
                }
                "TRAIL" -> {
                    repository.dao.setSelectedTrail(item.id)
                }
            }
        }
    }

    fun buyItem(item: ShopItem, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.purchaseItem(item)
            if (success) {
                soundManager.playCoinSound()
                equipItem(item)
            }
            onResult(success)
        }
    }

    // Settings
    fun toggleSound() {
        val current = _playerProfile.value?.isSoundEnabled ?: true
        val updated = !current
        soundManager.soundEnabled = updated
        viewModelScope.launch {
            _playerProfile.value?.let {
                repository.dao.setAudioSettings(sound = updated, music = it.isMusicEnabled, voice = it.isVoiceEnabled)
            }
        }
    }

    fun toggleMusic() {
        val current = _playerProfile.value?.isMusicEnabled ?: true
        val updated = !current
        soundManager.musicEnabled = updated
        if (updated && _screenState.value == ScreenState.PLAYING) {
            soundManager.startBackgroundMusic()
        } else {
            soundManager.stopBackgroundMusic()
        }
        viewModelScope.launch {
            _playerProfile.value?.let {
                repository.dao.setAudioSettings(sound = it.isSoundEnabled, music = updated, voice = it.isVoiceEnabled)
            }
        }
    }

    fun toggleVoice() {
        val current = _playerProfile.value?.isVoiceEnabled ?: true
        val updated = !current
        soundManager.voiceEnabled = updated
        viewModelScope.launch {
            _playerProfile.value?.let {
                repository.dao.setAudioSettings(sound = it.isSoundEnabled, music = it.isMusicEnabled, voice = updated)
            }
        }
    }

    fun toggleOnScreenControls() {
        val current = _playerProfile.value?.useOnScreenControls ?: false
        val updated = !current
        viewModelScope.launch {
            repository.dao.setOnScreenControls(updated)
        }
    }

    fun toggleFpsMode() {
        val current = _playerProfile.value?.is60FpsEnabled ?: true
        val updated = !current
        viewModelScope.launch {
            repository.dao.setFpsMode(updated)
        }
    }

    // Voice Lab & Soundboard testing methods
    fun previewVoiceRunner() {
        soundManager.previewRunnerVoice()
    }

    fun previewVoiceChaser() {
        soundManager.previewChaserVoice()
    }

    fun previewVoiceGiggle() {
        soundManager.previewCartoonBabble()
    }

    fun previewVoiceSadTrombone() {
        soundManager.previewSadTrombone()
    }

    fun previewVoiceCoin() {
        soundManager.previewCoinVoice()
    }

    fun tapCharacterTalk() {
        val characterQuotes = listOf(
            "Mitron! 56-inch running form ready!",
            "Vikas Express at your service!",
            "Looking sharp in this kurta and vest!",
            "Chalo chalo! Let's break the high score today!",
            "Wah! Full energy sprint mode!"
        )
        val quote = characterQuotes.random()
        engine.triggerSpeech(quote, isChaser = false, vocalType = com.example.game.FunnyVocalType.RUNNER_CHEER)
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        soundManager.release()
    }
}
