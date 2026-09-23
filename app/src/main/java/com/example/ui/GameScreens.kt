package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.LeaderboardEntry
import com.example.data.ShopItem
import com.example.game.GameViewModel
import com.example.game.PowerUpType
import com.example.game.ScreenState

@Composable
fun MainGameScreen(viewModel: GameViewModel) {
    val screenState by viewModel.screenState.collectAsState()

    when (screenState) {
        ScreenState.HOME -> HomeScreen(viewModel)
        ScreenState.PLAYING, ScreenState.PAUSED, ScreenState.GAME_OVER -> GameplayContainer(viewModel, screenState)
        ScreenState.CHARACTER -> CharacterSelectScreen(viewModel)
        ScreenState.SHOP -> ShopScreen(viewModel)
        ScreenState.LEADERBOARD -> LeaderboardScreen(viewModel)
        ScreenState.SETTINGS -> SettingsScreen(viewModel)
    }
}

@Composable
fun HomeScreen(viewModel: GameViewModel) {
    val profile by viewModel.playerProfile.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val playButtonScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            )
    ) {
        // Decorative Top Banner with stats
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Coins & Best Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.testTag("home_coins_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${profile?.totalCoins ?: 0}",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Best Score Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🏆", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Best: ${profile?.bestScore ?: 0}",
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Audio Quick Toggles
                Row {
                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier.size(44.dp).testTag("home_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (profile?.isSoundEnabled == true) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Toggle Sound",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleMusic() },
                        modifier = Modifier.size(44.dp).testTag("home_music_toggle")
                    ) {
                        Icon(
                            imageVector = if (profile?.isMusicEnabled == true) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = "Toggle Music",
                            tint = Color.White
                        )
                    }
                }
            }

            // Title & Mascot Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Game Title Logo
                Text(
                    text = "POLITICAL RUN",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF9933),
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "★ THE GREAT INDIAN ENDLESS RUNNER ★",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF138808),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cartoon Parody Notice
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.75f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Text(
                        text = "A Fictional Cartoon Comedy Parody",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Big Cartoon Running Mascot Card (Temple Run Hero Style)
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(220.dp)
                        .clickable { viewModel.tapCharacterTalk() },
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background road preview
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Mini road
                            drawRect(
                                color = Color(0xFF2B2D42),
                                size = size
                            )
                            // Dashed stripes
                            val midY = size.height * 0.72f
                            drawLine(
                                color = Color(0xFFFF9933),
                                start = androidx.compose.ui.geometry.Offset(0f, midY),
                                end = androidx.compose.ui.geometry.Offset(size.width, midY),
                                strokeWidth = 10f
                            )
                        }

                        // Mascot cartoon illustration
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🏃💨", fontSize = 66.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "MR. MODI: THE GREAT SPRINT",
                                color = Color(0xFFFF9933),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Run, Dodge, Jump & Collect Coins!",
                                color = Color(0xFFFFD166),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFF9933).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFFFF9933).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "🎙️ Tap Character for Loud Funny Voice! 🗣️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9933),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Big Play Button
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(64.dp)
                    .scale(playButtonScale)
                    .testTag("home_play_button"),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9933)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY NOW",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Bottom Navigation Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuActionButton(
                    icon = Icons.Default.DirectionsRun,
                    label = "Outfits",
                    color = Color(0xFF3B82F6),
                    testTag = "home_character_button"
                ) { viewModel.openCharacterScreen() }

                MenuActionButton(
                    icon = Icons.Default.ShoppingBag,
                    label = "Shop",
                    color = Color(0xFF10B981),
                    testTag = "home_shop_button"
                ) { viewModel.openShopScreen() }

                MenuActionButton(
                    icon = Icons.Default.Leaderboard,
                    label = "Ranks",
                    color = Color(0xFFF59E0B),
                    testTag = "home_leaderboard_button"
                ) { viewModel.openLeaderboardScreen() }

                MenuActionButton(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    color = Color(0xFF8B5CF6),
                    testTag = "home_settings_button"
                ) { viewModel.openSettingsScreen() }
            }
        }
    }
}

@Composable
fun MenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(6.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.2f),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GameplayContainer(viewModel: GameViewModel, screenState: ScreenState) {
    val textMeasurer = rememberTextMeasurer()
    val frameTick by viewModel.frameTick.collectAsState()
    val profile by viewModel.playerProfile.collectAsState()

    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                        dragStartY = offset.y
                    },
                    onDrag = { change, _ ->
                        val diffX = change.position.x - dragStartX
                        val diffY = change.position.y - dragStartY
                        val minSwipeDistance = 45f

                        if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                            if (diffX > minSwipeDistance) {
                                viewModel.swipeRight()
                                dragStartX = change.position.x
                                dragStartY = change.position.y
                            } else if (diffX < -minSwipeDistance) {
                                viewModel.swipeLeft()
                                dragStartX = change.position.x
                                dragStartY = change.position.y
                            }
                        } else {
                            if (diffY < -minSwipeDistance) {
                                viewModel.swipeUp()
                                dragStartX = change.position.x
                                dragStartY = change.position.y
                            } else if (diffY > minSwipeDistance) {
                                viewModel.swipeDown()
                                dragStartX = change.position.x
                                dragStartY = change.position.y
                            }
                        }
                    }
                )
            }
    ) {
        // 3D Canvas
        Canvas(modifier = Modifier.fillMaxSize().testTag("game_3d_canvas")) {
            // Read frameTick to trigger recomposition per frame
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick

            viewModel.renderer.drawWorld(
                drawScope = this,
                engine = viewModel.engine,
                width = size.width,
                height = size.height,
                textMeasurer = textMeasurer,
                isLowPolyMode = !(profile?.is60FpsEnabled ?: true)
            )
        }

        // HUD OVERLAY
        GameplayHUD(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        // ON-SCREEN BUTTON CONTROLS (If enabled in settings)
        if (profile?.useOnScreenControls == true) {
            OnScreenControlsOverlay(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }

        // PAUSE DIALOG
        if (screenState == ScreenState.PAUSED) {
            PauseDialog(viewModel)
        }

        // GAME OVER DIALOG
        if (screenState == ScreenState.GAME_OVER) {
            GameOverDialog(viewModel)
        }
    }
}

@Composable
fun GameplayHUD(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val engine = viewModel.engine

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 28.dp)
    ) {
        // Top status row: Score, Coins, Multiplier, Pause
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score & Distance
            Column {
                Text(
                    text = "${engine.score}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "${engine.distanceMeters.toInt()}m  •  ${engine.currentEnvironment.title}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFD166)
                )
            }

            // Coins & Multiplier
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (engine.scoreMultiplier > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "${engine.scoreMultiplier}X",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.7f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${engine.coinsCollected}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pause Button
                IconButton(
                    onClick = { viewModel.pauseGame() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.7f), CircleShape)
                        .testTag("game_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }
            }
        }

        // Active Power-Ups Row
        if (engine.activePowerUps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                for ((type, remaining) in engine.activePowerUps) {
                    PowerUpBadge(type, remaining)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun PowerUpBadge(type: PowerUpType, remainingSeconds: Float) {
    val progress = (remainingSeconds / type.durationSeconds).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(type.badgeColorHex).copy(alpha = 0.9f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (type) {
                PowerUpType.COIN_MAGNET -> "🧲"
                PowerUpType.SHIELD -> "🛡️"
                PowerUpType.SPEED_BOOST -> "⚡"
                PowerUpType.DOUBLE_COINS -> "2X"
                PowerUpType.SUPER_JUMP -> "🦘"
            }
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "${remainingSeconds.toInt()}s",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }
    }
}

@Composable
fun OnScreenControlsOverlay(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left & Right D-pad
        Row {
            IconButton(
                onClick = { viewModel.swipeLeft() },
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0x88000000), CircleShape)
                    .testTag("control_left_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Left",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = { viewModel.swipeRight() },
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0x88000000), CircleShape)
                    .testTag("control_right_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Right",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Jump & Slide D-pad
        Row {
            IconButton(
                onClick = { viewModel.swipeUp() },
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0x88000000), CircleShape)
                    .testTag("control_jump_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Jump",
                    tint = Color(0xFFFFD166),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = { viewModel.swipeDown() },
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0x88000000), CircleShape)
                    .testTag("control_slide_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Slide",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun PauseDialog(viewModel: GameViewModel) {
    val profile by viewModel.playerProfile.collectAsState()

    Dialog(onDismissRequest = { viewModel.resumeGame() }) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME PAUSED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9933)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Audio quick toggles in pause
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { viewModel.toggleSound() }) {
                        Icon(
                            imageVector = if (profile?.isSoundEnabled == true) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (profile?.isSoundEnabled == true) "SFX ON" else "SFX OFF")
                    }
                    OutlinedButton(onClick = { viewModel.toggleMusic() }) {
                        Icon(
                            imageVector = if (profile?.isMusicEnabled == true) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (profile?.isMusicEnabled == true) "Music ON" else "Music OFF")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Resume
                Button(
                    onClick = { viewModel.resumeGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pause_resume_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9933))
                ) {
                    Text("RESUME RUN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Restart
                OutlinedButton(
                    onClick = { viewModel.restartGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pause_restart_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Home
                Button(
                    onClick = { viewModel.goToHome() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pause_home_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                ) {
                    Text("MAIN MENU", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(viewModel: GameViewModel) {
    val engine = viewModel.engine
    val profile by viewModel.playerProfile.collectAsState()
    val isNewBest = engine.score > (profile?.bestScore ?: 0)

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "💫", fontSize = 48.sp)
                Text(
                    text = "GAME OVER!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEF4444)
                )
                Text(
                    text = "Arey bhai! What an obstacle!",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (isNewBest) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFD700)
                    ) {
                        Text(
                            text = "★ NEW BEST SCORE! ★",
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatRow(label = "Final Score", value = "${engine.score}", isHighlighted = true)
                        StatRow(label = "Coins Earned", value = "+${engine.coinsCollected} 🪙")
                        StatRow(label = "Distance", value = "${engine.distanceMeters.toInt()}m")
                        StatRow(label = "Environment", value = engine.currentEnvironment.title)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = { viewModel.restartGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("game_over_play_again_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9933)),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY AGAIN", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.openCharacterScreen() },
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    ) {
                        Text("OUTFITS")
                    }
                    Button(
                        onClick = { viewModel.goToHome() },
                        modifier = Modifier.weight(1f).padding(start = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                    ) {
                        Text("HOME")
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, isHighlighted: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 14.sp)
        Text(
            text = value,
            color = if (isHighlighted) Color(0xFFFFD166) else Color.White,
            fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.SemiBold,
            fontSize = if (isHighlighted) 18.sp else 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSelectScreen(viewModel: GameViewModel) {
    val profile by viewModel.playerProfile.collectAsState()
    val shopItems by viewModel.shopItems.collectAsState()
    val outfits = shopItems.filter { it.category == "OUTFIT" }

    Scaffold(
        topBar = {
            TopAppBarCustom(
                title = "Character Outfits",
                onBack = { viewModel.goToHome() },
                coins = profile?.totalCoins ?: 0
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Podium Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clickable { viewModel.tapCharacterTalk() },
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🏃", fontSize = 72.sp)
                        val equipped = outfits.find { it.id == profile?.selectedOutfitId }
                        Text(
                            text = equipped?.name ?: "Classic Saffron",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFFFFD166)
                        )
                        Text(
                            text = equipped?.description ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF9933).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFFF9933).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "🗣️ Tap Runner for Loud Funny Voice! 🎙️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9933),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Outfits List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(outfits) { item ->
                    val isEquipped = profile?.selectedOutfitId == item.id
                    OutfitCard(
                        item = item,
                        isEquipped = isEquipped,
                        onEquip = { viewModel.equipItem(item) },
                        onBuy = { viewModel.buyItem(item) {} }
                    )
                }
            }
        }
    }
}

@Composable
fun OutfitCard(
    item: ShopItem,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped) Color(0xFF1E293B) else Color(0xFF151E2E)
        ),
        border = if (isEquipped) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Color swatch circle
            Surface(
                shape = CircleShape,
                color = Color(item.primaryColorHex),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isEquipped) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = item.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Button
            when {
                isEquipped -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "EQUIPPED",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                item.isUnlocked -> {
                    Button(
                        onClick = onEquip,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("EQUIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = onBuy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9933)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${item.price} 🪙", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val profile by viewModel.playerProfile.collectAsState()
    val shopItems by viewModel.shopItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val categories = listOf("ALL", "ACCESSORY", "TRAIL", "SHOES")

    val filteredItems = when (selectedTab) {
        0 -> shopItems
        1 -> shopItems.filter { it.category == "ACCESSORY" }
        2 -> shopItems.filter { it.category == "TRAIL" }
        3 -> shopItems.filter { it.category == "SHOES" }
        else -> shopItems
    }

    Scaffold(
        topBar = {
            TopAppBarCustom(
                title = "In-Game Shop",
                onBack = { viewModel.goToHome() },
                coins = profile?.totalCoins ?: 0
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFFFF9933)
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems) { item ->
                    val isEquipped = when (item.category) {
                        "OUTFIT" -> profile?.selectedOutfitId == item.id
                        "ACCESSORY" -> profile?.selectedAccessoryId == item.id
                        "TRAIL" -> profile?.selectedTrailId == item.id
                        else -> false
                    }
                    OutfitCard(
                        item = item,
                        isEquipped = isEquipped,
                        onEquip = { viewModel.equipItem(item) },
                        onBuy = { viewModel.buyItem(item) {} }
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardScreen(viewModel: GameViewModel) {
    val profile by viewModel.playerProfile.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("DAILY", "WEEKLY", "ALL-TIME")

    val daily by viewModel.dailyLeaderboard.collectAsState()
    val weekly by viewModel.weeklyLeaderboard.collectAsState()
    val allTime by viewModel.allTimeLeaderboard.collectAsState()

    val currentList = when (selectedTab) {
        0 -> daily
        1 -> weekly
        else -> allTime
    }

    Scaffold(
        topBar = {
            TopAppBarCustom(
                title = "Leaderboards",
                onBack = { viewModel.goToHome() },
                coins = profile?.totalCoins ?: 0
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFFFF9933)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentList) { entry ->
                    LeaderboardCard(entry)
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(entry: LeaderboardEntry) {
    val medal = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCurrentUser) Color(0xFF1E3A8A) else Color(0xFF1E293B)
        ),
        border = if (entry.isCurrentUser) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = medal,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD166),
                    modifier = Modifier.width(40.dp)
                )
                Column {
                    Text(
                        text = entry.playerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${entry.levelName}  •  ${entry.coins} 🪙",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "${entry.score}",
                color = Color(0xFFFFD166),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    val profile by viewModel.playerProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBarCustom(
                title = "Settings",
                onBack = { viewModel.goToHome() },
                coins = profile?.totalCoins ?: 0
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONTROLS & DISPLAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFFF9933)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "On-Screen D-Pad Buttons",
                        subtitle = "Show touch buttons (ideal for buttons lovers)",
                        checked = profile?.useOnScreenControls ?: false,
                        onCheckedChange = { viewModel.toggleOnScreenControls() }
                    )

                    SettingToggleRow(
                        title = "Smooth 60 FPS Mode",
                        subtitle = "Turn off for 30 FPS battery saver mode",
                        checked = profile?.is60FpsEnabled ?: true,
                        onCheckedChange = { viewModel.toggleFpsMode() }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUDIO & SPEECH",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFFF9933)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Sound Effects",
                        subtitle = "Jumps, slides, obstacle whooshes & coins",
                        checked = profile?.isSoundEnabled ?: true,
                        onCheckedChange = { viewModel.toggleSound() }
                    )

                    SettingToggleRow(
                        title = "Background Music",
                        subtitle = "Energetic Indian rhythmic soundtrack",
                        checked = profile?.isMusicEnabled ?: true,
                        onCheckedChange = { viewModel.toggleMusic() }
                    )

                    SettingToggleRow(
                        title = "Cartoon Voice Lines",
                        subtitle = "Funny comedic non-political vocal reactions",
                        checked = profile?.isVoiceEnabled ?: true,
                        onCheckedChange = { viewModel.toggleVoice() }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📢", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Loudness: 100% Boosted with Audio Ducking",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Funny Voice Lab & Soundboard
            val ttsStatus by viewModel.soundManager.ttsStatus.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FUNNY VOICE SOUNDBOARD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFFF9933)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = ttsStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap any button below to audition the funny character voices & comedic sound effects live:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundboardButton(
                            modifier = Modifier.weight(1f),
                            label = "Runner (Modi)",
                            sub = "Mitron! 56-inch!",
                            icon = "🏃",
                            color = Color(0xFFFF9933),
                            onClick = { viewModel.previewVoiceRunner() }
                        )
                        SoundboardButton(
                            modifier = Modifier.weight(1f),
                            label = "Chaser (Rahul)",
                            sub = "Arey Ruko Bhai!",
                            icon = "🏃💨",
                            color = Color(0xFF38BDF8),
                            onClick = { viewModel.previewVoiceChaser() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundboardButton(
                            modifier = Modifier.weight(1f),
                            label = "Cartoon Giggle",
                            sub = "Hehehehe!",
                            icon = "😂",
                            color = Color(0xFFA855F7),
                            onClick = { viewModel.previewVoiceGiggle() }
                        )
                        SoundboardButton(
                            modifier = Modifier.weight(1f),
                            label = "Sad Trombone",
                            sub = "Wah-wah-waaah",
                            icon = "🎺",
                            color = Color(0xFFEF4444),
                            onClick = { viewModel.previewVoiceSadTrombone() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SoundboardButton(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Paisa Hi Paisa! (Coin Celebration)",
                        sub = "Balle balle! Cha-ching!",
                        icon = "🪙",
                        color = Color(0xFFFFD700),
                        onClick = { viewModel.previewVoiceCoin() }
                    )
                }
            }

            // How to Play Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HOW TO PLAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFFF9933)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Swipe Left / Right to switch lanes\n" +
                                "• Swipe Up to jump over barricades, boxes & potholes\n" +
                                "• Swipe Down to slide under low festival banners\n" +
                                "• Collect 🪙 Coins to unlock fresh outfits and accessories in Shop\n" +
                                "• Grab Power-Ups: 🛡️ Shield, 🧲 Magnet, ⚡ Speed Boost, 2X Coins, 🦘 Super Jump!",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFF9933),
                checkedTrackColor = Color(0xFFFF9933).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun TopAppBarCustom(
    title: String,
    onBack: () -> Unit,
    coins: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🪙", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$coins",
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SoundboardButton(
    modifier: Modifier = Modifier,
    label: String,
    sub: String,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sub,
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
