package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val totalCoins: Int = 500,
    val bestScore: Int = 0,
    val totalRuns: Int = 0,
    val selectedOutfitId: String = "outfit_classic_saffron",
    val selectedAccessoryId: String = "acc_glasses",
    val selectedTrailId: String = "trail_saffron_dust",
    val selectedLevelId: Int = 1,
    val useOnScreenControls: Boolean = false,
    val is60FpsEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val isVoiceEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true
)

@Entity(tableName = "shop_items")
data class ShopItem(
    @PrimaryKey val id: String,
    val category: String, // OUTFIT, ACCESSORY, TRAIL, POWERUP_UPGRADE
    val name: String,
    val description: String,
    val price: Int,
    val isUnlocked: Boolean,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val iconName: String
)

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rank: Int,
    val playerName: String,
    val score: Int,
    val coins: Int,
    val levelName: String,
    val timeframe: String, // DAILY, WEEKLY, ALL_TIME
    val isCurrentUser: Boolean = false
)
