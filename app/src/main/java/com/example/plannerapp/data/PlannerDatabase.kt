package com.example.plannerapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.plannerapp.credits.CreditDao
import com.example.plannerapp.credits.CreditTransactionEntity
import com.example.plannerapp.credits.StreakFreezeEntity
import com.example.plannerapp.sharing.SharedPlanEntity
import com.example.plannerapp.sharing.SharingDao

@Database(
    entities = [
        UserEntity::class,
        PlanEntity::class,
        TaskTemplateEntity::class,
        DailyCheckinEntity::class,
        PlanDayCompletionEntity::class,
        BadgeEntity::class,
        PlanVoteEntity::class,
        JoinedCommunityEntity::class,
        CreditTransactionEntity::class,
        StreakFreezeEntity::class,
        SharedPlanEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class PlannerDatabase : RoomDatabase() {

    abstract fun plannerDao(): PlannerDao
    abstract fun userDao(): UserDao
    abstract fun badgeDao(): BadgeDao
    abstract fun creditDao(): CreditDao
    abstract fun sharingDao(): SharingDao

    companion object {
        @Volatile
        private var INSTANCE: PlannerDatabase? = null

        fun getDatabase(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
