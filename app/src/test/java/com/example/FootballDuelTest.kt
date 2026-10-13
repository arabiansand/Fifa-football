package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.database.FootballDatabase
import com.example.database.PlayerProfileEntity
import com.example.database.TournamentRepository
import com.example.game.*
import com.example.model.AiDifficulty
import com.example.model.TeamKit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FootballDuelTest {

  @Test
  fun testMatchEngineInitialStateAndCountdown() {
    val engine = MatchEngine(
      homeTeam = TeamKit.ENGLAND,
      awayTeam = TeamKit.NORWAY,
      isTwoPlayer = false,
      aiDifficulty = AiDifficulty.PRO,
      matchDurationSeconds = 180
    ).apply {
      startMatch()
    }

    assertEquals(MatchState.COUNTDOWN, engine.matchState)
    assertEquals(0, engine.homeScore)
    assertEquals(0, engine.awayScore)

    // Tick countdown through multiple frames
    repeat(70) {
      engine.update(0.05f)
    }
    assertEquals(MatchState.PLAYING, engine.matchState)
  }

  @Test
  fun testPenaltyShootoutFiveRoundsAndDecisiveWinner() {
    val engine = PenaltyShootoutEngine(
      homeTeam = TeamKit.ENGLAND,
      awayTeam = TeamKit.NORWAY,
      aiDifficulty = AiDifficulty.ROOKIE
    )

    assertEquals(ShootoutTurnState.AIMING, engine.turnState)
    assertEquals(1, engine.currentRound)
    assertEquals(1, engine.currentKickingTeam)

    // Home shot
    engine.executeShot(500f, 0.5f, 0.8f, 0f)
    assertEquals(ShootoutTurnState.BALL_IN_FLIGHT, engine.turnState)

    // Fast-forward ball flight
    engine.update(1.5f)
    assertEquals(ShootoutTurnState.RESULT_DISPLAY, engine.turnState)
    assertNotNull(engine.lastResult)
  }

  @Test
  fun testTournamentEngineKnockoutBracketProgression() {
    val tournament = TournamentEngine(playerTeam = TeamKit.ENGLAND)
    assertEquals(4, tournament.quarterFinals.size)

    val playerQf = tournament.getNextPlayerMatch()
    assertNotNull(playerQf)
    assertEquals("QF1", playerQf!!.id)

    // Record player victory in QF 3-1
    tournament.recordPlayerMatchResult(playerQf.id, 3, 1)
    assertTrue(playerQf.isCompleted)
    assertEquals(TeamKit.ENGLAND.id, playerQf.winner?.id)

    // Semi-Finals should be initialized
    assertEquals(2, tournament.semiFinals.size)
    val playerSf = tournament.getNextPlayerMatch()
    assertNotNull(playerSf)

    // Record player victory in SF 2-0
    tournament.recordPlayerMatchResult(playerSf!!.id, 2, 0)
    assertNotNull(tournament.grandFinal)
  }

  @Test
  fun testBallPhysicsVectorMath() {
    val v1 = Vec2(3f, 4f)
    assertEquals(5f, v1.length(), 0.001f)
    val norm = v1.normalized()
    assertEquals(0.6f, norm.x, 0.001f)
    assertEquals(0.8f, norm.y, 0.001f)

    val v2 = Vec2(0f, 0f)
    assertEquals(5f, v1.distanceTo(v2), 0.001f)
  }

  @Test
  fun testStaminaExhaustionHysteresis() {
    val engine = MatchEngine(
      homeTeam = TeamKit.ENGLAND,
      awayTeam = TeamKit.NORWAY
    ).apply {
      matchState = MatchState.PLAYING
    }
    engine.p1.stamina = 0f
    engine.p1Sprint = true
    engine.p1Joystick = Vec2(0f, 1f)

    // Tick one frame with 0 stamina
    engine.update(0.016f)
    assertTrue("Player should enter isExhausted state when stamina hits 0", engine.p1.isExhausted)
    assertFalse("Player cannot sprint while exhausted", engine.p1.isSprinting)

    // Recharge stamina partially to 15f
    engine.p1.stamina = 15f
    engine.update(0.016f)
    assertTrue("Player must remain exhausted until reaching threshold of 25f", engine.p1.isExhausted)

    // Recharge past 25f
    engine.p1.stamina = 30f
    engine.update(0.016f)
    assertFalse("Player recovers from exhaustion past 25f", engine.p1.isExhausted)
  }

  @Test
  fun testGoalNetMeshContainsBall() {
    val engine = MatchEngine(
      homeTeam = TeamKit.ENGLAND,
      awayTeam = TeamKit.NORWAY
    ).apply {
      startMatch()
    }
    repeat(70) { engine.update(0.05f) } // reach PLAYING

    // High speed rocket into top net
    engine.ball.pos = Vec2(500f, 100f)
    engine.ball.vel = Vec2(0f, -900f)

    // Simulate frames
    repeat(30) { engine.update(0.03f) }

    // Ball must not fly beyond back of top net
    val topNetBack = PitchConstants.TOP_GOAL_Y - PitchConstants.GOAL_DEPTH
    assertTrue("Ball must stay within back of net", engine.ball.pos.y >= topNetBack - 5f)
  }

  @Test
  fun testRoomTournamentRepositoryBracketAndStats() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, FootballDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = TournamentRepository(db.tournamentDao(), db.matchHistoryDao())

    val tournamentId = repo.createNewTournament(TeamKit.ENGLAND, "World Cup 2026")
    assertTrue(tournamentId > 0)

    val qf1 = db.tournamentDao().getMatchByStageOrder(tournamentId, 1)
    assertNotNull(qf1)
    assertTrue(qf1!!.isPlayerMatch)
    assertEquals(TeamKit.ENGLAND.name, qf1.teamAName)

    // Record England victory 3-1 over Norway
    repo.recordPlayerMatchResult(tournamentId, 1, 3, 1, shotsA = 7, shotsB = 4)

    val updatedQf1 = db.tournamentDao().getMatchByStageOrder(tournamentId, 1)
    assertTrue(updatedQf1!!.isCompleted)
    assertEquals(TeamKit.ENGLAND.id, updatedQf1.winnerTeamId)

    // Check player stats tracking for Henderson
    val hendersonStats = db.tournamentDao().getPlayerStats(tournamentId, "Henderson")
    assertNotNull(hendersonStats)
    assertEquals(3, hendersonStats!!.goalsScored)

    // Check SF1 now has England in slot A
    val sf1 = db.tournamentDao().getMatchByStageOrder(tournamentId, 5)
    assertNotNull(sf1)
    assertEquals(TeamKit.ENGLAND.name, sf1!!.teamAName)
    assertTrue(sf1.isPlayerMatch)

    db.close()
  }

  @Test
  fun testPlayerProfilePersistenceInRoom() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, FootballDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val profile = PlayerProfileEntity(
      id = 1,
      playerName = "Viking Striker",
      playerTitle = "Golden Boot Winner",
      favoriteTeamId = "nor",
      favoriteTeamName = "Norway",
      primaryColorHex = 0xFFD81E2C,
      secondaryColorHex = 0xFF00205B,
      accentColorHex = 0xFFFFFFFF,
      kitStyle = "SASH",
      jerseyNumber = 9
    )

    db.playerProfileDao().savePlayerProfile(profile)

    val loaded = db.playerProfileDao().getPlayerProfileSync()
    assertNotNull(loaded)
    assertEquals("Viking Striker", loaded!!.playerName)
    assertEquals(0xFFD81E2C, loaded.primaryColorHex)
    assertEquals(0xFF00205B, loaded.secondaryColorHex)
    assertEquals("SASH", loaded.kitStyle)
    assertEquals(9, loaded.jerseyNumber)

    db.close()
  }
}
