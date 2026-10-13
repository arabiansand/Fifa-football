package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {
  @Query("SELECT * FROM tournaments ORDER BY createdAt DESC LIMIT 1")
  fun getLatestTournament(): Flow<TournamentEntity?>

  @Query("SELECT * FROM tournaments WHERE id = :tournamentId")
  fun getTournamentById(tournamentId: Long): Flow<TournamentEntity?>

  @Query("SELECT * FROM tournaments ORDER BY createdAt DESC")
  fun getAllTournaments(): Flow<List<TournamentEntity>>

  @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY stageOrder ASC")
  fun getMatchesForTournament(tournamentId: Long): Flow<List<TournamentMatchEntity>>

  @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId AND stageOrder = :stageOrder LIMIT 1")
  suspend fun getMatchByStageOrder(tournamentId: Long, stageOrder: Int): TournamentMatchEntity?

  @Query("SELECT * FROM tournament_player_stats WHERE tournamentId = :tournamentId ORDER BY goalsScored DESC, matchesPlayed ASC")
  fun getPlayerStatsForTournament(tournamentId: Long): Flow<List<TournamentPlayerStatsEntity>>

  @Query("SELECT * FROM tournament_player_stats WHERE tournamentId = :tournamentId AND playerName = :playerName LIMIT 1")
  suspend fun getPlayerStats(tournamentId: Long, playerName: String): TournamentPlayerStatsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTournament(tournament: TournamentEntity): Long

  @Update
  suspend fun updateTournament(tournament: TournamentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatches(matches: List<TournamentMatchEntity>)

  @Update
  suspend fun updateMatch(match: TournamentMatchEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdatePlayerStats(stats: TournamentPlayerStatsEntity)

  @Query("DELETE FROM tournaments WHERE id = :tournamentId")
  suspend fun deleteTournament(tournamentId: Long)

  @Query("DELETE FROM tournament_matches WHERE tournamentId = :tournamentId")
  suspend fun deleteMatchesForTournament(tournamentId: Long)

  @Query("DELETE FROM tournament_player_stats WHERE tournamentId = :tournamentId")
  suspend fun deletePlayerStatsForTournament(tournamentId: Long)
}
