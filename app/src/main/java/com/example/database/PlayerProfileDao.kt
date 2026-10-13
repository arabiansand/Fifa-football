package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {
  @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
  fun getPlayerProfile(): Flow<PlayerProfileEntity?>

  @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
  suspend fun getPlayerProfileSync(): PlayerProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun savePlayerProfile(profile: PlayerProfileEntity)
}
