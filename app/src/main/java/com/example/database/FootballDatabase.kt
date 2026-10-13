package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    MatchHistoryEntity::class,
    TournamentEntity::class,
    TournamentMatchEntity::class,
    TournamentPlayerStatsEntity::class,
    PlayerProfileEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class FootballDatabase : RoomDatabase() {
  abstract fun matchHistoryDao(): MatchHistoryDao
  abstract fun tournamentDao(): TournamentDao
  abstract fun playerProfileDao(): PlayerProfileDao

  companion object {
    @Volatile
    private var INSTANCE: FootballDatabase? = null

    fun getDatabase(context: Context): FootballDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          FootballDatabase::class.java,
          "football_duel_database"
        ).fallbackToDestructiveMigration(true).build()
        INSTANCE = instance
        instance
      }
    }
  }
}
