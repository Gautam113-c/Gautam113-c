package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfileFlow(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getPlayerProfile(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfile)

    @Update
    suspend fun updateProfile(profile: PlayerProfile)

    @Query("UPDATE player_profile SET totalCoins = totalCoins + :coinsEarned, bestScore = CASE WHEN :newScore > bestScore THEN :newScore ELSE bestScore END, totalRuns = totalRuns + 1 WHERE id = 1")
    suspend fun recordRunResults(coinsEarned: Int, newScore: Int)

    @Query("UPDATE player_profile SET totalCoins = totalCoins - :cost WHERE id = 1")
    suspend fun deductCoins(cost: Int)

    @Query("UPDATE player_profile SET selectedOutfitId = :outfitId WHERE id = 1")
    suspend fun setSelectedOutfit(outfitId: String)

    @Query("UPDATE player_profile SET selectedAccessoryId = :accessoryId WHERE id = 1")
    suspend fun setSelectedAccessory(accessoryId: String)

    @Query("UPDATE player_profile SET selectedTrailId = :trailId WHERE id = 1")
    suspend fun setSelectedTrail(trailId: String)

    @Query("UPDATE player_profile SET useOnScreenControls = :enabled WHERE id = 1")
    suspend fun setOnScreenControls(enabled: Boolean)

    @Query("UPDATE player_profile SET is60FpsEnabled = :enabled WHERE id = 1")
    suspend fun setFpsMode(enabled: Boolean)

    @Query("UPDATE player_profile SET isSoundEnabled = :sound, isMusicEnabled = :music, isVoiceEnabled = :voice WHERE id = 1")
    suspend fun setAudioSettings(sound: Boolean, music: Boolean, voice: Boolean)

    // Shop
    @Query("SELECT * FROM shop_items")
    fun getAllShopItemsFlow(): Flow<List<ShopItem>>

    @Query("SELECT * FROM shop_items WHERE category = :category")
    fun getShopItemsByCategoryFlow(category: String): Flow<List<ShopItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShopItems(items: List<ShopItem>)

    @Query("UPDATE shop_items SET isUnlocked = 1 WHERE id = :itemId")
    suspend fun unlockShopItem(itemId: String)

    // Leaderboard
    @Query("SELECT * FROM leaderboard_entries WHERE timeframe = :timeframe ORDER BY score DESC LIMIT 25")
    fun getLeaderboardFlow(timeframe: String): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<LeaderboardEntry>)

    @Query("DELETE FROM leaderboard_entries")
    suspend fun clearLeaderboard()
}

@Database(entities = [PlayerProfile::class, ShopItem::class, LeaderboardEntry::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
