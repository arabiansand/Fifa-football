package com.example.game

import com.example.model.AiDifficulty
import com.example.model.MatchStats
import com.example.model.TeamKit
import kotlin.math.*

class MatchEngine(
  val homeTeam: TeamKit,
  val awayTeam: TeamKit,
  val isTwoPlayer: Boolean = false,
  val aiDifficulty: AiDifficulty = AiDifficulty.PRO,
  val matchDurationSeconds: Int = 180, // Default 3 mins (or configurable)
  val onGoalScored: (teamId: Int) -> Unit = {},
  val onPlaySound: (sound: String) -> Unit = {}
) {
  var matchState: MatchState = MatchState.COUNTDOWN
  var countdownTimer: Float = 3.0f
  var matchTimeSeconds: Float = matchDurationSeconds.toFloat()
  var homeScore: Int = 0
  var awayScore: Int = 0
  var goalCelebrationTimer: Float = 0f
  var scoringTeamId: Int = 0

  val p1 = Player(id = 1, teamId = 1, pos = Vec2(500f, 1050f), facing = -PI.toFloat() / 2f)
  val p2 = Player(id = 2, teamId = 2, pos = Vec2(500f, 550f), facing = PI.toFloat() / 2f)

  val homeGK = Goalkeeper(teamId = 1, pos = Vec2(500f, 1500f), defaultY = 1500f)
  val awayGK = Goalkeeper(teamId = 2, pos = Vec2(500f, 100f), defaultY = 100f)

  val ball = Ball(pos = Vec2(500f, 800f))

  val particles = mutableListOf<Particle>()
  var stats = MatchStats()

  // Input states
  var p1Joystick = Vec2(0f, 0f)
  var p1Sprint = false
  var p1ShootCharge = 0f
  var p1CurveActive = false

  var p2Joystick = Vec2(0f, 0f)
  var p2Sprint = false
  var p2ShootCharge = 0f
  var p2CurveActive = false

  // AI timing & state
  private var aiActionTimer = 0f
  private var aiTargetPos = Vec2(500f, 550f)

  fun startMatch() {
    matchState = MatchState.COUNTDOWN
    countdownTimer = 3.0f
    resetPositions(kickoffTeam = 1)
  }

  fun resetPositions(kickoffTeam: Int) {
    p1.pos = Vec2(500f, 1050f)
    p1.vel = Vec2(0f, 0f)
    p1.facing = -PI.toFloat() / 2f
    p1.isTackling = false
    p1.tackleTimer = 0f
    p1.stunnedTimer = 0f

    p2.pos = Vec2(500f, 550f)
    p2.vel = Vec2(0f, 0f)
    p2.facing = PI.toFloat() / 2f
    p2.isTackling = false
    p2.tackleTimer = 0f
    p2.stunnedTimer = 0f

    homeGK.pos = Vec2(500f, 1500f)
    homeGK.vel = Vec2(0f, 0f)
    homeGK.diveTimer = 0f

    awayGK.pos = Vec2(500f, 100f)
    awayGK.vel = Vec2(0f, 0f)
    awayGK.diveTimer = 0f

    ball.pos = Vec2(500f, 800f)
    ball.vel = Vec2(0f, 0f)
    ball.heightZ = 0f
    ball.velZ = 0f
    ball.spin = 0f
    ball.trail.clear()

    // Place kickoff team near ball
    if (kickoffTeam == 1) {
      p1.pos = Vec2(500f, 850f)
    } else {
      p2.pos = Vec2(500f, 750f)
    }
  }

  fun update(dt: Float) {
    val clampedDt = dt.coerceIn(0.001f, 0.05f)

    when (matchState) {
      MatchState.COUNTDOWN -> {
        countdownTimer -= clampedDt
        if (countdownTimer <= 0f) {
          matchState = MatchState.PLAYING
          onPlaySound("whistle")
        }
      }
      MatchState.PLAYING -> {
        matchTimeSeconds -= clampedDt
        if (matchTimeSeconds <= 0f) {
          matchTimeSeconds = 0f
          matchState = MatchState.FULLTIME
          onPlaySound("whistle")
        } else {
          updateGameLogic(clampedDt)
        }
      }
      MatchState.GOAL_CELEBRATION -> {
        goalCelebrationTimer -= clampedDt
        updateParticles(clampedDt)
        if (goalCelebrationTimer <= 0f) {
          // Restart kickoff
          val nextKickoff = if (scoringTeamId == 1) 2 else 1
          resetPositions(nextKickoff)
          matchState = MatchState.PLAYING
          onPlaySound("whistle")
        }
      }
      else -> {}
    }
  }

  private fun updateGameLogic(dt: Float) {
    // 1. Process P1 Player movement & stamina
    updatePlayerInput(p1, p1Joystick, p1Sprint, dt)

    // 2. Process P2 Player movement (AI or 2-player)
    if (isTwoPlayer) {
      updatePlayerInput(p2, p2Joystick, p2Sprint, dt)
    } else {
      updateAi(dt)
    }

    // 3. Update Goalkeepers
    updateGoalkeepers(dt)

    // 4. Update Ball Physics
    updateBall(dt)

    // 5. Check Collisions & Tackles
    checkPlayerBallInteractions()
    checkPlayerCollisions()

    // 6. Update Particles
    updateParticles(dt)

    // 7. Track Possession
    val distP1 = ball.pos.distanceTo(p1.pos)
    val distP2 = ball.pos.distanceTo(p2.pos)
    if (distP1 < 80f && distP1 < distP2) {
      stats = stats.copy(homePossessionFrames = stats.homePossessionFrames + 1)
      ball.lastTouchTeam = 1
    } else if (distP2 < 80f) {
      stats = stats.copy(awayPossessionFrames = stats.awayPossessionFrames + 1)
      ball.lastTouchTeam = 2
    }
  }

  private fun updatePlayerInput(player: Player, stick: Vec2, sprint: Boolean, dt: Float) {
    if (player.stunnedTimer > 0f) {
      player.stunnedTimer -= dt
      player.vel = player.vel * 0.9f
      player.pos = player.pos + player.vel * dt
      return
    }

    if (player.tackleCooldown > 0f) {
      player.tackleCooldown -= dt
    }

    if (player.isTackling) {
      player.tackleTimer -= dt
      player.pos = player.pos + player.vel * dt
      spawnTurfParticles(player.pos, 2)
      if (player.tackleTimer <= 0f) {
        player.isTackling = false
      }
      clampPlayerToBounds(player)
      return
    }

    // Stamina regen or drain with exhaustion hysteresis
    if (player.stamina <= 0f) {
      player.isExhausted = true
    } else if (player.stamina >= 25f) {
      player.isExhausted = false
    }

    var speed = PitchConstants.BASE_PLAYER_SPEED
    if (sprint && !player.isExhausted && player.stamina > 5f && stick.length() > 0.1f) {
      speed *= PitchConstants.SPRINT_MULTIPLIER
      player.stamina = (player.stamina - 28f * dt).coerceAtLeast(0f)
      player.isSprinting = true
    } else {
      player.stamina = (player.stamina + 18f * dt).coerceAtMost(100f)
      player.isSprinting = false
    }

    if (stick.length() > 0.05f) {
      val norm = stick.normalized()
      val targetVel = norm * speed
      // Smooth acceleration
      player.vel = Vec2(
        player.vel.x + (targetVel.x - player.vel.x) * 12f * dt,
        player.vel.y + (targetVel.y - player.vel.y) * 12f * dt
      )
      player.facing = atan2(norm.y, norm.x)
    } else {
      player.vel = player.vel * (1f - 10f * dt).coerceAtLeast(0f)
    }

    player.pos = player.pos + player.vel * dt
    clampPlayerToBounds(player)
  }

  private fun clampPlayerToBounds(player: Player) {
    val pad = PitchConstants.PADDING + player.radius
    player.pos.x = player.pos.x.coerceIn(pad, PitchConstants.WIDTH - pad)
    player.pos.y = player.pos.y.coerceIn(pad, PitchConstants.HEIGHT - pad)
  }

  private fun updateAi(dt: Float) {
    aiActionTimer += dt

    val ballPos = ball.pos
    val p2Pos = p2.pos
    val distToBall = p2Pos.distanceTo(ballPos)

    val speedMult = aiDifficulty.speedFactor
    val baseSpeed = PitchConstants.BASE_PLAYER_SPEED * speedMult

    // AI tactical decision
    val isBallInAiHalf = ballPos.y < 900f
    val isP1CloseToAiGoal = p1.pos.y < 600f

    val target: Vec2
    var shouldSprint = false

    if (distToBall < 65f) {
      // In possession! Advance toward home goal (y = 1500f)
      val goalTarget = Vec2(500f + sin(aiActionTimer * 3f) * 100f, PitchConstants.BOTTOM_GOAL_Y)
      target = goalTarget
      shouldSprint = true

      // Decide when to shoot
      if (p2Pos.y > 900f || (p2Pos.y > 750f && p1.pos.distanceTo(p2Pos) < 100f)) {
        // Shoot!
        val aimAngle = atan2(PitchConstants.BOTTOM_GOAL_Y - p2Pos.y, 500f - p2Pos.x)
        val power = if (aiDifficulty == AiDifficulty.LEGEND) 1100f else 850f
        val curve = if (aiDifficulty == AiDifficulty.LEGEND) (if (Math.random() > 0.5) 0.6f else -0.6f) else 0f
        triggerShot(p2, power, aimAngle, curve)
        stats = stats.copy(awayShots = stats.awayShots + 1)
      }
    } else if (distToBall < 450f || isBallInAiHalf) {
      // Hunt ball!
      target = ballPos
      shouldSprint = distToBall > 120f && p2.stamina > 30f

      // Check tackle opportunity
      if (distToBall < 90f && p1.pos.distanceTo(ballPos) < 60f && p2.tackleCooldown <= 0f) {
        if (Math.random() < (aiDifficulty.shotAccuracy * 0.8)) {
          triggerTackle(p2)
        }
      }
    } else {
      // Retain defensive position between ball and top goal
      target = Vec2(500f + (ballPos.x - 500f) * 0.4f, 500f)
    }

    val dir = (target - p2Pos).normalized()
    val stick = Vec2(dir.x, dir.y)
    updatePlayerInput(p2, stick, shouldSprint, dt)
  }

  private fun updateGoalkeepers(dt: Float) {
    // Away GK (protects y = 100f)
    val awayTargetX = ball.pos.x.coerceIn(PitchConstants.GOAL_LEFT + 25f, PitchConstants.GOAL_RIGHT - 25f)
    val awayDx = awayTargetX - awayGK.pos.x
    awayGK.pos.x += (awayDx * 6f * dt).coerceIn(-280f * dt, 280f * dt)

    // Check dive / parry
    if (ball.pos.y < 300f && ball.vel.y < -100f) {
      if (abs(ball.pos.x - awayGK.pos.x) < 70f && ball.pos.y < 150f) {
        // Save!
        ball.vel.y = abs(ball.vel.y) * 0.8f + 150f
        ball.vel.x += (Math.random().toFloat() - 0.5f) * 200f
        stats = stats.copy(awaySaves = stats.awaySaves + 1)
        onPlaySound("kick")
        spawnTurfParticles(awayGK.pos, 5)
      }
    }

    // Home GK (protects y = 1500f)
    val homeTargetX = ball.pos.x.coerceIn(PitchConstants.GOAL_LEFT + 25f, PitchConstants.GOAL_RIGHT - 25f)
    val homeDx = homeTargetX - homeGK.pos.x
    homeGK.pos.x += (homeDx * 6f * dt).coerceIn(-280f * dt, 280f * dt)

    // Check dive / parry
    if (ball.pos.y > 1300f && ball.vel.y > 100f) {
      if (abs(ball.pos.x - homeGK.pos.x) < 70f && ball.pos.y > 1450f) {
        // Save!
        ball.vel.y = -abs(ball.vel.y) * 0.8f - 150f
        ball.vel.x += (Math.random().toFloat() - 0.5f) * 200f
        stats = stats.copy(homeSaves = stats.homeSaves + 1)
        onPlaySound("kick")
        spawnTurfParticles(homeGK.pos, 5)
      }
    }
  }

  private fun updateBall(dt: Float) {
    // Curve spin force
    if (abs(ball.spin) > 0.05f) {
      val curveForce = ball.spin * 280f * dt
      ball.vel.x += curveForce * (ball.vel.length() / 500f)
      ball.spin *= 0.985f
    }

    // Velocity update
    ball.pos = ball.pos + ball.vel * dt
    ball.vel = ball.vel * PitchConstants.BALL_FRICTION

    // Height & bounce
    if (ball.heightZ > 0f || ball.velZ != 0f) {
      ball.heightZ += ball.velZ * dt
      ball.velZ += PitchConstants.GRAVITY * dt
      if (ball.heightZ <= 0f) {
        ball.heightZ = 0f
        ball.velZ = -ball.velZ * 0.6f
        if (abs(ball.velZ) < 40f) ball.velZ = 0f
      }
    }

    // Trailing particles for high speed shots
    if (ball.vel.length() > 400f) {
      ball.trail.add(0, Vec2(ball.pos.x, ball.pos.y))
      if (ball.trail.size > 8) ball.trail.removeAt(ball.trail.size - 1)
    } else {
      if (ball.trail.isNotEmpty()) ball.trail.removeAt(ball.trail.size - 1)
    }

    // Pitch side bounds bounce
    val minX = PitchConstants.PADDING + ball.radius
    val maxX = PitchConstants.WIDTH - PitchConstants.PADDING - ball.radius

    if (ball.pos.x < minX) {
      ball.pos.x = minX
      ball.vel.x = -ball.vel.x * 0.75f
      onPlaySound("kick")
    } else if (ball.pos.x > maxX) {
      ball.pos.x = maxX
      ball.vel.x = -ball.vel.x * 0.75f
      onPlaySound("kick")
    }

    // Goal checks and end-line bounces
    checkGoalOrBounce()
  }

  private fun checkGoalOrBounce() {
    val inGoalX = ball.pos.x in PitchConstants.GOAL_LEFT..PitchConstants.GOAL_RIGHT

    // Top net (Home scores)
    if (ball.pos.y < PitchConstants.TOP_GOAL_Y) {
      if (inGoalX) {
        val topNetBack = PitchConstants.TOP_GOAL_Y - PitchConstants.GOAL_DEPTH
        if (ball.pos.y < topNetBack) {
          ball.pos.y = topNetBack
          ball.vel.y = -ball.vel.y * 0.25f
          ball.vel.x *= 0.5f
        }
        if (ball.pos.y < PitchConstants.TOP_GOAL_Y - 40f && matchState == MatchState.PLAYING) {
          triggerGoal(teamId = 1)
        }
      } else {
        ball.pos.y = PitchConstants.TOP_GOAL_Y
        ball.vel.y = -ball.vel.y * 0.7f
        onPlaySound("kick")
      }
    }

    // Goal posts top
    checkGoalPostBounce(PitchConstants.GOAL_LEFT, PitchConstants.TOP_GOAL_Y)
    checkGoalPostBounce(PitchConstants.GOAL_RIGHT, PitchConstants.TOP_GOAL_Y)

    // Bottom net (Away scores)
    if (ball.pos.y > PitchConstants.BOTTOM_GOAL_Y) {
      if (inGoalX) {
        val bottomNetBack = PitchConstants.BOTTOM_GOAL_Y + PitchConstants.GOAL_DEPTH
        if (ball.pos.y > bottomNetBack) {
          ball.pos.y = bottomNetBack
          ball.vel.y = -ball.vel.y * 0.25f
          ball.vel.x *= 0.5f
        }
        if (ball.pos.y > PitchConstants.BOTTOM_GOAL_Y + 40f && matchState == MatchState.PLAYING) {
          triggerGoal(teamId = 2)
        }
      } else {
        ball.pos.y = PitchConstants.BOTTOM_GOAL_Y
        ball.vel.y = -ball.vel.y * 0.7f
        onPlaySound("kick")
      }
    }

    // Goal posts bottom
    checkGoalPostBounce(PitchConstants.GOAL_LEFT, PitchConstants.BOTTOM_GOAL_Y)
    checkGoalPostBounce(PitchConstants.GOAL_RIGHT, PitchConstants.BOTTOM_GOAL_Y)
  }

  private fun checkGoalPostBounce(px: Float, py: Float) {
    val postPos = Vec2(px, py)
    val dist = ball.pos.distanceTo(postPos)
    if (dist < ball.radius + 14f) {
      val normal = (ball.pos - postPos).normalized()
      ball.pos = postPos + normal * (ball.radius + 15f)
      ball.vel = normal * (ball.vel.length() * 0.85f).coerceAtLeast(300f)
      onPlaySound("post")
      spawnTurfParticles(postPos, 6)
    }
  }

  private fun checkPlayerBallInteractions() {
    val interactP1 = checkSinglePlayerBall(p1)
    val interactP2 = checkSinglePlayerBall(p2)
    p1.hasBall = interactP1
    p2.hasBall = interactP2
  }

  private fun checkSinglePlayerBall(player: Player): Boolean {
    val dist = player.pos.distanceTo(ball.pos)
    val touchRadius = player.radius + ball.radius + 6f

    if (dist < touchRadius) {
      // Normal collision push
      val normal = (ball.pos - player.pos).normalized()
      val playerSpeed = player.vel.length()

      if (player.isTackling) {
        // Dispossess and blast ball forward!
        val blastAngle = player.facing
        ball.vel = Vec2(cos(blastAngle), sin(blastAngle)) * 800f
        ball.spin = 0f
        onPlaySound("tackle")
        spawnTurfParticles(ball.pos, 8)
        return false
      }

      if (playerSpeed > 150f) {
        // Dribble touch: push ball slightly in player's forward vector
        val pushDir = Vec2(cos(player.facing), sin(player.facing))
        ball.pos = player.pos + pushDir * (player.radius + ball.radius + 8f)
        ball.vel = pushDir * (playerSpeed * 1.15f + 120f)
        return true
      } else {
        // Soft touch
        ball.pos = player.pos + normal * touchRadius
        ball.vel = normal * 100f
        return true
      }
    }
    return false
  }

  private fun checkPlayerCollisions() {
    val dist = p1.pos.distanceTo(p2.pos)
    val minDist = p1.radius + p2.radius

    if (dist < minDist) {
      val normal = (p2.pos - p1.pos).normalized()
      val overlap = minDist - dist

      // Tackle hit check
      if (p1.isTackling && !p2.isTackling) {
        p2.stunnedTimer = 0.8f
        p2.vel = normal * 500f
        stats = stats.copy(homeTackles = stats.homeTackles + 1)
        onPlaySound("tackle")
        spawnTurfParticles(p2.pos, 10)
      } else if (p2.isTackling && !p1.isTackling) {
        p1.stunnedTimer = 0.8f
        p1.vel = normal * -500f
        stats = stats.copy(awayTackles = stats.awayTackles + 1)
        onPlaySound("tackle")
        spawnTurfParticles(p1.pos, 10)
      } else {
        // Elastic separation
        p1.pos = p1.pos - normal * (overlap * 0.5f)
        p2.pos = p2.pos + normal * (overlap * 0.5f)
      }
    }
  }

  fun triggerPlayerShot(playerId: Int, charge: Float, curve: Boolean) {
    val player = if (playerId == 1) p1 else p2
    val dist = player.pos.distanceTo(ball.pos)
    if (dist > player.radius + ball.radius + 35f) return // not in range

    val clampedCharge = charge.coerceIn(0.2f, 1f)
    val power = 450f + clampedCharge * 750f // up to 1200f speed!
    val goalY = if (player.teamId == 1) PitchConstants.TOP_GOAL_Y else PitchConstants.BOTTOM_GOAL_Y
    val targetAim = Vec2(500f, goalY)
    val aimDir = (targetAim - player.pos).normalized()
    val aimAngle = atan2(aimDir.y, aimDir.x)

    val spinAmount = if (curve) (if (player.pos.x < 500f) 0.8f else -0.8f) else 0f

    triggerShot(player, power, aimAngle, spinAmount)

    if (playerId == 1) {
      stats = stats.copy(
        homeShots = stats.homeShots + 1,
        homeShotsOnTarget = stats.homeShotsOnTarget + 1
      )
    } else {
      stats = stats.copy(
        awayShots = stats.awayShots + 1,
        awayShotsOnTarget = stats.awayShotsOnTarget + 1
      )
    }
  }

  private fun triggerShot(player: Player, power: Float, angle: Float, spin: Float) {
    ball.vel = Vec2(cos(angle) * power, sin(angle) * power)
    ball.spin = spin
    ball.heightZ = 10f
    ball.velZ = power * 0.35f
    ball.lastTouchTeam = player.teamId

    if (power > 900f) {
      onPlaySound("powershot")
      spawnSparkParticles(ball.pos, 12, 0xFFFFCC00)
    } else {
      onPlaySound("kick")
      spawnSparkParticles(ball.pos, 6, 0xFFFFFFFF)
    }
  }

  fun triggerTackle(player: Player) {
    if (player.tackleCooldown > 0f || player.isTackling || player.stunnedTimer > 0f) return

    player.isTackling = true
    player.tackleTimer = 0.32f
    player.tackleCooldown = 1.1f

    val tackleDir = Vec2(cos(player.facing), sin(player.facing))
    player.vel = tackleDir * PitchConstants.TACKLE_SPEED
    onPlaySound("tackle")
    spawnTurfParticles(player.pos, 8)
  }

  private fun triggerGoal(teamId: Int) {
    matchState = MatchState.GOAL_CELEBRATION
    goalCelebrationTimer = 3.2f
    scoringTeamId = teamId

    if (teamId == 1) {
      homeScore++
    } else {
      awayScore++
    }

    onGoalScored(teamId)
    onPlaySound("goal")
    spawnGoalConfetti(if (teamId == 1) homeTeam.primaryColorHex else awayTeam.primaryColorHex)
  }

  private fun spawnTurfParticles(pos: Vec2, count: Int) {
    for (i in 0 until count) {
      val angle = Math.random().toFloat() * 2 * PI.toFloat()
      val speed = (Math.random().toFloat() * 180f + 60f)
      particles.add(
        Particle(
          x = pos.x,
          y = pos.y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          colorHex = if (Math.random() > 0.5) 0xFF2E7D32 else 0xFF1B5E20,
          size = (Math.random().toFloat() * 4f + 3f),
          life = 0.5f
        )
      )
    }
  }

  private fun spawnSparkParticles(pos: Vec2, count: Int, colorHex: Long) {
    for (i in 0 until count) {
      val angle = Math.random().toFloat() * 2 * PI.toFloat()
      val speed = (Math.random().toFloat() * 260f + 100f)
      particles.add(
        Particle(
          x = pos.x,
          y = pos.y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          colorHex = colorHex,
          size = (Math.random().toFloat() * 5f + 4f),
          life = 0.4f
        )
      )
    }
  }

  private fun spawnGoalConfetti(teamColorHex: Long) {
    val colors = listOf(teamColorHex, 0xFFFFD700, 0xFFFFFFFF, 0xFF00E5FF, 0xFFFF3366)
    for (i in 0 until 120) {
      val angle = Math.random().toFloat() * 2 * PI.toFloat()
      val speed = (Math.random().toFloat() * 550f + 150f)
      particles.add(
        Particle(
          x = 500f + (Math.random().toFloat() - 0.5f) * 300f,
          y = if (scoringTeamId == 1) PitchConstants.TOP_GOAL_Y else PitchConstants.BOTTOM_GOAL_Y,
          vx = cos(angle) * speed,
          vy = sin(angle) * speed,
          colorHex = colors.random(),
          size = (Math.random().toFloat() * 8f + 5f),
          life = (Math.random().toFloat() * 1.5f + 1.2f)
        )
      )
    }
  }

  private fun updateParticles(dt: Float) {
    val it = particles.iterator()
    while (it.hasNext()) {
      val p = it.next()
      p.life -= dt
      if (p.life <= 0f) {
        it.remove()
      } else {
        p.x += p.vx * dt
        p.y += p.vy * dt
        p.vx *= 0.94f
        p.vy *= 0.94f
        p.alpha = (p.life / 1.0f).coerceIn(0f, 1f)
      }
    }
  }
}
