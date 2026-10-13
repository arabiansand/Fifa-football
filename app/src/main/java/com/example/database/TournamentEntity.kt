package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tournaments")
data class TournamentEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val playerTeamId: String,
  val playerTeamName: String,
  val currentRound: String = "QUARTER_FINALS", // QUARTER_FINALS, SEMI_FINALS, FINAL, COMPLETED
  val isCompleted: Boolean = false,
  val championTeamName: String? = null,
  val championTeamId: String? = null,
  val createdAt: Long = System.currentTimeMillis()
)
