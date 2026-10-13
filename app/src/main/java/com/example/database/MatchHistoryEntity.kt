package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val mode: String,
  val homeTeamName: String,
  val awayTeamName: String,
  val homeScore: Int,
  val awayScore: Int,
  val homeShots: Int,
  val awayShots: Int,
  val homePossessionPct: Int,
  val winner: String
)
