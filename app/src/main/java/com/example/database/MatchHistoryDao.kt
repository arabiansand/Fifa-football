package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchHistoryDao {
  @Query("SELECT * FROM match_history ORDER BY timestamp DESC")
  fun getAllMatches(): Flow<List<MatchHistoryEntity>>

  @Query("SELECT COUNT(*) FROM match_history")
  suspend fun getTotalMatchesCount(): Int

  @Query("SELECT COUNT(*) FROM match_history WHERE winner = 'HOME'")
  suspend fun getHomeWinsCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatch(match: MatchHistoryEntity): Long

  @Query("DELETE FROM match_history")
  suspend fun clearHistory()
}
