package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournament_matches")
data class TournamentMatchEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val tournamentId: Long,
  val roundName: String, // e.g. "Quarter-Final 1"
  val roundCategory: String, // "QF", "SF", "FINAL"
  val stageOrder: Int, // 1..4 (QF), 5..6 (SF), 7 (FINAL)
  val teamAId: String,
  val teamAName: String,
  val teamAShortName: String,
  val teamAColorHex: Long,
  val teamAStarPlayer: String,
  val teamBId: String,
  val teamBName: String,
  val teamBShortName: String,
  val teamBColorHex: Long,
  val teamBStarPlayer: String,
  val scoreA: Int? = null,
  val scoreB: Int? = null,
  val isPlayerMatch: Boolean = false,
  val isCompleted: Boolean = false,
  val winnerTeamId: String? = null,
  val winnerTeamName: String? = null,
  val nextMatchStageOrder: Int? = null, // Which stage this feeds into
  val nextMatchSlot: String? = null // "A" or "B"
)
