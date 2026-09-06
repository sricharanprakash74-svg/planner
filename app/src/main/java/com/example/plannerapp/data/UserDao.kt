package com.example.plannerapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT 1")
    fun getActiveUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveUserOnce(): UserEntity?

    // Flow 1: Guest → Cloud handshake
    @Query("UPDATE users SET cloudUserId = :cloudUserId, email = :email, displayName = :displayName WHERE userId = :userId")
    suspend fun upgradeToCloudUser(userId: Long, cloudUserId: String, email: String, displayName: String)

    @Query("UPDATE users SET displayName = :displayName, avatarUrl = :avatarUrl WHERE userId = :userId")
    suspend fun updateProfile(userId: Long, displayName: String, avatarUrl: String?)

    // Flow 5: Creator toggle
    @Query("UPDATE users SET isCreator = :isCreator WHERE userId = :userId")
    suspend fun updateCreatorStatus(userId: Long, isCreator: Boolean)

    // Flow 5: Account deletion
    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: Long)

    // Timezone update
    @Query("UPDATE users SET userTimezone = :timezone WHERE userId = :userId")
    suspend fun updateTimezone(userId: Long, timezone: String)
}
