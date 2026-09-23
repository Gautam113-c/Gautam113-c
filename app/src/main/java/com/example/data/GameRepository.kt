package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        GameDatabase::class.java,
        "political_run_db"
    ).build()

    val dao = database.gameDao()

    val profileFlow: Flow<PlayerProfile?> = dao.getPlayerProfileFlow()
    val shopItemsFlow: Flow<List<ShopItem>> = dao.getAllShopItemsFlow()

    suspend fun initializeDefaultData() {
        val existingProfile = dao.getPlayerProfile()
        if (existingProfile == null) {
            dao.insertOrUpdateProfile(
                PlayerProfile(
                    id = 1,
                    totalCoins = 250,
                    bestScore = 0,
                    totalRuns = 0
                )
            )
        }

        // Initialize shop items
        val defaultItems = listOf(
            // Outfits
            ShopItem(
                id = "outfit_classic_saffron",
                category = "OUTFIT",
                name = "Classic Saffron",
                description = "Iconic saffron Nehru vest over crisp white cotton kurta.",
                price = 0,
                isUnlocked = true,
                primaryColorHex = 0xFFFF9933,
                secondaryColorHex = 0xFFFFFFFF,
                iconName = "ic_vest"
            ),
            ShopItem(
                id = "outfit_royal_blue",
                category = "OUTFIT",
                name = "Royal Navy Sherwani",
                description = "Deep regal blue ceremonial vest with silk trim.",
                price = 300,
                isUnlocked = false,
                primaryColorHex = 0xFF1A365D,
                secondaryColorHex = 0xFFCBD5E0,
                iconName = "ic_vest"
            ),
            ShopItem(
                id = "outfit_yoga_white",
                category = "OUTFIT",
                name = "Zen Yoga Kurta",
                description = "Pure peace white yoga attire for maximum running flexibility.",
                price = 500,
                isUnlocked = false,
                primaryColorHex = 0xFFF7FAFC,
                secondaryColorHex = 0xFFE2E8F0,
                iconName = "ic_vest"
            ),
            ShopItem(
                id = "outfit_sport_tricolor",
                category = "OUTFIT",
                name = "Tricolor Sprinter",
                description = "Athletic sporty running jacket with saffron, white, and green stripes!",
                price = 800,
                isUnlocked = false,
                primaryColorHex = 0xFF138808,
                secondaryColorHex = 0xFFFF9933,
                iconName = "ic_vest"
            ),
            ShopItem(
                id = "outfit_maharaja_gold",
                category = "OUTFIT",
                name = "Maharaja Gold",
                description = "Gleaming golden silk ceremonial vest with royal brocade.",
                price = 1500,
                isUnlocked = false,
                primaryColorHex = 0xFFFFD700,
                secondaryColorHex = 0xFF8B0000,
                iconName = "ic_vest"
            ),

            // Accessories
            ShopItem(
                id = "acc_glasses",
                category = "ACCESSORY",
                name = "Signature Spectacles",
                description = "Round iconic cartoon reading glasses.",
                price = 0,
                isUnlocked = true,
                primaryColorHex = 0xFF4A5568,
                secondaryColorHex = 0xFF718096,
                iconName = "ic_glasses"
            ),
            ShopItem(
                id = "acc_turban_saffron",
                category = "ACCESSORY",
                name = "Royal Saffron Pagdi",
                description = "Grand Rajasthani ceremonial turban with fluttering tail.",
                price = 400,
                isUnlocked = false,
                primaryColorHex = 0xFFFF6F00,
                secondaryColorHex = 0xFFFFD54F,
                iconName = "ic_turban"
            ),
            ShopItem(
                id = "acc_sunglasses",
                category = "ACCESSORY",
                name = "VIP Aviator Shades",
                description = "Super cool tinted aviator glasses with gold rims.",
                price = 600,
                isUnlocked = false,
                primaryColorHex = 0xFF2D3748,
                secondaryColorHex = 0xFFD69E2E,
                iconName = "ic_glasses"
            ),
            ShopItem(
                id = "acc_chai_cup",
                category = "ACCESSORY",
                name = "Clay Kulhad Chai",
                description = "Steaming fragrant masala tea in an earthy terracotta cup.",
                price = 750,
                isUnlocked = false,
                primaryColorHex = 0xFFC05621,
                secondaryColorHex = 0xFFECC94B,
                iconName = "ic_cup"
            ),

            // Trails
            ShopItem(
                id = "trail_saffron_dust",
                category = "TRAIL",
                name = "Saffron Dust",
                description = "Golden saffron dust swirls kicking up behind your steps.",
                price = 0,
                isUnlocked = true,
                primaryColorHex = 0xFFFF9933,
                secondaryColorHex = 0xFFFFB366,
                iconName = "ic_trail"
            ),
            ShopItem(
                id = "trail_rainbow",
                category = "TRAIL",
                name = "Festival of Colors",
                description = "Vibrant Holi color powder bursts at high speed!",
                price = 700,
                isUnlocked = false,
                primaryColorHex = 0xFFE53E3E,
                secondaryColorHex = 0xFF3182CE,
                iconName = "ic_trail"
            ),
            ShopItem(
                id = "trail_electric",
                category = "TRAIL",
                name = "Vikas Electric Spark",
                description = "Crackling energetic lightning sparks running across the road.",
                price = 1200,
                isUnlocked = false,
                primaryColorHex = 0xFF00B4D8,
                secondaryColorHex = 0xFF90E0EF,
                iconName = "ic_trail"
            ),

            // Shoes
            ShopItem(
                id = "shoe_classic_mojri",
                category = "SHOES",
                name = "Classic Mojri",
                description = "Traditional comfortable embroidered leather slip-ons.",
                price = 0,
                isUnlocked = true,
                primaryColorHex = 0xFF7B341E,
                secondaryColorHex = 0xFFD69E2E,
                iconName = "ic_shoe"
            ),
            ShopItem(
                id = "shoe_turbo_sneakers",
                category = "SHOES",
                name = "Turbo Sprinter Kicks",
                description = "High-tech neon running shoes with springy soles!",
                price = 650,
                isUnlocked = false,
                primaryColorHex = 0xFFE53E3E,
                secondaryColorHex = 0xFFFFFF00,
                iconName = "ic_shoe"
            )
        )
        dao.insertShopItems(defaultItems)

        // Seed Leaderboard
        seedLeaderboardIfEmpty()
    }

    private suspend fun seedLeaderboardIfEmpty() {
        val daily = dao.getLeaderboardFlow("DAILY").firstOrNull() ?: emptyList()
        if (daily.isEmpty()) {
            val sampleDaily = listOf(
                LeaderboardEntry(rank = 1, playerName = "Chaiwala Express", score = 18450, coins = 420, levelName = "Night City", timeframe = "DAILY"),
                LeaderboardEntry(rank = 2, playerName = "Speedy Vikas", score = 15200, coins = 350, levelName = "Mountain Road", timeframe = "DAILY"),
                LeaderboardEntry(rank = 3, playerName = "Desi Sprinter", score = 12900, coins = 290, levelName = "Forest", timeframe = "DAILY"),
                LeaderboardEntry(rank = 4, playerName = "You (Player)", score = 8500, coins = 180, levelName = "Village", timeframe = "DAILY", isCurrentUser = true),
                LeaderboardEntry(rank = 5, playerName = "Metro Runner 99", score = 7300, coins = 140, levelName = "Indian City", timeframe = "DAILY"),
                LeaderboardEntry(rank = 6, playerName = "Bhangra Racer", score = 5900, coins = 110, levelName = "Indian City", timeframe = "DAILY")
            )
            val sampleWeekly = listOf(
                LeaderboardEntry(rank = 1, playerName = "Super Bullet Train", score = 42800, coins = 980, levelName = "Night City", timeframe = "WEEKLY"),
                LeaderboardEntry(rank = 2, playerName = "Chaiwala Express", score = 38500, coins = 890, levelName = "Night City", timeframe = "WEEKLY"),
                LeaderboardEntry(rank = 3, playerName = "Desi Sprinter", score = 31200, coins = 720, levelName = "Mountain Road", timeframe = "WEEKLY"),
                LeaderboardEntry(rank = 4, playerName = "Speedy Vikas", score = 27600, coins = 640, levelName = "Forest", timeframe = "WEEKLY"),
                LeaderboardEntry(rank = 5, playerName = "You (Player)", score = 18400, coins = 410, levelName = "Village", timeframe = "WEEKLY", isCurrentUser = true),
                LeaderboardEntry(rank = 6, playerName = "Rocket Rickshaw", score = 14200, coins = 310, levelName = "Indian City", timeframe = "WEEKLY")
            )
            val sampleAllTime = listOf(
                LeaderboardEntry(rank = 1, playerName = "Legend of Gujarat", score = 98500, coins = 2400, levelName = "Night City", timeframe = "ALL_TIME"),
                LeaderboardEntry(rank = 2, playerName = "Super Bullet Train", score = 84200, coins = 1950, levelName = "Night City", timeframe = "ALL_TIME"),
                LeaderboardEntry(rank = 3, playerName = "Chaiwala Express", score = 76900, coins = 1820, levelName = "Night City", timeframe = "ALL_TIME"),
                LeaderboardEntry(rank = 4, playerName = "Desi Sprinter", score = 65400, coins = 1530, levelName = "Mountain Road", timeframe = "ALL_TIME"),
                LeaderboardEntry(rank = 5, playerName = "Speedy Vikas", score = 51200, coins = 1200, levelName = "Forest", timeframe = "ALL_TIME"),
                LeaderboardEntry(rank = 6, playerName = "You (Player)", score = 25000, coins = 580, levelName = "Village", timeframe = "ALL_TIME", isCurrentUser = true)
            )
            dao.insertLeaderboardEntries(sampleDaily + sampleWeekly + sampleAllTime)
        }
    }

    suspend fun recordRun(coinsEarned: Int, score: Int, levelName: String) {
        dao.recordRunResults(coinsEarned, score)
        // Update player's entry on daily leaderboard if better
        dao.insertLeaderboardEntries(
            listOf(
                LeaderboardEntry(
                    rank = 3,
                    playerName = "You (Player)",
                    score = score,
                    coins = coinsEarned,
                    levelName = levelName,
                    timeframe = "DAILY",
                    isCurrentUser = true
                )
            )
        )
    }

    suspend fun purchaseItem(item: ShopItem): Boolean {
        val profile = dao.getPlayerProfile() ?: return false
        if (profile.totalCoins >= item.price && !item.isUnlocked) {
            dao.deductCoins(item.price)
            dao.unlockShopItem(item.id)
            return true
        }
        return false
    }
}
