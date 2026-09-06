package com.example.plannerapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: BadgeEntity): Long

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun getBadgesForUser(userId: Long): Flow<List<BadgeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM badges WHERE userId = :userId AND badgeType = :badgeType)")
    suspend fun hasBadge(userId: Long, badgeType: String): Boolean

    @Query("SELECT * FROM badges WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncBadges(): List<BadgeEntity>

    @Query("UPDATE badges SET syncStatus = 'SYNCED' WHERE badgeId IN (:ids)")
    suspend fun markBadgesSynced(ids: List<Long>)
}
