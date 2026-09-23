package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

class Game3DRenderer {

    // Camera settings: Temple Run dynamic third-person chase camera
    private val cameraZ = -4.2f
    private val cameraY = 2.15f
    private val fov = 1.35f
    private val laneWorldWidth = 1.65f

    data class ProjectedPoint(
        val x: Float,
        val y: Float,
        val scale: Float,
        val isVisible: Boolean
    )

    private fun project3D(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        width: Float,
        height: Float
    ): ProjectedPoint {
        val relZ = worldZ - cameraZ
        if (relZ <= 0.2f) {
            return ProjectedPoint(0f, 0f, 0f, false)
        }
        val scale = fov / relZ
        val horizonY = height * 0.36f
        val centerX = width * 0.5f

        val screenX = centerX + (worldX * laneWorldWidth) * scale * width * 0.85f
        val screenY = horizonY + (cameraY - worldY) * scale * height * 0.95f

        return ProjectedPoint(screenX, screenY, scale, true)
    }

    fun drawWorld(
        drawScope: DrawScope,
        engine: GameEngine,
        width: Float,
        height: Float,
        textMeasurer: TextMeasurer,
        isLowPolyMode: Boolean = false
    ) {
        val env = engine.currentEnvironment
        val horizonY = height * 0.36f

        // 1. SKY & HORIZON
        drawSky(drawScope, env, width, horizonY, engine.distanceMeters)

        // 2. SCENERY BACKGROUND (Hills / City Skyline / Trees)
        drawBackgroundScenery(drawScope, env, width, horizonY, engine.distanceMeters)

        // 3. ROAD & LANES
        drawRoad(drawScope, env, width, height, engine.runCycleTime, engine.currentSpeed)

        // 4. ROADSIDE PROPS (Buildings, shops, trees, lamp posts)
        drawRoadsideProps(drawScope, env, width, height, engine.distanceMeters)

        // Collect all dynamic 3D elements for back-to-front depth sorting
        // Elements: Obstacles, Coins, PowerUps, Chaser, Player
        val renderList = mutableListOf<RenderItem>()

        for (obs in engine.obstacles) {
            if (obs.z in 0.5f..50f) {
                renderList.add(RenderItem.ObstacleItem(obs))
            }
        }
        for (coin in engine.coins) {
            if (coin.z in 0.5f..50f) {
                renderList.add(RenderItem.CoinItem(coin))
            }
        }
        for (pUp in engine.powerUps) {
            if (pUp.z in 0.5f..50f) {
                renderList.add(RenderItem.PowerUpItem(pUp))
            }
        }

        // Add Chaser (behind player, dynamically placed in camera frustum)
        val chaserWorldZ = (0.4f - (engine.chaserDistance * 0.22f)).coerceAtLeast(cameraZ + 0.6f)
        if (chaserWorldZ > cameraZ + 0.3f) {
            renderList.add(RenderItem.ChaserItem(chaserWorldZ))
        }

        // Add Player (z = 0.4f)
        renderList.add(RenderItem.PlayerItem)

        // Depth sort descending (far away objects drawn first)
        renderList.sortByDescending { it.getDepth() }

        // 5. DRAW SORTED ELEMENTS
        for (item in renderList) {
            when (item) {
                is RenderItem.ObstacleItem -> drawObstacle(drawScope, item.obstacle, width, height)
                is RenderItem.CoinItem -> drawCoin(drawScope, item.coin, width, height, engine.runCycleTime)
                is RenderItem.PowerUpItem -> drawPowerUp(drawScope, item.powerUp, width, height, engine.runCycleTime, textMeasurer)
                is RenderItem.ChaserItem -> drawChaser(drawScope, engine, width, height)
                is RenderItem.PlayerItem -> drawPlayer(drawScope, engine, width, height)
            }
        }

        // 6. SPEED LINES EFFECT (if Speed Boost active)
        if (engine.activePowerUps.containsKey(PowerUpType.SPEED_BOOST)) {
            drawSpeedBoostLines(drawScope, width, height, engine.runCycleTime)
        }

        // 7. SPEECH BUBBLE
        engine.activeSpeech?.let { speech ->
            drawSpeechBubble(drawScope, speech, engine, width, height, textMeasurer)
        }
    }

    private sealed class RenderItem {
        abstract fun getDepth(): Float

        data class ObstacleItem(val obstacle: ObstacleInstance) : RenderItem() {
            override fun getDepth(): Float = obstacle.z
        }

        data class CoinItem(val coin: CoinInstance) : RenderItem() {
            override fun getDepth(): Float = coin.z
        }

        data class PowerUpItem(val powerUp: PowerUpInstance) : RenderItem() {
            override fun getDepth(): Float = powerUp.z
        }

        data class ChaserItem(val z: Float) : RenderItem() {
            override fun getDepth(): Float = z
        }

        object PlayerItem : RenderItem() {
            override fun getDepth(): Float = 0.4f
        }
    }

    private fun drawSky(
        scope: DrawScope,
        env: GameEnvironment,
        width: Float,
        horizonY: Float,
        distance: Float
    ) {
        // Sky Gradient
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(env.skyTopColorHex), Color(env.skyBottomColorHex)),
                startY = 0f,
                endY = horizonY
            ),
            topLeft = Offset(0f, 0f),
            size = Size(width, horizonY)
        )

        // Sun / Moon / Stars
        if (env == GameEnvironment.NIGHT_CITY) {
            // Crescent Moon and Stars
            scope.drawCircle(
                color = Color(0xFFFFFBEA),
                radius = width * 0.06f,
                center = Offset(width * 0.78f, horizonY * 0.35f)
            )
            // Stars
            for (i in 0 until 18) {
                val sx = (width * ((i * 37) % 100) / 100f)
                val sy = (horizonY * ((i * 53) % 80) / 100f)
                scope.drawCircle(Color(0xFFE2E8F0), radius = 2f, center = Offset(sx, sy))
            }
        } else {
            // Bright cheerful cartoon sun with rays
            val sunCenter = Offset(width * 0.78f, horizonY * 0.35f)
            scope.drawCircle(
                color = Color(0xFFFFD166),
                radius = width * 0.07f,
                center = sunCenter
            )
            // Floating cartoon clouds
            val cloudOffset = (distance * 0.4f) % (width * 1.5f)
            for (i in 0..2) {
                val cx = ((i * width * 0.5f - cloudOffset + width * 2f) % (width * 1.5f)) - width * 0.2f
                val cy = horizonY * (0.2f + i * 0.18f)
                scope.drawCircle(Color(0xCCFFFFFF), radius = 26f, center = Offset(cx, cy))
                scope.drawCircle(Color(0xCCFFFFFF), radius = 34f, center = Offset(cx + 22f, cy - 6f))
                scope.drawCircle(Color(0xCCFFFFFF), radius = 24f, center = Offset(cx + 44f, cy))
            }
        }
    }

    private fun drawBackgroundScenery(
        scope: DrawScope,
        env: GameEnvironment,
        width: Float,
        horizonY: Float,
        distance: Float
    ) {
        val scroll = (distance * 2.5f) % (width * 2f)

        when (env) {
            GameEnvironment.INDIAN_CITY, GameEnvironment.NIGHT_CITY -> {
                // Skyline buildings silhouettes
                val buildingCount = 14
                val bWidth = width / 7f
                for (i in -1..buildingCount) {
                    val bx = i * bWidth - (scroll * 0.25f % bWidth)
                    val bHeight = 40f + ((i * 73) % 85)
                    val bColor = if (env == GameEnvironment.NIGHT_CITY) Color(0xFF1E2235) else Color(0x996B7280)
                    scope.drawRect(
                        color = bColor,
                        topLeft = Offset(bx, horizonY - bHeight),
                        size = Size(bWidth * 0.92f, bHeight)
                    )
                    // Windows
                    if (env == GameEnvironment.NIGHT_CITY) {
                        for (w in 0..2) {
                            val wy = horizonY - bHeight + 12f + w * 18f
                            scope.drawRect(
                                color = if ((i + w) % 3 == 0) Color(0xFFFFD166) else Color(0xFF00E5FF),
                                topLeft = Offset(bx + 10f, wy),
                                size = Size(8f, 10f)
                            )
                        }
                    }
                }
            }
            GameEnvironment.VILLAGE -> {
                // Rolling green hills & distant village trees
                val hillPath = Path().apply {
                    moveTo(0f, horizonY)
                    for (x in 0..width.toInt() step 60) {
                        val hy = horizonY - 25f - 18f * sin((x + distance * 0.5f) * 0.015f)
                        lineTo(x.toFloat(), hy)
                    }
                    lineTo(width, horizonY)
                    close()
                }
                scope.drawPath(hillPath, color = Color(0xFF40916C))
            }
            GameEnvironment.MOUNTAIN_ROAD -> {
                // Jagged majestic mountain peaks
                val peakPath = Path().apply {
                    moveTo(0f, horizonY)
                    for (x in 0..width.toInt() step 90) {
                        val py = horizonY - 45f - 40f * kotlin.math.abs(sin((x + distance * 0.3f) * 0.01f))
                        lineTo(x.toFloat(), py)
                    }
                    lineTo(width, horizonY)
                    close()
                }
                scope.drawPath(peakPath, color = Color(0xFF4A5568))
            }
            GameEnvironment.FOREST -> {
                // Dense jungle tree canopy
                for (i in 0 until 12) {
                    val tx = i * (width / 6f) - (scroll * 0.3f % (width / 6f))
                    val ty = horizonY - 35f
                    scope.drawCircle(Color(0xFF1B4332), radius = 35f, center = Offset(tx, ty))
                    scope.drawCircle(Color(0xFF2D6A4F), radius = 28f, center = Offset(tx + 18f, ty - 10f))
                }
            }
        }
    }

    private fun drawRoad(
        scope: DrawScope,
        env: GameEnvironment,
        width: Float,
        height: Float,
        runCycle: Float,
        speed: Float
    ) {
        val horizonY = height * 0.36f

        // Roadside grass / terrain
        scope.drawRect(
            color = Color(env.roadsideColorHex),
            topLeft = Offset(0f, horizonY),
            size = Size(width, height - horizonY)
        )

        // Road Trapezoid (3D perspective)
        val pFarLeft = project3D(-1.75f, 0f, 50f, width, height)
        val pFarRight = project3D(1.75f, 0f, 50f, width, height)
        val pNearLeft = project3D(-1.75f, 0f, 0.4f, width, height)
        val pNearRight = project3D(1.75f, 0f, 0.4f, width, height)

        val roadPath = Path().apply {
            moveTo(pFarLeft.x, pFarLeft.y)
            lineTo(pFarRight.x, pFarRight.y)
            lineTo(pNearRight.x, height)
            lineTo(pNearLeft.x, height)
            close()
        }
        scope.drawPath(roadPath, color = Color(env.roadColorHex))

        // Curbs / Sidewalk strips
        val curbWidthFar = (pFarRight.x - pFarLeft.x) * 0.05f
        val curbWidthNear = (pNearRight.x - pNearLeft.x) * 0.045f

        // Left curb
        val leftCurb = Path().apply {
            moveTo(pFarLeft.x - curbWidthFar, pFarLeft.y)
            lineTo(pFarLeft.x, pFarLeft.y)
            lineTo(pNearLeft.x, height)
            lineTo(pNearLeft.x - curbWidthNear, height)
            close()
        }
        scope.drawPath(leftCurb, color = Color(0xFFF77F00))

        // Right curb
        val rightCurb = Path().apply {
            moveTo(pFarRight.x, pFarRight.y)
            lineTo(pFarRight.x + curbWidthFar, pFarRight.y)
            lineTo(pNearRight.x + curbWidthNear, height)
            lineTo(pNearRight.x, height)
            close()
        }
        scope.drawPath(rightCurb, color = Color(0xFFF77F00))

        // Lane Dividers (Dashed stripes moving with speed)
        val dashZSpacing = 4.5f
        val dashOffset = (runCycle * 2.2f) % dashZSpacing

        for (z in 1..10) {
            val stripeZ = z * dashZSpacing - dashOffset
            if (stripeZ in 0.5f..48f) {
                // Lane line between Left & Center: X = -0.5
                val l1Near = project3D(-0.55f, 0f, stripeZ, width, height)
                val l1Far = project3D(-0.55f, 0f, stripeZ + 2.0f, width, height)
                if (l1Near.isVisible && l1Far.isVisible) {
                    scope.drawLine(
                        color = Color(0xFFFFFFFF),
                        start = Offset(l1Near.x, l1Near.y),
                        end = Offset(l1Far.x, l1Far.y),
                        strokeWidth = (l1Near.scale * 18f).coerceIn(1.5f, 10f)
                    )
                }

                // Lane line between Center & Right: X = 0.55
                val l2Near = project3D(0.55f, 0f, stripeZ, width, height)
                val l2Far = project3D(0.55f, 0f, stripeZ + 2.0f, width, height)
                if (l2Near.isVisible && l2Far.isVisible) {
                    scope.drawLine(
                        color = Color(0xFFFFFFFF),
                        start = Offset(l2Near.x, l2Near.y),
                        end = Offset(l2Far.x, l2Far.y),
                        strokeWidth = (l2Near.scale * 18f).coerceIn(1.5f, 10f)
                    )
                }
            }
        }
    }

    private fun drawRoadsideProps(
        scope: DrawScope,
        env: GameEnvironment,
        width: Float,
        height: Float,
        distance: Float
    ) {
        val propSpacing = 16f
        val propOffset = (distance % propSpacing)

        for (i in 0..3) {
            val z = (i + 1) * propSpacing - propOffset
            if (z in 1f..50f) {
                // Left roadside prop
                val pLeft = project3D(-2.4f, 0f, z, width, height)
                if (pLeft.isVisible) {
                    drawRoadsideObject(scope, env, pLeft, isLeft = true)
                }
                // Right roadside prop
                val pRight = project3D(2.4f, 0f, z, width, height)
                if (pRight.isVisible) {
                    drawRoadsideObject(scope, env, pRight, isLeft = false)
                }
            }
        }
    }

    private fun drawRoadsideObject(
        scope: DrawScope,
        env: GameEnvironment,
        proj: ProjectedPoint,
        isLeft: Boolean
    ) {
        val s = proj.scale * 140f
        when (env) {
            GameEnvironment.INDIAN_CITY -> {
                // Colorful Shophouses or Auto-Rickshaw
                val shopColor = if (isLeft) Color(0xFFE76F51) else Color(0xFF2A9D8F)
                scope.drawRect(
                    color = shopColor,
                    topLeft = Offset(proj.x - s * 0.5f, proj.y - s * 1.1f),
                    size = Size(s, s * 1.1f)
                )
                // Awning
                scope.drawRect(
                    color = Color(0xFFFFD166),
                    topLeft = Offset(proj.x - s * 0.55f, proj.y - s * 0.7f),
                    size = Size(s * 1.1f, s * 0.18f)
                )
            }
            GameEnvironment.VILLAGE -> {
                // Village Thatched Hut or Banyan Tree
                if (isLeft) {
                    // Tree
                    scope.drawRect(
                        color = Color(0xFF6B4226),
                        topLeft = Offset(proj.x - s * 0.12f, proj.y - s * 1.2f),
                        size = Size(s * 0.24f, s * 1.2f)
                    )
                    scope.drawCircle(
                        color = Color(0xFF2D6A4F),
                        radius = s * 0.6f,
                        center = Offset(proj.x, proj.y - s * 1.3f)
                    )
                } else {
                    // Thatched mud hut
                    scope.drawRect(
                        color = Color(0xFFD4A373),
                        topLeft = Offset(proj.x - s * 0.4f, proj.y - s * 0.8f),
                        size = Size(s * 0.8f, s * 0.8f)
                    )
                    // Roof
                    val roof = Path().apply {
                        moveTo(proj.x, proj.y - s * 1.2f)
                        lineTo(proj.x + s * 0.5f, proj.y - s * 0.8f)
                        lineTo(proj.x - s * 0.5f, proj.y - s * 0.8f)
                        close()
                    }
                    scope.drawPath(roof, color = Color(0xFF8B5E3C))
                }
            }
            GameEnvironment.NIGHT_CITY -> {
                // Neon lamppost & skyscraper
                scope.drawLine(
                    color = Color(0xFF00F5D4),
                    start = Offset(proj.x, proj.y),
                    end = Offset(proj.x, proj.y - s * 1.6f),
                    strokeWidth = s * 0.08f
                )
                scope.drawCircle(
                    color = Color(0xFFFF007F),
                    radius = s * 0.2f,
                    center = Offset(proj.x, proj.y - s * 1.6f)
                )
            }
            else -> {
                // Trees / Rocks
                scope.drawCircle(
                    color = Color(0xFF2D6A4F),
                    radius = s * 0.5f,
                    center = Offset(proj.x, proj.y - s * 0.8f)
                )
            }
        }
    }

    private fun drawObstacle(
        scope: DrawScope,
        obs: ObstacleInstance,
        width: Float,
        height: Float
    ) {
        val proj = project3D(obs.lane.toFloat(), 0f, obs.z, width, height)
        if (!proj.isVisible) return

        val obsBase = kotlin.math.max(height * 0.22f, width * 0.33f)
        val s = (proj.scale / 0.293f) * obsBase

        // Shadow on road
        scope.drawOval(
            color = Color(0x55000000),
            topLeft = Offset(proj.x - s * 0.45f, proj.y - s * 0.1f),
            size = Size(s * 0.9f, s * 0.2f)
        )

        when (obs.type) {
            ObstacleType.TRAFFIC_BARRICADE, ObstacleType.CONSTRUCTION_BARRIER -> {
                // Sturdy barricade with hazard stripes
                val barW = s * 0.9f
                val barH = s * 0.65f
                val topY = proj.y - barH

                // Legs
                scope.drawLine(Color(0xFF333333), Offset(proj.x - barW * 0.35f, proj.y), Offset(proj.x - barW * 0.35f, topY), strokeWidth = s * 0.08f)
                scope.drawLine(Color(0xFF333333), Offset(proj.x + barW * 0.35f, proj.y), Offset(proj.x + barW * 0.35f, topY), strokeWidth = s * 0.08f)

                // Main striped board
                val barColor = if (obs.type == ObstacleType.CONSTRUCTION_BARRIER) Color(0xFFFFB703) else Color(0xFFE63946)
                scope.drawRect(
                    color = barColor,
                    topLeft = Offset(proj.x - barW * 0.5f, topY),
                    size = Size(barW, barH * 0.45f)
                )
                // White hazard stripes
                for (k in -2..2) {
                    val sx = proj.x + k * (barW * 0.2f)
                    scope.drawLine(Color.White, Offset(sx - barW * 0.06f, topY + barH * 0.45f), Offset(sx + barW * 0.06f, topY), strokeWidth = s * 0.05f)
                }
            }
            ObstacleType.WOODEN_BOX -> {
                val boxSize = s * 0.6f
                scope.drawRect(
                    color = Color(0xFF8B5A2B),
                    topLeft = Offset(proj.x - boxSize * 0.5f, proj.y - boxSize),
                    size = Size(boxSize, boxSize)
                )
                // Cross planks
                scope.drawLine(Color(0xFF5C3A1E), Offset(proj.x - boxSize * 0.5f, proj.y - boxSize), Offset(proj.x + boxSize * 0.5f, proj.y), strokeWidth = s * 0.04f)
                scope.drawLine(Color(0xFF5C3A1E), Offset(proj.x + boxSize * 0.5f, proj.y - boxSize), Offset(proj.x - boxSize * 0.5f, proj.y), strokeWidth = s * 0.04f)
            }
            ObstacleType.ROAD_CONES -> {
                val coneW = s * 0.5f
                val coneH = s * 0.6f
                // Base
                scope.drawRect(Color(0xFF222222), Offset(proj.x - coneW * 0.5f, proj.y - coneH * 0.15f), Size(coneW, coneH * 0.15f))
                // Triangle cone
                val conePath = Path().apply {
                    moveTo(proj.x, proj.y - coneH)
                    lineTo(proj.x + coneW * 0.4f, proj.y - coneH * 0.15f)
                    lineTo(proj.x - coneW * 0.4f, proj.y - coneH * 0.15f)
                    close()
                }
                scope.drawPath(conePath, color = Color(0xFFF77F00))
                // White reflective stripe
                scope.drawRect(Color.White, Offset(proj.x - coneW * 0.22f, proj.y - coneH * 0.58f), Size(coneW * 0.44f, coneH * 0.14f))
            }
            ObstacleType.POTHOLE -> {
                // Dark cracked depression
                scope.drawOval(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(proj.x - s * 0.45f, proj.y - s * 0.16f),
                    size = Size(s * 0.9f, s * 0.32f)
                )
                scope.drawOval(
                    color = Color(0xFF0F0F0F),
                    topLeft = Offset(proj.x - s * 0.32f, proj.y - s * 0.1f),
                    size = Size(s * 0.64f, s * 0.2f)
                )
            }
            ObstacleType.SMALL_WALL, ObstacleType.PARK_BENCH -> {
                val wallW = s * 0.85f
                val wallH = s * 0.45f
                scope.drawRect(
                    color = Color(0xFFB04A36),
                    topLeft = Offset(proj.x - wallW * 0.5f, proj.y - wallH),
                    size = Size(wallW, wallH)
                )
                // Brick lines
                scope.drawLine(Color(0xFF7A2E20), Offset(proj.x - wallW * 0.5f, proj.y - wallH * 0.5f), Offset(proj.x + wallW * 0.5f, proj.y - wallH * 0.5f), strokeWidth = 2f)
            }
            ObstacleType.OVERHEAD_BANNER -> {
                // Tall banner player must slide under
                val postH = s * 1.5f
                val bannerW = s * 1.0f
                // Poles on left & right
                scope.drawLine(Color(0xFF8B5A2B), Offset(proj.x - bannerW * 0.5f, proj.y), Offset(proj.x - bannerW * 0.5f, proj.y - postH), strokeWidth = s * 0.06f)
                scope.drawLine(Color(0xFF8B5A2B), Offset(proj.x + bannerW * 0.5f, proj.y), Offset(proj.x + bannerW * 0.5f, proj.y - postH), strokeWidth = s * 0.06f)
                // Banner high up
                scope.drawRect(
                    color = Color(0xFFE63946),
                    topLeft = Offset(proj.x - bannerW * 0.5f, proj.y - postH),
                    size = Size(bannerW, postH * 0.4f)
                )
                // Slide reminder icon or arrow
                scope.drawOval(Color(0xFFFFD166), Offset(proj.x - s * 0.15f, proj.y - postH + postH * 0.1f), Size(s * 0.3f, s * 0.2f))
            }
            ObstacleType.CARTOON_COW -> {
                // Harmless funny cartoon cow peacefully sitting/chilling
                val cowW = s * 0.9f
                val cowH = s * 0.65f
                // Body
                scope.drawOval(
                    color = Color(0xFFEEEEEE),
                    topLeft = Offset(proj.x - cowW * 0.45f, proj.y - cowH * 0.85f),
                    size = Size(cowW * 0.9f, cowH * 0.75f)
                )
                // Black spots
                scope.drawCircle(Color(0xFF333333), radius = cowW * 0.14f, center = Offset(proj.x - cowW * 0.15f, proj.y - cowH * 0.5f))
                scope.drawCircle(Color(0xFF333333), radius = cowW * 0.11f, center = Offset(proj.x + cowW * 0.18f, proj.y - cowH * 0.6f))
                // Head
                scope.drawCircle(Color(0xFFFFFFFF), radius = cowW * 0.22f, center = Offset(proj.x - cowW * 0.28f, proj.y - cowH * 0.7f))
                // Muzzle (pink)
                scope.drawOval(Color(0xFFFFB4A2), Offset(proj.x - cowW * 0.36f, proj.y - cowH * 0.62f), Size(cowW * 0.22f, cowH * 0.22f))
                // Cute small horns
                scope.drawLine(Color(0xFF8B5A2B), Offset(proj.x - cowW * 0.32f, proj.y - cowH * 0.88f), Offset(proj.x - cowW * 0.38f, proj.y - cowH * 1.05f), strokeWidth = s * 0.05f)
                scope.drawLine(Color(0xFF8B5A2B), Offset(proj.x - cowW * 0.24f, proj.y - cowH * 0.88f), Offset(proj.x - cowW * 0.18f, proj.y - cowH * 1.05f), strokeWidth = s * 0.05f)
            }
        }
    }

    private fun drawCoin(
        scope: DrawScope,
        coin: CoinInstance,
        width: Float,
        height: Float,
        runCycle: Float
    ) {
        val proj = project3D(coin.lane.toFloat(), coin.y, coin.z, width, height)
        if (!proj.isVisible) return

        val coinBase = kotlin.math.max(height * 0.13f, width * 0.18f)
        val s = (proj.scale / 0.293f) * coinBase
        // Spinning width effect
        val spin = cos(runCycle * 4f + coin.id.toFloat())
        val coinWidth = (s * 0.35f * kotlin.math.abs(spin)).coerceAtLeast(s * 0.06f)
        val coinHeight = s * 0.35f

        // Shadow on road
        val shadowProj = project3D(coin.lane.toFloat(), 0f, coin.z, width, height)
        if (shadowProj.isVisible) {
            scope.drawOval(
                color = Color(0x44000000),
                topLeft = Offset(shadowProj.x - s * 0.18f, shadowProj.y - s * 0.06f),
                size = Size(s * 0.36f, s * 0.12f)
            )
        }

        // Golden rim
        scope.drawOval(
            color = Color(0xFFE5A100),
            topLeft = Offset(proj.x - coinWidth * 0.5f, proj.y - coinHeight * 0.5f),
            size = Size(coinWidth, coinHeight)
        )
        // Golden face
        scope.drawOval(
            color = Color(0xFFFFD700),
            topLeft = Offset(proj.x - coinWidth * 0.42f, proj.y - coinHeight * 0.42f),
            size = Size(coinWidth * 0.84f, coinHeight * 0.84f)
        )
        // Center glint
        scope.drawCircle(
            color = Color(0xFFFFFBEA),
            radius = (coinWidth * 0.18f).coerceAtLeast(1.5f),
            center = Offset(proj.x, proj.y)
        )
    }

    private fun drawPowerUp(
        scope: DrawScope,
        pUp: PowerUpInstance,
        width: Float,
        height: Float,
        runCycle: Float,
        textMeasurer: TextMeasurer
    ) {
        val hoverY = 0.7f + 0.15f * sin(runCycle * 3f + pUp.id.toFloat())
        val proj = project3D(pUp.lane.toFloat(), hoverY, pUp.z, width, height)
        if (!proj.isVisible) return

        val powerUpBase = kotlin.math.max(height * 0.16f, width * 0.22f)
        val s = (proj.scale / 0.293f) * powerUpBase

        // Glowing outer aura
        scope.drawCircle(
            color = Color(pUp.type.badgeColorHex).copy(alpha = 0.45f),
            radius = s * 0.38f,
            center = Offset(proj.x, proj.y)
        )

        // Floating Gem Box
        scope.drawRoundRect(
            color = Color(pUp.type.badgeColorHex),
            topLeft = Offset(proj.x - s * 0.24f, proj.y - s * 0.24f),
            size = Size(s * 0.48f, s * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f)
        )

        // Draw symbol
        val symbol = when (pUp.type) {
            PowerUpType.COIN_MAGNET -> "🧲"
            PowerUpType.SHIELD -> "🛡️"
            PowerUpType.SPEED_BOOST -> "⚡"
            PowerUpType.DOUBLE_COINS -> "2X"
            PowerUpType.SUPER_JUMP -> "🦘"
        }

        val textLayout = textMeasurer.measure(
            text = symbol,
            style = TextStyle(
                fontSize = (s * 0.22f).coerceIn(9f, 26f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        drawScopeText(scope, textLayout, Offset(proj.x - textLayout.size.width / 2f, proj.y - textLayout.size.height / 2f))
    }

    private fun drawPlayer(
        scope: DrawScope,
        engine: GameEngine,
        width: Float,
        height: Float
    ) {
        // Player is at Z = 0.4f
        val proj = project3D(engine.playerLaneX, engine.playerY, 0.4f, width, height)
        if (!proj.isVisible) return

        // Large, heroic Temple Run runner proportions ("Tamlrum game Mr")
        val heroBase = kotlin.math.max(height * 0.28f, width * 0.44f)
        val s = (proj.scale / 0.293f) * heroBase
        val runAngle = engine.runCycleTime * 10f

        // Ground shadow
        val shadowProj = project3D(engine.playerLaneX, 0f, 0.4f, width, height)
        val shadowScale = (1.0f - (engine.playerY / 4.0f)).coerceIn(0.4f, 1.0f)
        scope.drawOval(
            color = Color(0x66000000),
            topLeft = Offset(shadowProj.x - s * 0.35f * shadowScale, shadowProj.y - s * 0.1f * shadowScale),
            size = Size(s * 0.7f * shadowScale, s * 0.2f * shadowScale)
        )

        // If sliding: draw crouched horizontal pose
        if (engine.isSliding) {
            drawSlidingPlayer(scope, proj, s, engine.outfitColor)
            return
        }

        // Running / Jumping Pose
        val legSwing = if (engine.isJumping) 0.3f else sin(runAngle)
        val armSwing = if (engine.isJumping) -0.6f else -legSwing

        // 1. LEGS (white churidar/pyjama)
        val legW = s * 0.12f
        val legH = s * 0.32f
        val hipY = proj.y - s * 0.35f

        // Left Leg
        val leftLegFootY = hipY + legH + legSwing * s * 0.12f
        scope.drawLine(
            Color(0xFFE2E8F0),
            Offset(proj.x - s * 0.12f, hipY),
            Offset(proj.x - s * 0.12f + legSwing * s * 0.1f, leftLegFootY),
            strokeWidth = legW
        )
        // Left Shoe (brown mojri)
        scope.drawOval(Color(0xFF5C3A1E), Offset(proj.x - s * 0.18f + legSwing * s * 0.1f, leftLegFootY - s * 0.04f), Size(s * 0.18f, s * 0.09f))

        // Right Leg
        val rightLegFootY = hipY + legH - legSwing * s * 0.12f
        scope.drawLine(
            Color(0xFFE2E8F0),
            Offset(proj.x + s * 0.12f, hipY),
            Offset(proj.x + s * 0.12f - legSwing * s * 0.1f, rightLegFootY),
            strokeWidth = legW
        )
        // Right Shoe
        scope.drawOval(Color(0xFF5C3A1E), Offset(proj.x + s * 0.06f - legSwing * s * 0.1f, rightLegFootY - s * 0.04f), Size(s * 0.18f, s * 0.09f))

        // 2. TORSO & OUTFIT (White kurta under vibrant Modi vest)
        val torsoW = s * 0.34f
        val torsoH = s * 0.38f
        val torsoY = hipY - torsoH

        // White Kurta base
        scope.drawRoundRect(
            color = Color(0xFFF8F9FA),
            topLeft = Offset(proj.x - torsoW * 0.5f, torsoY),
            size = Size(torsoW, torsoH * 1.15f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f)
        )

        // Modi Vest (Selected outfit color)
        scope.drawRoundRect(
            color = Color(engine.outfitColor),
            topLeft = Offset(proj.x - torsoW * 0.48f, torsoY + s * 0.02f),
            size = Size(torsoW * 0.96f, torsoH * 0.95f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f, s * 0.06f)
        )

        // Vest buttons
        for (b in 0..2) {
            scope.drawCircle(Color(0xFF222222), radius = s * 0.015f, center = Offset(proj.x, torsoY + s * 0.08f + b * s * 0.09f))
        }

        // 3. ARMS
        val armW = s * 0.09f
        // Left Arm
        scope.drawLine(
            Color(0xFFF8F9FA),
            Offset(proj.x - torsoW * 0.5f, torsoY + s * 0.06f),
            Offset(proj.x - torsoW * 0.5f + armSwing * s * 0.15f, torsoY + s * 0.3f),
            strokeWidth = armW
        )
        // Right Arm
        scope.drawLine(
            Color(0xFFF8F9FA),
            Offset(proj.x + torsoW * 0.5f, torsoY + s * 0.06f),
            Offset(proj.x + torsoW * 0.5f - armSwing * s * 0.15f, torsoY + s * 0.3f),
            strokeWidth = armW
        )

        // 4. CARTOON HEAD & FACE
        val headRadius = s * 0.18f
        val headCenter = Offset(proj.x, torsoY - headRadius * 0.75f)

        // White stylized cartoon hair (back)
        scope.drawCircle(Color(0xFFEDEDED), radius = headRadius * 1.12f, center = headCenter)

        // Face skin
        scope.drawCircle(Color(0xFFFFDFBA), radius = headRadius, center = headCenter)

        // White stylized cartoon beard
        val beardPath = Path().apply {
            moveTo(headCenter.x - headRadius * 0.85f, headCenter.y + headRadius * 0.1f)
            lineTo(headCenter.x - headRadius * 0.6f, headCenter.y + headRadius * 1.05f)
            lineTo(headCenter.x, headCenter.y + headRadius * 1.25f)
            lineTo(headCenter.x + headRadius * 0.6f, headCenter.y + headRadius * 1.05f)
            lineTo(headCenter.x + headRadius * 0.85f, headCenter.y + headRadius * 0.1f)
            close()
        }
        scope.drawPath(beardPath, color = Color(0xFFF0F0F0))

        // Spectacles (round rim glasses)
        val specR = headRadius * 0.32f
        scope.drawCircle(Color(0xFF333333), radius = specR, center = Offset(headCenter.x - headRadius * 0.38f, headCenter.y - headRadius * 0.1f), style = Stroke(width = s * 0.02f))
        scope.drawCircle(Color(0xFF333333), radius = specR, center = Offset(headCenter.x + headRadius * 0.38f, headCenter.y - headRadius * 0.1f), style = Stroke(width = s * 0.02f))
        scope.drawLine(Color(0xFF333333), Offset(headCenter.x - headRadius * 0.1f, headCenter.y - headRadius * 0.1f), Offset(headCenter.x + headRadius * 0.1f, headCenter.y - headRadius * 0.1f), strokeWidth = s * 0.02f)

        // Smiling eyes
        scope.drawCircle(Color(0xFF222222), radius = s * 0.02f, center = Offset(headCenter.x - headRadius * 0.38f, headCenter.y - headRadius * 0.1f))
        scope.drawCircle(Color(0xFF222222), radius = s * 0.02f, center = Offset(headCenter.x + headRadius * 0.38f, headCenter.y - headRadius * 0.1f))

        // Warm smile
        scope.drawArc(
            color = Color(0xFFC0392B),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(headCenter.x - headRadius * 0.3f, headCenter.y + headRadius * 0.2f),
            size = Size(headRadius * 0.6f, headRadius * 0.45f),
            style = Stroke(width = s * 0.02f)
        )

        // POWER-UP AURAS ON PLAYER
        // Shield Bubble
        if (engine.activePowerUps.containsKey(PowerUpType.SHIELD)) {
            scope.drawCircle(
                color = Color(0x6600E5FF),
                radius = s * 0.65f,
                center = Offset(proj.x, torsoY),
                style = Stroke(width = s * 0.05f)
            )
            scope.drawCircle(
                color = Color(0x2200E5FF),
                radius = s * 0.65f,
                center = Offset(proj.x, torsoY)
            )
        }

        // Magnet Aura
        if (engine.activePowerUps.containsKey(PowerUpType.COIN_MAGNET)) {
            val rot = engine.runCycleTime * 6f
            scope.drawCircle(
                color = Color(0x88FFD700),
                radius = s * 0.55f + 5f * sin(rot),
                center = Offset(proj.x, torsoY),
                style = Stroke(width = 3f)
            )
        }
    }

    private fun drawSlidingPlayer(
        scope: DrawScope,
        proj: ProjectedPoint,
        s: Float,
        outfitColor: Long
    ) {
        // Slide pose: horizontal low crouching
        val slideW = s * 0.65f
        val slideH = s * 0.28f
        val baseY = proj.y - slideH

        // Dust particles kicking up behind
        for (i in 0..3) {
            val dx = proj.x - slideW * 0.5f - (i * 12f)
            val dy = proj.y - (i * 4f)
            scope.drawCircle(Color(0x77CCCCCC), radius = 6f + i * 2f, center = Offset(dx, dy))
        }

        // Body
        scope.drawRoundRect(
            color = Color(outfitColor),
            topLeft = Offset(proj.x - slideW * 0.5f, baseY),
            size = Size(slideW, slideH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f)
        )

        // Head looking ahead
        scope.drawCircle(Color(0xFFFFDFBA), radius = s * 0.14f, center = Offset(proj.x + slideW * 0.35f, baseY + slideH * 0.4f))
        // White hair & beard
        scope.drawCircle(Color(0xFFEEEEEE), radius = s * 0.08f, center = Offset(proj.x + slideW * 0.42f, baseY + slideH * 0.6f))
    }

    private fun drawChaser(
        scope: DrawScope,
        engine: GameEngine,
        width: Float,
        height: Float
    ) {
        val chaserWorldZ = (0.4f - (engine.chaserDistance * 0.22f)).coerceAtLeast(cameraZ + 0.6f)
        // Offset slightly to the left behind Modi so the cartoon stick waving is clearly visible and never obscures the player
        val chaserLane = engine.chaserLaneX - 0.42f
        val proj = project3D(chaserLane, 0f, chaserWorldZ, width, height)
        if (!proj.isVisible) return

        val chaserBase = kotlin.math.max(height * 0.25f, width * 0.38f)
        val s = (proj.scale / 0.293f) * chaserBase
        val chaserRunAngle = engine.runCycleTime * 11f
        val legSwing = sin(chaserRunAngle)
        val armSwing = -legSwing

        // Shadow on road
        scope.drawOval(
            color = Color(0x66000000),
            topLeft = Offset(proj.x - s * 0.3f, proj.y - s * 0.08f),
            size = Size(s * 0.6f, s * 0.16f)
        )

        val hipY = proj.y - s * 0.32f

        // Legs (white trousers)
        val legW = s * 0.11f
        val legH = s * 0.28f

        // Left leg
        scope.drawLine(Color(0xFFE2E8F0), Offset(proj.x - s * 0.1f, hipY), Offset(proj.x - s * 0.1f + legSwing * s * 0.1f, hipY + legH), strokeWidth = legW)
        // Right leg
        scope.drawLine(Color(0xFFE2E8F0), Offset(proj.x + s * 0.1f, hipY), Offset(proj.x + s * 0.1f - legSwing * s * 0.1f, hipY + legH), strokeWidth = legW)

        // Torso: White Kurta with folded sleeves
        val torsoW = s * 0.32f
        val torsoH = s * 0.34f
        val torsoY = hipY - torsoH

        scope.drawRoundRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(proj.x - torsoW * 0.5f, torsoY),
            size = Size(torsoW, torsoH * 1.1f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f, s * 0.06f)
        )

        // Left Arm swinging forward
        scope.drawLine(
            Color(0xFFF1F5F9),
            Offset(proj.x - torsoW * 0.45f, torsoY + s * 0.05f),
            Offset(proj.x - torsoW * 0.45f + armSwing * s * 0.15f, torsoY + s * 0.28f),
            strokeWidth = s * 0.08f
        )

        // Right Arm waving the harmless cartoon stick playfully above!
        val stickWavingOffset = sin(engine.chaserWavingStickAngle) * s * 0.08f
        val handPos = Offset(proj.x + torsoW * 0.5f + stickWavingOffset, torsoY - s * 0.15f)

        scope.drawLine(
            Color(0xFFF1F5F9),
            Offset(proj.x + torsoW * 0.45f, torsoY + s * 0.05f),
            handPos,
            strokeWidth = s * 0.08f
        )

        // Harmless Cartoon Stick (smooth wooden stick / playful baton)
        val stickTip = Offset(handPos.x + s * 0.15f, handPos.y - s * 0.22f)
        scope.drawLine(
            Color(0xFF8B5A2B),
            handPos,
            stickTip,
            strokeWidth = s * 0.05f
        )
        // Decorative harmless cartoon flag / ribbon on stick tip
        scope.drawCircle(Color(0xFFFF9933), radius = s * 0.04f, center = stickTip)

        // Cartoon Chaser Head
        val headRadius = s * 0.17f
        val headCenter = Offset(proj.x, torsoY - headRadius * 0.7f)

        // Dark wavy cartoon hair
        scope.drawCircle(Color(0xFF1E293B), radius = headRadius * 1.15f, center = Offset(headCenter.x, headCenter.y - headRadius * 0.1f))

        // Cartoon youthful face skin
        scope.drawCircle(Color(0xFFFFE0BD), radius = headRadius, center = headCenter)

        // Playful cartoon eyes & dimpled smile
        scope.drawCircle(Color(0xFF1E293B), radius = s * 0.022f, center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.05f))
        scope.drawCircle(Color(0xFF1E293B), radius = s * 0.022f, center = Offset(headCenter.x + headRadius * 0.35f, headCenter.y - headRadius * 0.05f))

        // Comedic wide open smile ("Wait for me!")
        scope.drawArc(
            color = Color(0xFFC0392B),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = true,
            topLeft = Offset(headCenter.x - headRadius * 0.35f, headCenter.y + headRadius * 0.1f),
            size = Size(headRadius * 0.7f, headRadius * 0.5f)
        )
    }

    private fun drawSpeedBoostLines(
        scope: DrawScope,
        width: Float,
        height: Float,
        runCycle: Float
    ) {
        val count = 12
        for (i in 0 until count) {
            val lx = (width * ((i * 19 + runCycle * 50) % 100) / 100f)
            val ly = (height * 0.3f) + (height * 0.6f * ((i * 31) % 100) / 100f)
            val len = 60f + ((i * 23) % 80)
            scope.drawLine(
                color = Color(0x88FFD166),
                start = Offset(lx, ly),
                end = Offset(lx + (if (lx > width / 2) len else -len), ly + 40f),
                strokeWidth = 3.5f
            )
        }
    }

    private fun drawSpeechBubble(
        scope: DrawScope,
        speech: SpeechBubble,
        engine: GameEngine,
        width: Float,
        height: Float,
        textMeasurer: TextMeasurer
    ) {
        val bubbleX = if (speech.isChaser) width * 0.30f else width * 0.50f
        val bubbleY = height * 0.35f

        val textLayout = textMeasurer.measure(
            text = speech.text,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A202C)
            )
        )

        val paddingH = 24f
        val paddingV = 16f
        val bW = textLayout.size.width + paddingH * 2
        val bH = textLayout.size.height + paddingV * 2

        // Bubble background with border
        scope.drawRoundRect(
            color = Color.White,
            topLeft = Offset(bubbleX - bW / 2, bubbleY - bH / 2),
            size = Size(bW, bH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
        )
        scope.drawRoundRect(
            color = if (speech.isChaser) Color(0xFF3182CE) else Color(0xFFFF9933),
            topLeft = Offset(bubbleX - bW / 2, bubbleY - bH / 2),
            size = Size(bW, bH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
            style = Stroke(width = 3.5f)
        )

        // Text
        drawScopeText(scope, textLayout, Offset(bubbleX - textLayout.size.width / 2, bubbleY - textLayout.size.height / 2))
    }

    private fun drawScopeText(
        scope: DrawScope,
        textLayout: androidx.compose.ui.text.TextLayoutResult,
        topLeft: Offset
    ) {
        scope.drawText(textLayout, topLeft = topLeft)
    }
}
