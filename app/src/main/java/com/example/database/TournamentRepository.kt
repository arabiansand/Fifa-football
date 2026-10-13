package com.example.database

import com.example.model.TeamKit
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class TournamentRepository(private val tournamentDao: TournamentDao, private val matchHistoryDao: MatchHistoryDao) {

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

  fun observeLatestTournament(): Flow<TournamentEntity?> = tournamentDao.getLatestTournament()

  fun observeMatches(tournamentId: Long): Flow<List<TournamentMatchEntity>> =
    tournamentDao.getMatchesForTournament(tournamentId)

  fun observePlayerStats(tournamentId: Long): Flow<List<TournamentPlayerStatsEntity>> =
    tournamentDao.getPlayerStatsForTournament(tournamentId)

  suspend fun createNewTournament(playerTeam: TeamKit, name: String = "World Cup 2026"): Long {
    // 1. Prepare 8 teams
    val pool = allTeams.toMutableList()
    if (!pool.any { it.id == playerTeam.id }) {
      pool[0] = playerTeam
    } else {
      pool.removeIf { it.id == playerTeam.id }
      pool.add(0, playerTeam)
    }

    val opponents = pool.drop(1).shuffled()
    val finalTeams = listOf(playerTeam) + opponents

    // 2. Insert Tournament
    val tournamentId = tournamentDao.insertTournament(
      TournamentEntity(
        name = name,
        playerTeamId = playerTeam.id,
        playerTeamName = playerTeam.name,
        currentRound = "QUARTER_FINALS"
      )
    )

    // 3. Initialize Player Stats for all star players
    finalTeams.forEach { team ->
      tournamentDao.insertOrUpdatePlayerStats(
        TournamentPlayerStatsEntity(
          tournamentId = tournamentId,
          teamId = team.id,
          teamName = team.name,
          playerName = team.starPlayer,
          matchesPlayed = 0,
          goalsScored = 0
        )
      )
    }

    // 4. Create Matches
    // QFs (Stages 1..4)
    val qf1 = TournamentMatchEntity(
      tournamentId = tournamentId,
      roundName = "Quarter-Final 1",
      roundCategory = "QF",
      stageOrder = 1,
      teamAId = finalTeams[0].id,
      teamAName = finalTeams[0].name,
      teamAShortName = finalTeams[0].shortName,
      teamAColorHex = finalTeams[0].primaryColorHex,
      teamAStarPlayer = finalTeams[0].starPlayer,
      teamBId = finalTeams[1].id,
      teamBName = finalTeams[1].name,
      teamBShortName = finalTeams[1].shortName,
      teamBColorHex = finalTeams[1].primaryColorHex,
      teamBStarPlayer = finalTeams[1].starPlayer,
      isPlayerMatch = true,
      nextMatchStageOrder = 5,
      nextMatchSlot = "A"
    )

    val qf2 = TournamentMatchEntity(
      tournamentId = tournamentId,
      roundName = "Quarter-Final 2",
      roundCategory = "QF",
      stageOrder = 2,
      teamAId = finalTeams[2].id,
      teamAName = finalTeams[2].name,
      teamAShortName = finalTeams[2].shortName,
      teamAColorHex = finalTeams[2].primaryColorHex,
      teamAStarPlayer = finalTeams[2].starPlayer,
      teamBId = finalTeams[3].id,
      teamBName = finalTeams[3].name,
      teamBShortName = finalTeams[3].shortName,
      teamBColorHex = finalTeams[3].primaryColorHex,
      teamBStarPlayer = finalTeams[3].starPlayer,
      isPlayerMatch = false,
      nextMatchStageOrder = 5,
      nextMatchSlot = "B"
    )

    val qf3 = TournamentMatchEntity(
      tournamentId = tournamentId,
      roundName = "Quarter-Final 3",
      roundCategory = "QF",
      stageOrder = 3,
      teamAId = finalTeams[4].id,
      teamAName = finalTeams[4].name,
      teamAShortName = finalTeams[4].shortName,
      teamAColorHex = finalTeams[4].primaryColorHex,
      teamAStarPlayer = finalTeams[4].starPlayer,
      teamBId = finalTeams[5].id,
      teamBName = finalTeams[5].name,
      teamBShortName = finalTeams[5].shortName,
      teamBColorHex = finalTeams[5].primaryColorHex,
      teamBStarPlayer = finalTeams[5].starPlayer,
      isPlayerMatch = false,
      nextMatchStageOrder = 6,
      nextMatchSlot = "A"
    )

    val qf4 = TournamentMatchEntity(
      tournamentId = tournamentId,
      roundName = "Quarter-Final 4",
      roundCategory = "QF",
      stageOrder = 4,
      teamAId = finalTeams[6].id,
      teamAName = finalTeams[6].name,
      teamAShortName = finalTeams[6].shortName,
      teamAColorHex = finalTeams[6].primaryColorHex,
      teamAStarPlayer = finalTeams[6].starPlayer,
      teamBId = finalTeams[7].id,
      teamBName = finalTeams[7].name,
      teamBShortName = finalTeams[7].shortName,
      teamBColorHex = finalTeams[7].primaryColorHex,
      teamBStarPlayer = finalTeams[7].starPlayer,
      isPlayerMatch = false,
      nextMatchStageOrder = 6,
      nextMatchSlot = "B"
    )

    // Semi-Finals (Stages 5..6, placeholders)
    val sf1 = TournamentMatchEntity(
      tournamentId = tournamentId,
      roundName = "Semi-Final 1",
      roundCategory = "SF",
      stageOrder = 5,
      teamAId = "tbd",
      teamAName = "Winner QF1",
      teamAShortName = "TBD",
      teamAColorHex = 0xFF555555,
      teamAStarPlayer = "TBD",
      teamBId = "tbd",
      teamBName = "Winner QF2",
      teamBShortName = "TBD",
      teamBColorHex = 0xFF555555,
      teamBStarPlayer = "TBD",
      nextMatchStageOrder = 7,
      nextMatchSlot = "A"
    )

    val sf2 = TournamentMatchEntity(
      tournamentId = tournamentId,
      roundName = "Semi-Final 2",
      roundCategory = "SF",
      stageOrder = 6,
      teamAId = "tbd",
      teamAName = "Winner QF3",
      teamAShortName = "TBD",
      teamAColorHex = 0xFF555555,
      teamAStarPlayer = "TBD",
      teamBId = "tbd",
      teamBName = "Winner QF4",
      teamBShortName = "TBD",
      teamBColorHex = 0xFF555555,
      teamBStarPlayer = "TBD",
      nextMatchStageOrder = 7,
      nextMatchSlot = "B"
    )

    // Grand Final (Stage 7)
    val finalMatch = TournamentMatchEntity(
      tournamentId = tournamentId,
      roundName = "World Cup Final",
      roundCategory = "FINAL",
      stageOrder = 7,
      teamAId = "tbd",
      teamAName = "Winner SF1",
      teamAShortName = "TBD",
      teamAColorHex = 0xFF555555,
      teamAStarPlayer = "TBD",
      teamBId = "tbd",
      teamBName = "Winner SF2",
      teamBShortName = "TBD",
      teamBColorHex = 0xFF555555,
      teamBStarPlayer = "TBD"
    )

    tournamentDao.insertMatches(listOf(qf1, qf2, qf3, qf4, sf1, sf2, finalMatch))

    // 5. Pre-simulate non-player QFs (QF2, QF3, QF4)
    simulateAiMatch(tournamentId, 2)
    simulateAiMatch(tournamentId, 3)
    simulateAiMatch(tournamentId, 4)

    return tournamentId
  }

  suspend fun recordPlayerMatchResult(
    tournamentId: Long,
    stageOrder: Int,
    scoreA: Int,
    scoreB: Int,
    shotsA: Int = 0,
    shotsB: Int = 0,
    tacklesA: Int = 0,
    tacklesB: Int = 0
  ) {
    val match = tournamentDao.getMatchByStageOrder(tournamentId, stageOrder) ?: return
    val winnerIsA = scoreA > scoreB
    val winnerTeamId = if (winnerIsA) match.teamAId else match.teamBId
    val winnerTeamName = if (winnerIsA) match.teamAName else match.teamBName

    val updatedMatch = match.copy(
      scoreA = scoreA,
      scoreB = scoreB,
      isCompleted = true,
      winnerTeamId = winnerTeamId,
      winnerTeamName = winnerTeamName
    )
    tournamentDao.updateMatch(updatedMatch)

    // Update Player Stats in Room
    updatePlayerStatsForMatch(tournamentId, match.teamAStarPlayer, match.teamAId, match.teamAName, scoreA, shotsA, tacklesA)
    updatePlayerStatsForMatch(tournamentId, match.teamBStarPlayer, match.teamBId, match.teamBName, scoreB, shotsB, tacklesB)

    // Record in global match history table
    matchHistoryDao.insertMatch(
      MatchHistoryEntity(
        mode = "Cup: ${match.roundName}",
        homeTeamName = match.teamAName,
        awayTeamName = match.teamBName,
        homeScore = scoreA,
        awayScore = scoreB,
        homeShots = maxOf(shotsA, scoreA),
        awayShots = maxOf(shotsB, scoreB),
        homePossessionPct = 52,
        winner = if (winnerIsA) "HOME" else "AWAY"
      )
    )

    // Feed winner to next stage
    if (match.nextMatchStageOrder != null && match.nextMatchSlot != null) {
      advanceWinnerToNextStage(
        tournamentId = tournamentId,
        targetStageOrder = match.nextMatchStageOrder,
        slot = match.nextMatchSlot,
        winnerTeamId = winnerTeamId,
        winnerTeamName = winnerTeamName,
        winnerTeamShort = if (winnerIsA) match.teamAShortName else match.teamBShortName,
        winnerColorHex = if (winnerIsA) match.teamAColorHex else match.teamBColorHex,
        winnerStarPlayer = if (winnerIsA) match.teamAStarPlayer else match.teamBStarPlayer
      )
    }

    // Check if tournament concluded (stageOrder 7 is final)
    if (stageOrder == 7) {
      tournamentDao.getTournamentById(tournamentId).collect { t ->
        if (t != null && !t.isCompleted) {
          tournamentDao.updateTournament(
            t.copy(
              isCompleted = true,
              currentRound = "COMPLETED",
              championTeamName = winnerTeamName,
              championTeamId = winnerTeamId
            )
          )
        }
      }
    }
  }

  private suspend fun advanceWinnerToNextStage(
    tournamentId: Long,
    targetStageOrder: Int,
    slot: String,
    winnerTeamId: String,
    winnerTeamName: String,
    winnerTeamShort: String,
    winnerColorHex: Long,
    winnerStarPlayer: String
  ) {
    val nextMatch = tournamentDao.getMatchByStageOrder(tournamentId, targetStageOrder) ?: return
    val tournament = tournamentDao.getMatchByStageOrder(tournamentId, 1) // check player team
    val playerTeamId = tournament?.teamAId ?: ""

    val updatedNextMatch = if (slot == "A") {
      nextMatch.copy(
        teamAId = winnerTeamId,
        teamAName = winnerTeamName,
        teamAShortName = winnerTeamShort,
        teamAColorHex = winnerColorHex,
        teamAStarPlayer = winnerStarPlayer,
        isPlayerMatch = (winnerTeamId == playerTeamId || nextMatch.teamBId == playerTeamId)
      )
    } else {
      nextMatch.copy(
        teamBId = winnerTeamId,
        teamBName = winnerTeamName,
        teamBShortName = winnerTeamShort,
        teamBColorHex = winnerColorHex,
        teamBStarPlayer = winnerStarPlayer,
        isPlayerMatch = (nextMatch.teamAId == playerTeamId || winnerTeamId == playerTeamId)
      )
    }
    tournamentDao.updateMatch(updatedNextMatch)

    // If both slots are now filled and it's an AI vs AI match, simulate it!
    if (updatedNextMatch.teamAId != "tbd" && updatedNextMatch.teamBId != "tbd") {
      if (!updatedNextMatch.isPlayerMatch && !updatedNextMatch.isCompleted) {
        simulateAiMatch(tournamentId, targetStageOrder)
      }
    }
  }

  private suspend fun simulateAiMatch(tournamentId: Long, stageOrder: Int) {
    val match = tournamentDao.getMatchByStageOrder(tournamentId, stageOrder) ?: return
    if (match.teamAId == "tbd" || match.teamBId == "tbd" || match.isCompleted) return

    val sA = Random.nextInt(0, 4)
    var sB = Random.nextInt(0, 4)
    if (sA == sB) sB += 1 // ensure decisive outcome

    val winnerIsA = sA > sB
    val winnerTeamId = if (winnerIsA) match.teamAId else match.teamBId
    val winnerTeamName = if (winnerIsA) match.teamAName else match.teamBName

    val updated = match.copy(
      scoreA = sA,
      scoreB = sB,
      isCompleted = true,
      winnerTeamId = winnerTeamId,
      winnerTeamName = winnerTeamName
    )
    tournamentDao.updateMatch(updated)

    // Update stats
    updatePlayerStatsForMatch(tournamentId, match.teamAStarPlayer, match.teamAId, match.teamAName, sA, sA + 2, 2)
    updatePlayerStatsForMatch(tournamentId, match.teamBStarPlayer, match.teamBId, match.teamBName, sB, sB + 2, 2)

    // Advance to next stage
    if (match.nextMatchStageOrder != null && match.nextMatchSlot != null) {
      advanceWinnerToNextStage(
        tournamentId = tournamentId,
        targetStageOrder = match.nextMatchStageOrder,
        slot = match.nextMatchSlot,
        winnerTeamId = winnerTeamId,
        winnerTeamName = winnerTeamName,
        winnerTeamShort = if (winnerIsA) match.teamAShortName else match.teamBShortName,
        winnerColorHex = if (winnerIsA) match.teamAColorHex else match.teamBColorHex,
        winnerStarPlayer = if (winnerIsA) match.teamAStarPlayer else match.teamBStarPlayer
      )
    }
  }

  private suspend fun updatePlayerStatsForMatch(
    tournamentId: Long,
    playerName: String,
    teamId: String,
    teamName: String,
    goals: Int,
    shots: Int,
    tackles: Int
  ) {
    val existing = tournamentDao.getPlayerStats(tournamentId, playerName)
    if (existing != null) {
      tournamentDao.insertOrUpdatePlayerStats(
        existing.copy(
          matchesPlayed = existing.matchesPlayed + 1,
          goalsScored = existing.goalsScored + goals,
          shotsOnTarget = existing.shotsOnTarget + shots,
          tacklesWon = existing.tacklesWon + tackles
        )
      )
    } else {
      tournamentDao.insertOrUpdatePlayerStats(
        TournamentPlayerStatsEntity(
          tournamentId = tournamentId,
          teamId = teamId,
          teamName = teamName,
          playerName = playerName,
          matchesPlayed = 1,
          goalsScored = goals,
          shotsOnTarget = shots,
          tacklesWon = tackles
        )
      )
    }
  }
}
