package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
  @PrimaryKey
  val id: Int = 1, // Single player profile row
  val playerName: String = "Captain Striker",
  val playerTitle: String = "World Cup Legend",
  val favoriteTeamId: String = "eng",
  val favoriteTeamName: String = "England",
  val primaryColorHex: Long = 0xFFFFFFFF,
  val secondaryColorHex: Long = 0xFFCC0000,
  val accentColorHex: Long = 0xFF00247D,
  val kitStyle: String = "SOLID",
  val avatarIcon: String = "⚽",
  val jerseyNumber: Int = 10,
  val updatedAt: Long = System.currentTimeMillis()
)
