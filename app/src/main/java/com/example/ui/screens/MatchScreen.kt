package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.SoundManager
import com.example.database.FootballDatabase
import com.example.database.MatchHistoryEntity
import com.example.game.MatchEngine
import com.example.game.MatchState
import com.example.model.AiDifficulty
import com.example.model.GameMode
import com.example.model.TeamKit
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MatchScreen(
  homeTeam: TeamKit,
  awayTeam: TeamKit,
  mode: GameMode,
  aiDifficulty: AiDifficulty,
  matchDuration: Int,
  soundManager: SoundManager,
  database: FootballDatabase,
  onFinishMatch: () -> Unit,
  onTournamentAdvance: ((homeScore: Int, awayScore: Int) -> Unit)? = null
) {
  val scope = rememberCoroutineScope()
  var isAudioEnabled by remember { mutableStateOf(soundManager.isAudioEnabled) }
  var isPaused by remember { mutableStateOf(false) }
  var showStatsDialog by remember { mutableStateOf(false) }

  val engine = remember {
    MatchEngine(
      homeTeam = homeTeam,
      awayTeam = awayTeam,
      isTwoPlayer = mode == GameMode.TWO_PLAYER_LOCAL,
      aiDifficulty = aiDifficulty,
      matchDurationSeconds = matchDuration,
      onGoalScored = { _ -> },
      onPlaySound = { sound ->
        when (sound) {
          "kick" -> soundManager.playKick()
          "powershot" -> soundManager.playPowerShot()
          "tackle" -> soundManager.playTackle()
          "post" -> soundManager.playPostHit()
          "whistle" -> soundManager.playWhistle()
          "goal" -> soundManager.playGoalCheer()
        }
      }
    ).apply {
      startMatch()
    }
  }

  // Save to database once full-time is reached
  var hasSavedMatch by remember { mutableStateOf(false) }

  // 60 FPS Physics & Render loop
  LaunchedEffect(isPaused) {
    var lastNano = System.nanoTime()
    while (!isPaused) {
      withFrameNanos { nowNano ->
        val dt = (nowNano - lastNano) / 1_000_000_000f
        lastNano = nowNano
        engine.update(dt)

        if (engine.matchState == MatchState.FULLTIME && !hasSavedMatch) {
          hasSavedMatch = true
          showStatsDialog = true
          onTournamentAdvance?.invoke(engine.homeScore, engine.awayScore)
          // Save in Room
          scope.launch {
            val winner = when {
              engine.homeScore > engine.awayScore -> "HOME"
              engine.awayScore > engine.homeScore -> "AWAY"
              else -> "DRAW"
            }
            database.matchHistoryDao().insertMatch(
              MatchHistoryEntity(
                mode = mode.label,
                homeTeamName = homeTeam.name,
                awayTeamName = awayTeam.name,
                homeScore = engine.homeScore,
                awayScore = engine.awayScore,
                homeShots = engine.stats.homeShots,
                awayShots = engine.stats.awayShots,
                homePossessionPct = engine.stats.homePossessionPct,
                winner = winner
              )
            )
          }
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(StadiumNightBg)
      .testTag("match_screen")
  ) {
    // 1. Pitch Canvas
    FootballPitchCanvas(
      engine = engine,
      homeTeam = homeTeam,
      awayTeam = awayTeam,
      modifier = Modifier.fillMaxSize()
    )

    // 2. Scoreboard Banner
    ScoreboardBanner(
      homeTeam = homeTeam,
      awayTeam = awayTeam,
      homeScore = engine.homeScore,
      awayScore = engine.awayScore,
      timeRemainingSeconds = engine.matchTimeSeconds,
      isPaused = isPaused,
      isAudioEnabled = isAudioEnabled,
      isGoalCelebration = engine.matchState == MatchState.GOAL_CELEBRATION,
      scoringTeamId = engine.scoringTeamId,
      isMatchOver = engine.matchState == MatchState.FULLTIME,
      onPauseClick = {
        isPaused = !isPaused
        if (isPaused) showStatsDialog = true
      },
      onToggleAudio = {
        isAudioEnabled = !isAudioEnabled
        soundManager.isAudioEnabled = isAudioEnabled
      },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
    )

    // 3. 2-Player Local duel top controls (if TWO_PLAYER_LOCAL)
    if (mode == GameMode.TWO_PLAYER_LOCAL) {
      // Player 2 Controls (Top Half)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .padding(top = 70.dp, start = 16.dp, end = 16.dp)
      ) {
        // P2 Action Buttons (Left)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          ActionButton(
            label = "SHOOT",
            color = TeamRedPrimary,
            sizeDp = 52.dp,
            isHoldToCharge = true,
            testTag = "p2_shoot_btn",
            onReleaseCharge = { p -> engine.triggerPlayerShot(2, p, engine.p2CurveActive) }
          )
          ActionButton(
            label = "SLIDE",
            color = Color(0xFFF59E0B),
            sizeDp = 52.dp,
            cooldownFraction = (engine.p2.tackleCooldown / 1.1f).coerceIn(0f, 1f),
            testTag = "p2_tackle_btn",
            onTap = { engine.triggerTackle(engine.p2) }
          )
        }

        // P2 Joystick (Right)
        VirtualJoystick(
          sizeDp = 90.dp,
          stickColor = TeamRedPrimary,
          testTag = "p2_joystick",
          modifier = Modifier.align(Alignment.CenterEnd),
          onMove = { stick -> engine.p2Joystick = stick }
        )
      }
    }

    // 4. Player 1 On-Screen Controls (Bottom)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Virtual Joystick (Left)
      VirtualJoystick(
        sizeDp = 120.dp,
        stickColor = FloodlightCyan,
        testTag = "p1_joystick",
        modifier = Modifier.align(Alignment.BottomStart),
        onMove = { stick -> engine.p1Joystick = stick }
      )

      // Action Buttons Cluster (Right)
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.align(Alignment.BottomEnd)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          // Curve Shot Toggle
          ActionButton(
            label = "CURVE",
            color = NeonGold,
            sizeDp = 50.dp,
            isToggle = true,
            isToggledOn = engine.p1CurveActive,
            testTag = "curve_toggle_btn",
            onToggleChanged = { engine.p1CurveActive = it }
          )

          // Turbo Sprint
          ActionButton(
            label = "TURBO",
            color = Color(0xFF10B981),
            sizeDp = 50.dp,
            isToggle = true,
            isToggledOn = engine.p1Sprint,
            testTag = "sprint_toggle_btn",
            onToggleChanged = { engine.p1Sprint = it }
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          // Slide Tackle
          ActionButton(
            label = "SLIDE",
            color = Color(0xFFF59E0B),
            sizeDp = 56.dp,
            cooldownFraction = (engine.p1.tackleCooldown / 1.1f).coerceIn(0f, 1f),
            testTag = "tackle_btn",
            onTap = { engine.triggerTackle(engine.p1) }
          )

          // Shoot / Power Strike
          ActionButton(
            label = "SHOOT",
            color = TeamBluePrimary,
            sizeDp = 62.dp,
            isHoldToCharge = true,
            testTag = "shoot_btn",
            onReleaseCharge = { power ->
              engine.triggerPlayerShot(1, power, engine.p1CurveActive)
            }
          )
        }
      }
    }

    // 5. Goal Celebration Overlay
    GoalOverlay(
      isVisible = engine.matchState == MatchState.GOAL_CELEBRATION,
      scoringTeam = if (engine.scoringTeamId == 1) homeTeam else awayTeam
    )

    // 6. Match Report / Pause Dialog
    if (showStatsDialog) {
      MatchStatsDialog(
        homeTeam = homeTeam,
        awayTeam = awayTeam,
        homeScore = engine.homeScore,
        awayScore = engine.awayScore,
        stats = engine.stats,
        title = if (engine.matchState == MatchState.FULLTIME) "FULL TIME REPORT" else "MATCH PAUSED",
        onRematch = {
          showStatsDialog = false
          isPaused = false
          hasSavedMatch = false
          engine.startMatch()
        },
        onMainMenu = {
          showStatsDialog = false
          if (mode == GameMode.TOURNAMENT && engine.matchState == MatchState.FULLTIME) {
            onTournamentAdvance?.invoke(engine.homeScore, engine.awayScore)
          }
          onFinishMatch()
        },
        onDismiss = if (engine.matchState != MatchState.FULLTIME) {
          {
            showStatsDialog = false
            isPaused = false
          }
        } else null
      )
    }
  }
}
