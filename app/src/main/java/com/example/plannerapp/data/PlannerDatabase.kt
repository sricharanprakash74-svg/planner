package com.example.plannerapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        PlanEntity::class,
        TaskTemplateEntity::class,
        DailyCheckinEntity::class,
        BadgeEntity::class,
        PlanVoteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PlannerDatabase : RoomDatabase() {

    abstract fun plannerDao(): PlannerDao
    abstract fun userDao(): UserDao
    abstract fun badgeDao(): BadgeDao

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
