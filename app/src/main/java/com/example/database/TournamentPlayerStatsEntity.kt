package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournament_player_stats")
data class TournamentPlayerStatsEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val tournamentId: Long,
  val teamId: String,
  val teamName: String,
  val playerName: String,
  val matchesPlayed: Int = 0,
  val goalsScored: Int = 0,
  val shotsOnTarget: Int = 0,
  val tacklesWon: Int = 0
)
