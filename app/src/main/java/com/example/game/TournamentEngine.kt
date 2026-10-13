package com.example.game

import com.example.model.TeamKit
import kotlin.random.Random

data class TournamentMatch(
  val id: String,
  val roundName: String,
  val teamA: TeamKit,
  val teamB: TeamKit,
  var scoreA: Int? = null,
  var scoreB: Int? = null,
  var isPlayerMatch: Boolean = false,
  var isCompleted: Boolean = false,
  var winner: TeamKit? = null
)

class TournamentEngine(val playerTeam: TeamKit) {
  val allTeams = listOf(
    TeamKit.ENGLAND,
    TeamKit.NORWAY,
    TeamKit.BLUE_TITANS,
    TeamKit.RED_PHOENIX,
    TeamKit.CYBER_EMERALD,
    TeamKit.GOLDEN_EAGLES,
    TeamKit(
      id = "fra",
      name = "France",
      shortName = "FRA",
      primaryColorHex = 0xFF002395,
      secondaryColorHex = 0xFFED2939,
      accentColorHex = 0xFFFFFFFF,
      starPlayer = "Mbappé"
    ),
    TeamKit(
      id = "arg",
      name = "Argentina",
      shortName = "ARG",
      primaryColorHex = 0xFF75AADB,
      secondaryColorHex = 0xFFFFFFFF,
      accentColorHex = 0xFFF6B40E,
      starPlayer = "Messi"
    )
  )

  var quarterFinals = mutableListOf<TournamentMatch>()
  var semiFinals = mutableListOf<TournamentMatch>()
  var grandFinal: TournamentMatch? = null
  var tournamentChampion: TeamKit? = null

  init {
    initializeBracket()
  }

  private fun initializeBracket() {
    val pool = allTeams.toMutableList()
    // Make sure playerTeam is in the pool
    if (!pool.any { it.id == playerTeam.id }) {
      pool[0] = playerTeam
    } else {
      // Move playerTeam to first slot
      pool.removeIf { it.id == playerTeam.id }
      pool.add(0, playerTeam)
    }

    // Shuffle opponents
    val opponents = pool.drop(1).shuffled()
    val finalTeams = listOf(playerTeam) + opponents

    // QF 1: Player vs Opponent 1
    quarterFinals.add(
      TournamentMatch(
        id = "QF1",
        roundName = "Quarter-Final 1",
        teamA = finalTeams[0],
        teamB = finalTeams[1],
        isPlayerMatch = true
      )
    )

    // QF 2:
    quarterFinals.add(
      TournamentMatch(
        id = "QF2",
        roundName = "Quarter-Final 2",
        teamA = finalTeams[2],
        teamB = finalTeams[3]
      )
    )

    // QF 3:
    quarterFinals.add(
      TournamentMatch(
        id = "QF3",
        roundName = "Quarter-Final 3",
        teamA = finalTeams[4],
        teamB = finalTeams[5]
      )
    )

    // QF 4:
    quarterFinals.add(
      TournamentMatch(
        id = "QF4",
        roundName = "Quarter-Final 4",
        teamA = finalTeams[6],
        teamB = finalTeams[7]
      )
    )

    // Auto simulate non-player QFs
    simulateNonPlayerMatches()
  }

  fun simulateNonPlayerMatches() {
    // Simulate any pending non-player matches in QF
    for (m in quarterFinals) {
      if (!m.isPlayerMatch && !m.isCompleted) {
        val sA = Random.nextInt(0, 4)
        var sB = Random.nextInt(0, 4)
        if (sA == sB) sB += 1 // ensure decisive outcome
        m.scoreA = sA
        m.scoreB = sB
        m.isCompleted = true
        m.winner = if (sA > sB) m.teamA else m.teamB
      }
    }

    // Check if semi-finals can be populated
    if (quarterFinals.all { it.isCompleted } && semiFinals.isEmpty()) {
      val w1 = quarterFinals[0].winner!!
      val w2 = quarterFinals[1].winner!!
      val w3 = quarterFinals[2].winner!!
      val w4 = quarterFinals[3].winner!!

      semiFinals.add(
        TournamentMatch(
          id = "SF1",
          roundName = "Semi-Final 1",
          teamA = w1,
          teamB = w2,
          isPlayerMatch = (w1.id == playerTeam.id || w2.id == playerTeam.id)
        )
      )
      semiFinals.add(
        TournamentMatch(
          id = "SF2",
          roundName = "Semi-Final 2",
          teamA = w3,
          teamB = w4,
          isPlayerMatch = (w3.id == playerTeam.id || w4.id == playerTeam.id)
        )
      )

      // Simulate non-player SF
      for (m in semiFinals) {
        if (!m.isPlayerMatch && !m.isCompleted) {
          var sA = Random.nextInt(1, 4)
          var sB = Random.nextInt(0, 3)
          if (sA == sB) sA += 1
          m.scoreA = sA
          m.scoreB = sB
          m.isCompleted = true
          m.winner = if (sA > sB) m.teamA else m.teamB
        }
      }
    }

    // Check if grand final can be populated
    if (semiFinals.isNotEmpty() && semiFinals.all { it.isCompleted } && grandFinal == null) {
      val w1 = semiFinals[0].winner!!
      val w2 = semiFinals[1].winner!!
      grandFinal = TournamentMatch(
        id = "FINAL",
        roundName = "World Cup Final",
        teamA = w1,
        teamB = w2,
        isPlayerMatch = (w1.id == playerTeam.id || w2.id == playerTeam.id)
      )

      if (!grandFinal!!.isPlayerMatch) {
        var sA = Random.nextInt(2, 5)
        var sB = Random.nextInt(0, 3)
        if (sA == sB) sA += 1
        grandFinal!!.scoreA = sA
        grandFinal!!.scoreB = sB
        grandFinal!!.isCompleted = true
        grandFinal!!.winner = if (sA > sB) grandFinal!!.teamA else grandFinal!!.teamB
        tournamentChampion = grandFinal!!.winner
      }
    }
  }

  fun recordPlayerMatchResult(matchId: String, pScore: Int, oppScore: Int) {
    val currentMatch = when {
      quarterFinals.any { it.id == matchId } -> quarterFinals.first { it.id == matchId }
      semiFinals.any { it.id == matchId } -> semiFinals.first { it.id == matchId }
      grandFinal?.id == matchId -> grandFinal
      else -> null
    } ?: return

    val isTeamA = currentMatch.teamA.id == playerTeam.id
    currentMatch.scoreA = if (isTeamA) pScore else oppScore
    currentMatch.scoreB = if (isTeamA) oppScore else pScore
    currentMatch.isCompleted = true
    currentMatch.winner = if (currentMatch.scoreA!! > currentMatch.scoreB!!) currentMatch.teamA else currentMatch.teamB

    if (currentMatch.id == "FINAL") {
      tournamentChampion = currentMatch.winner
    }

    simulateNonPlayerMatches()
  }

  fun getNextPlayerMatch(): TournamentMatch? {
    quarterFinals.firstOrNull { it.isPlayerMatch && !it.isCompleted }?.let { return it }
    semiFinals.firstOrNull { it.isPlayerMatch && !it.isCompleted }?.let { return it }
    grandFinal?.let { if (it.isPlayerMatch && !it.isCompleted) return it }
    return null
  }
}
