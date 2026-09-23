package com.example.game

enum class ObstacleType(
    val displayName: String,
    val canJumpOver: Boolean,
    val canSlideUnder: Boolean,
    val widthInLanes: Int = 1,
    val heightUnits: Float = 1.0f
) {
    TRAFFIC_BARRICADE("Traffic Barricade", canJumpOver = true, canSlideUnder = false, widthInLanes = 1, heightUnits = 1.2f),
    WOODEN_BOX("Wooden Box", canJumpOver = true, canSlideUnder = false, widthInLanes = 1, heightUnits = 0.9f),
    ROAD_CONES("Road Cones", canJumpOver = true, canSlideUnder = false, widthInLanes = 1, heightUnits = 0.7f),
    POTHOLE("Pothole", canJumpOver = true, canSlideUnder = false, widthInLanes = 1, heightUnits = 0.1f),
    SMALL_WALL("Small Wall", canJumpOver = true, canSlideUnder = false, widthInLanes = 1, heightUnits = 1.1f),
    PARK_BENCH("Park Bench", canJumpOver = true, canSlideUnder = false, widthInLanes = 1, heightUnits = 0.8f),
    CONSTRUCTION_BARRIER("Construction Barrier", canJumpOver = true, canSlideUnder = false, widthInLanes = 1, heightUnits = 1.3f),
    OVERHEAD_BANNER("Low Festival Banner", canJumpOver = false, canSlideUnder = true, widthInLanes = 1, heightUnits = 2.0f),
    CARTOON_COW("Cartoon Cow", canJumpOver = false, canSlideUnder = false, widthInLanes = 1, heightUnits = 1.4f)
}

enum class PowerUpType(
    val title: String,
    val durationSeconds: Float,
    val badgeColorHex: Long
) {
    COIN_MAGNET("Coin Magnet", 10.0f, 0xFFFFB703),
    SHIELD("Shield Bubble", 12.0f, 0xFF219EBC),
    SPEED_BOOST("Turbo Boost", 7.0f, 0xFFFB8500),
    DOUBLE_COINS("2X Multiplier", 12.0f, 0xFF52B788),
    SUPER_JUMP("Super Jump", 10.0f, 0xFF9D4EDD)
}

data class ObstacleInstance(
    val id: Long,
    val type: ObstacleType,
    val lane: Int, // -1, 0, 1
    var z: Float,  // Distance ahead of camera/player
    val isCleared: Boolean = false
)

data class CoinInstance(
    val id: Long,
    var lane: Int,
    var z: Float,
    var y: Float = 0.4f,
    var isCollected: Boolean = false,
    var attractProgress: Float = 0.0f // For magnet pull
)

data class PowerUpInstance(
    val id: Long,
    val type: PowerUpType,
    val lane: Int,
    var z: Float,
    var isCollected: Boolean = false
)

enum class GameEnvironment(
    val levelNumber: Int,
    val title: String,
    val subtitle: String,
    val roadColorHex: Long,
    val roadsideColorHex: Long,
    val skyTopColorHex: Long,
    val skyBottomColorHex: Long,
    val horizonLineColorHex: Long
) {
    INDIAN_CITY(
        levelNumber = 1,
        title = "Indian City",
        subtitle = "Vibrant streets with shops & auto-rickshaws",
        roadColorHex = 0xFF2B2D42,
        roadsideColorHex = 0xFF8D99AE,
        skyTopColorHex = 0xFF4EA8DE,
        skyBottomColorHex = 0xFF90E0EF,
        horizonLineColorHex = 0xFFF77F00
    ),
    VILLAGE(
        levelNumber = 2,
        title = "Desi Village",
        subtitle = "Dusty roads, thatched huts & lush fields",
        roadColorHex = 0xFF8B5E3C,
        roadsideColorHex = 0xFF588157,
        skyTopColorHex = 0xFF70E000,
        skyBottomColorHex = 0xFFFFF3B0,
        horizonLineColorHex = 0xFFE07A5F
    ),
    MOUNTAIN_ROAD(
        levelNumber = 3,
        title = "Mountain Pass",
        subtitle = "Scenic winding cliffside with floating clouds",
        roadColorHex = 0xFF3D405B,
        roadsideColorHex = 0xFF6C757D,
        skyTopColorHex = 0xFF1D3557,
        skyBottomColorHex = 0xFFA8DADC,
        horizonLineColorHex = 0xFFE63946
    ),
    FOREST(
        levelNumber = 4,
        title = "Jungle Trail",
        subtitle = "Ancient trees, riverbanks & wooden bridges",
        roadColorHex = 0xFF4A3525,
        roadsideColorHex = 0xFF2D6A4F,
        skyTopColorHex = 0xFF1B4332,
        skyBottomColorHex = 0xFF74C69D,
        horizonLineColorHex = 0xFFD8F3DC
    ),
    NIGHT_CITY(
        levelNumber = 5,
        title = "Neon Night City",
        subtitle = "Glowing neon skyline, traffic & festive lights",
        roadColorHex = 0xFF101018,
        roadsideColorHex = 0xFF1A1A2E,
        skyTopColorHex = 0xFF0D1B2A,
        skyBottomColorHex = 0xFF415A77,
        horizonLineColorHex = 0xFFFF007F
    );

    companion object {
        fun forScore(score: Int): GameEnvironment {
            return when {
                score < 1500 -> INDIAN_CITY
                score < 3500 -> VILLAGE
                score < 6000 -> MOUNTAIN_ROAD
                score < 9000 -> FOREST
                else -> NIGHT_CITY
            }
        }
    }
}

data class SpeechBubble(
    val text: String,
    val isChaser: Boolean = false,
    val durationSeconds: Float = 2.2f,
    var remainingSeconds: Float = 2.2f
)

enum class FunnyVocalType {
    RUNNER_TALK,
    RUNNER_JUMP,
    RUNNER_SLIDE,
    RUNNER_CHEER,
    RUNNER_GIGGLE,
    RUNNER_OUCH,
    CHASER_SHOUT,
    CHASER_GASP,
    CHASER_LAUGH,
    CHASER_TAUNT,
    SAD_TROMBONE,
    CHA_CHING,
    POWERUP_FANFARE
}
