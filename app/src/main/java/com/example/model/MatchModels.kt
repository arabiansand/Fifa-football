package com.example.model

import androidx.compose.ui.graphics.Color

enum class GameMode(val label: String, val description: String) {
  QUICK_MATCH("Quick Match", "1v1 vs Intelligent AI"),
  TWO_PLAYER_LOCAL("Local 2-Player", "Split screen pass & play duel on one device"),
  PENALTY_SHOOTOUT("Penalty Shootout", "Dramatic 5-round penalty drama"),
  TOURNAMENT("World Cup Bracket", "Lead your nation to tournament glory")
}

enum class AiDifficulty(val label: String, val speedFactor: Float, val shotAccuracy: Float, val reactionDelayMs: Long) {
  ROOKIE("Rookie", 0.85f, 0.70f, 220L),
  PRO("Pro", 1.05f, 0.88f, 120L),
  LEGEND("Legend", 1.25f, 0.98f, 40L)
}

enum class KitStyle {
  SOLID, VERTICAL_STRIPES, HOOPS, SASH
}

data class TeamKit(
  val id: String,
  val name: String,
  val shortName: String,
  val primaryColorHex: Long,
  val secondaryColorHex: Long,
  val accentColorHex: Long,
  val style: KitStyle = KitStyle.SOLID,
  val starPlayer: String = "Striker"
) {
  val primaryColor: Color get() = Color(primaryColorHex)
  val secondaryColor: Color get() = Color(secondaryColorHex)
  val accentColor: Color get() = Color(accentColorHex)

  companion object {
    val ENGLAND = TeamKit(
      id = "eng",
      name = "England",
      shortName = "ENG",
      primaryColorHex = 0xFFFFFFFF,
      secondaryColorHex = 0xFFCC0000,
      accentColorHex = 0xFF00247D,
      style = KitStyle.SOLID,
      starPlayer = "Henderson"
    )

    val NORWAY = TeamKit(
      id = "nor",
      name = "Norway",
      shortName = "NOR",
      primaryColorHex = 0xFFD81E2C,
      secondaryColorHex = 0xFF00205B,
      accentColorHex = 0xFFFFFFFF,
      style = KitStyle.SASH,
      starPlayer = "Haaland"
    )

    val BLUE_TITANS = TeamKit(
      id = "blue",
      name = "Royal Titans",
      shortName = "BLU",
      primaryColorHex = 0xFF0055FF,
      secondaryColorHex = 0xFF00D4FF,
      accentColorHex = 0xFFFFFFFF,
      style = KitStyle.VERTICAL_STRIPES,
      starPlayer = "Blitz"
    )

    val RED_PHOENIX = TeamKit(
      id = "red",
      name = "Red Phoenix",
      shortName = "RED",
      primaryColorHex = 0xFFFF1744,
      secondaryColorHex = 0xFFFF8A80,
      accentColorHex = 0xFFFFEA00,
      style = KitStyle.HOOPS,
      starPlayer = "Inferno"
    )

    val CYBER_EMERALD = TeamKit(
      id = "cyber",
      name = "Emerald Stars",
      shortName = "EMR",
      primaryColorHex = 0xFF00E676,
      secondaryColorHex = 0xFF004D40,
      accentColorHex = 0xFF76FF03,
      style = KitStyle.SOLID,
      starPlayer = "Viper"
    )

    val GOLDEN_EAGLES = TeamKit(
      id = "gold",
      name = "Golden Eagles",
      shortName = "GLD",
      primaryColorHex = 0xFFFFC107,
      secondaryColorHex = 0xFF212121,
      accentColorHex = 0xFFFFFFFF,
      style = KitStyle.VERTICAL_STRIPES,
      starPlayer = "Falcon"
    )

    val DEFAULT_TEAMS = listOf(ENGLAND, NORWAY, BLUE_TITANS, RED_PHOENIX, CYBER_EMERALD, GOLDEN_EAGLES)
  }
}

data class MatchStats(
  val homeShots: Int = 0,
  val awayShots: Int = 0,
  val homeShotsOnTarget: Int = 0,
  val awayShotsOnTarget: Int = 0,
  val homeSaves: Int = 0,
  val awaySaves: Int = 0,
  val homeTackles: Int = 0,
  val awayTackles: Int = 0,
  val homePossessionFrames: Long = 0,
  val awayPossessionFrames: Long = 0
) {
  val homePossessionPct: Int
    get() {
      val total = homePossessionFrames + awayPossessionFrames
      return if (total == 0L) 50 else ((homePossessionFrames * 100) / total).toInt()
    }
  val awayPossessionPct: Int
    get() = 100 - homePossessionPct
}
