package com.example.game

import com.example.model.AiDifficulty
import com.example.model.TeamKit
import kotlin.math.abs

enum class ShootoutTurnState {
  AIMING, BALL_IN_FLIGHT, RESULT_DISPLAY, SHOOTOUT_OVER
}

enum class KickResult {
  GOAL, SAVED, POST_MISS, WIDE_MISS
}

data class PenaltyAttempt(
  val roundNumber: Int,
  val teamId: Int,
  val result: KickResult?
)

class PenaltyShootoutEngine(
  val homeTeam: TeamKit,
  val awayTeam: TeamKit,
  val aiDifficulty: AiDifficulty = AiDifficulty.PRO,
  val onPlaySound: (sound: String) -> Unit = {}
) {
  var turnState: ShootoutTurnState = ShootoutTurnState.AIMING
  var currentRound: Int = 1
  var currentKickingTeam: Int = 1 // 1 = Home, 2 = Away

  val homeAttempts = mutableListOf<PenaltyAttempt>()
  val awayAttempts = mutableListOf<PenaltyAttempt>()

  var homeGoals: Int = 0
  var awayGoals: Int = 0

  // Shot simulation parameters
  var aimTargetX: Float = 500f // 350 to 650 is goal width
  var aimTargetHeight: Float = 0.5f // 0 = low grass, 1 = top corner
  var shotPower: Float = 0.8f // 0 to 1
  var shotCurve: Float = 0f // -1 to 1

  var ballPos = Vec2(500f, 1200f) // penalty spot
  var ballVel = Vec2(0f, 0f)
  var ballHeight = 0f
  var ballVelZ = 0f

  var keeperPos = Vec2(500f, 750f)
  var keeperDiveTargetX = 500f
  var keeperDiveTimer = 0f

  var lastResult: KickResult? = null
  var winnerTeamId: Int? = null

  var animationTimer: Float = 0f
  var aiKickDelayTimer: Float = 0f

  init {
    resetForNextKick()
  }

  fun resetForNextKick() {
    ballPos = Vec2(500f, 1200f)
    ballVel = Vec2(0f, 0f)
    ballHeight = 0f
    ballVelZ = 0f
    keeperPos = Vec2(500f, 750f)
    keeperDiveTimer = 0f
    lastResult = null
    turnState = ShootoutTurnState.AIMING

    if (currentKickingTeam == 2) {
      aiKickDelayTimer = 0.85f
    }
  }

  fun executeShot(targetX: Float, targetHeight: Float, power: Float, curve: Float) {
    if (turnState != ShootoutTurnState.AIMING) return

    aimTargetX = targetX
    aimTargetHeight = targetHeight
    shotPower = power
    shotCurve = curve

    turnState = ShootoutTurnState.BALL_IN_FLIGHT
    animationTimer = 0f

    // Calculate ball velocity toward goal line (y = 750f)
    val dx = targetX - ballPos.x
    val dy = 750f - ballPos.y // -450f
    val speed = 650f + power * 450f

    val dist = Vec2(dx, dy).length()
    ballVel = Vec2(dx / dist * speed, dy / dist * speed)
    ballVelZ = (targetHeight * 400f + 100f)

    // Keeper AI anticipation
    val keeperGuessError = when (aiDifficulty) {
      AiDifficulty.ROOKIE -> (Math.random().toFloat() - 0.5f) * 160f
      AiDifficulty.PRO -> (Math.random().toFloat() - 0.5f) * 90f
      AiDifficulty.LEGEND -> (Math.random().toFloat() - 0.5f) * 40f
    }
    keeperDiveTargetX = (targetX + keeperGuessError).coerceIn(360f, 640f)

    if (power > 0.85f) {
      onPlaySound("powershot")
    } else {
      onPlaySound("kick")
    }
  }

  fun update(dt: Float) {
    if (turnState == ShootoutTurnState.AIMING && currentKickingTeam == 2) {
      aiKickDelayTimer -= dt
      if (aiKickDelayTimer <= 0f) {
        val targetX = 370f + Math.random().toFloat() * 260f
        val targetH = 0.2f + Math.random().toFloat() * 0.7f
        val power = 0.75f + Math.random().toFloat() * 0.2f
        val curve = (Math.random().toFloat() - 0.5f) * 0.6f
        executeShot(targetX, targetH, power, curve)
      }
    } else if (turnState == ShootoutTurnState.BALL_IN_FLIGHT) {
      animationTimer += dt

      // Move ball
      ballPos.x += ballVel.x * dt + shotCurve * 150f * dt
      ballPos.y += ballVel.y * dt
      ballHeight = (ballHeight + ballVelZ * dt).coerceAtLeast(0f)

      // Move keeper dive
      val kdx = keeperDiveTargetX - keeperPos.x
      keeperPos.x += (kdx * 5.5f * dt).coerceIn(-400f * dt, 400f * dt)

      // Check if ball reached goal line (y <= 760f)
      if (ballPos.y <= 760f) {
        evaluateKickResult()
      }
    } else if (turnState == ShootoutTurnState.RESULT_DISPLAY) {
      animationTimer += dt
      if (animationTimer >= 2.2f) {
        advanceTurn()
      }
    }
  }

  private fun evaluateKickResult() {
    turnState = ShootoutTurnState.RESULT_DISPLAY
    animationTimer = 0f

    val inGoalX = ballPos.x in 360f..640f
    val hitPost = abs(ballPos.x - 355f) < 18f || abs(ballPos.x - 645f) < 18f
    val distToKeeper = abs(ballPos.x - keeperPos.x)

    if (hitPost) {
      lastResult = KickResult.POST_MISS
      onPlaySound("post")
    } else if (!inGoalX) {
      lastResult = KickResult.WIDE_MISS
      onPlaySound("whistle")
    } else if (distToKeeper < 52f && ballHeight < 320f) {
      lastResult = KickResult.SAVED
      onPlaySound("kick")
    } else {
      lastResult = KickResult.GOAL
      if (currentKickingTeam == 1) homeGoals++ else awayGoals++
      onPlaySound("goal")
    }

    if (currentKickingTeam == 1) {
      homeAttempts.add(PenaltyAttempt(currentRound, 1, lastResult))
    } else {
      awayAttempts.add(PenaltyAttempt(currentRound, 2, lastResult))
    }

    checkShootoutConclusion()
  }

  private fun checkShootoutConclusion() {
    val hCount = homeAttempts.size
    val aCount = awayAttempts.size

    // Standard 5 rounds rule
    if (hCount <= 5 && aCount <= 5) {
      val remainingHome = 5 - hCount
      val remainingAway = 5 - aCount

      if (homeGoals > awayGoals + remainingAway) {
        winnerTeamId = 1
        turnState = ShootoutTurnState.SHOOTOUT_OVER
        return
      }
      if (awayGoals > homeGoals + remainingHome) {
        winnerTeamId = 2
        turnState = ShootoutTurnState.SHOOTOUT_OVER
        return
      }

      if (hCount == 5 && aCount == 5) {
        if (homeGoals > awayGoals) {
          winnerTeamId = 1
          turnState = ShootoutTurnState.SHOOTOUT_OVER
        } else if (awayGoals > homeGoals) {
          winnerTeamId = 2
          turnState = ShootoutTurnState.SHOOTOUT_OVER
        }
        // else sudden death continues!
      }
    } else {
      // Sudden death (after round 5, both must take 1 kick)
      if (hCount == aCount) {
        if (homeGoals > awayGoals) {
          winnerTeamId = 1
          turnState = ShootoutTurnState.SHOOTOUT_OVER
        } else if (awayGoals > homeGoals) {
          winnerTeamId = 2
          turnState = ShootoutTurnState.SHOOTOUT_OVER
        }
      }
    }
  }

  private fun advanceTurn() {
    if (turnState == ShootoutTurnState.SHOOTOUT_OVER) return

    if (currentKickingTeam == 1) {
      currentKickingTeam = 2
    } else {
      currentKickingTeam = 1
      currentRound++
    }

    resetForNextKick()
  }
}
