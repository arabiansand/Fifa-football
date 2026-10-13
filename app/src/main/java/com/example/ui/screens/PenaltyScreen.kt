package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.database.FootballDatabase
import com.example.database.MatchHistoryEntity
import com.example.game.*
import com.example.model.AiDifficulty
import com.example.model.TeamKit
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PenaltyScreen(
  homeTeam: TeamKit,
  awayTeam: TeamKit,
  aiDifficulty: AiDifficulty,
  soundManager: SoundManager,
  database: FootballDatabase,
  onExit: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val engine = remember {
    PenaltyShootoutEngine(
      homeTeam = homeTeam,
      awayTeam = awayTeam,
      aiDifficulty = aiDifficulty,
      onPlaySound = { sound ->
        when (sound) {
          "kick" -> soundManager.playKick()
          "powershot" -> soundManager.playPowerShot()
          "post" -> soundManager.playPostHit()
          "whistle" -> soundManager.playWhistle()
          "goal" -> soundManager.playGoalCheer()
        }
      }
    )
  }

  var aimX by remember { mutableFloatStateOf(500f) }
  var aimHeight by remember { mutableFloatStateOf(0.5f) }
  var powerValue by remember { mutableFloatStateOf(0.8f) }
  var selectedCurve by remember { mutableFloatStateOf(0f) } // -0.6f, 0f, 0.6f

  // 60 FPS animation loop
  LaunchedEffect(Unit) {
    var lastNano = System.nanoTime()
    while (true) {
      withFrameNanos { nowNano ->
        val dt = (nowNano - lastNano) / 1_000_000_000f
        lastNano = nowNano
        engine.update(dt)
      }
    }
  }

  var savedResult by remember { mutableStateOf(false) }
  LaunchedEffect(engine.turnState) {
    if (engine.turnState == ShootoutTurnState.SHOOTOUT_OVER && !savedResult) {
      savedResult = true
      scope.launch {
        database.matchHistoryDao().insertMatch(
          MatchHistoryEntity(
            mode = "Penalty Shootout",
            homeTeamName = homeTeam.name,
            awayTeamName = awayTeam.name,
            homeScore = engine.homeGoals,
            awayScore = engine.awayGoals,
            homeShots = engine.homeAttempts.size,
            awayShots = engine.awayAttempts.size,
            homePossessionPct = 50,
            winner = if (engine.winnerTeamId == 1) "HOME" else "AWAY"
          )
        )
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(StadiumNightBg)
      .testTag("penalty_screen")
  ) {
    // 1. Penalty Shootout Pitch Canvas
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(engine.turnState, engine.currentKickingTeam) {
          if (engine.turnState == ShootoutTurnState.AIMING && engine.currentKickingTeam == 1) {
            detectTapGestures { offset ->
              val scaleX = size.width / 1000f
              val normX = (offset.x / scaleX).coerceIn(370f, 630f)
              aimX = normX
              val normH = (1f - (offset.y / size.height)).coerceIn(0.1f, 0.9f)
              aimHeight = normH
            }
          }
        }
        .pointerInput(engine.turnState, engine.currentKickingTeam) {
          if (engine.turnState == ShootoutTurnState.AIMING && engine.currentKickingTeam == 1) {
            detectDragGestures { change, _ ->
              change.consume()
              val scaleX = size.width / 1000f
              aimX = (change.position.x / scaleX).coerceIn(370f, 630f)
              aimHeight = (1f - (change.position.y / size.height)).coerceIn(0.1f, 0.9f)
            }
          }
        }
    ) {
      val canvasW = size.width
      val canvasH = size.height
      val scaleX = canvasW / 1000f
      val scaleY = canvasH / 1600f

      // Grass turf
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(StadiumPitchDark, StadiumPitchLight)
        ),
        size = size
      )

      // Floodlights
      val glowRad = canvasW * 0.4f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(FloodlightCyanGlow, Color.Transparent),
          center = Offset(canvasW * 0.2f, canvasH * 0.1f),
          radius = glowRad
        ),
        center = Offset(canvasW * 0.2f, canvasH * 0.1f),
        radius = glowRad
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(FloodlightCyanGlow, Color.Transparent),
          center = Offset(canvasW * 0.8f, canvasH * 0.1f),
          radius = glowRad
        ),
        center = Offset(canvasW * 0.8f, canvasH * 0.1f),
        radius = glowRad
      )

      // Goal Frame (y = 750f in normalized coords)
      val goalLeft = 350f * scaleX
      val goalRight = 650f * scaleX
      val goalLineY = 750f * scaleY
      val goalNetTop = 520f * scaleY

      // Net background
      drawRect(
        color = Color(0x66000000),
        topLeft = Offset(goalLeft, goalNetTop),
        size = Size(goalRight - goalLeft, goalLineY - goalNetTop)
      )

      // Net grid
      val gridCols = 8
      val gridRows = 5
      for (c in 0..gridCols) {
        val gx = goalLeft + c * ((goalRight - goalLeft) / gridCols)
        drawLine(color = Color(0x66FFFFFF), start = Offset(gx, goalNetTop), end = Offset(gx, goalLineY), strokeWidth = 1f)
      }
      for (r in 0..gridRows) {
        val gy = goalNetTop + r * ((goalLineY - goalNetTop) / gridRows)
        drawLine(color = Color(0x66FFFFFF), start = Offset(goalLeft, gy), end = Offset(goalRight, gy), strokeWidth = 1f)
      }

      // Goal Posts & Crossbar
      drawLine(
        color = Color.White,
        start = Offset(goalLeft, goalNetTop),
        end = Offset(goalRight, goalNetTop),
        strokeWidth = 8f
      )
      drawLine(
        color = Color.White,
        start = Offset(goalLeft, goalNetTop),
        end = Offset(goalLeft, goalLineY),
        strokeWidth = 8f
      )
      drawLine(
        color = Color.White,
        start = Offset(goalRight, goalNetTop),
        end = Offset(goalRight, goalLineY),
        strokeWidth = 8f
      )
      drawLine(
        color = PitchLineWhite,
        start = Offset(100f * scaleX, goalLineY),
        end = Offset(900f * scaleX, goalLineY),
        strokeWidth = 4f
      )

      // Penalty Spot
      val penSpotY = 1200f * scaleY
      drawCircle(
        color = PitchLineWhite,
        center = Offset(500f * scaleX, penSpotY),
        radius = 8f
      )

      // Goalkeeper
      val gkX = engine.keeperPos.x * scaleX
      val gkY = goalLineY - 10f
      val gkColor = if (engine.currentKickingTeam == 1) awayTeam.primaryColor else homeTeam.primaryColor

      // GK Shadow
      drawOval(
        color = Color(0x66000000),
        topLeft = Offset(gkX - 25f, gkY + 5f),
        size = Size(50f, 20f)
      )
      // GK Jersey
      drawCircle(
        color = gkColor,
        center = Offset(gkX, gkY),
        radius = 28f
      )
      drawCircle(
        color = Color.White,
        center = Offset(gkX, gkY),
        radius = 28f,
        style = Stroke(width = 3f)
      )
      // GK Gloves
      drawCircle(color = Color(0xFFFF9100), center = Offset(gkX - 30f, gkY - 5f), radius = 10f)
      drawCircle(color = Color(0xFFFF9100), center = Offset(gkX + 30f, gkY - 5f), radius = 10f)

      // Aim Reticle (when P1 is aiming)
      if (engine.turnState == ShootoutTurnState.AIMING && engine.currentKickingTeam == 1) {
        val targetScreenX = aimX * scaleX
        val targetScreenY = goalLineY - (goalLineY - goalNetTop) * aimHeight

        drawCircle(
          color = NeonGold.copy(alpha = 0.8f),
          center = Offset(targetScreenX, targetScreenY),
          radius = 18f,
          style = Stroke(width = 2.5f)
        )
        drawLine(
          color = NeonGold,
          start = Offset(targetScreenX - 24f, targetScreenY),
          end = Offset(targetScreenX + 24f, targetScreenY),
          strokeWidth = 2f
        )
        drawLine(
          color = NeonGold,
          start = Offset(targetScreenX, targetScreenY - 24f),
          end = Offset(targetScreenX, targetScreenY + 24f),
          strokeWidth = 2f
        )
      }

      // Ball
      val ballX = engine.ballPos.x * scaleX
      val ballY = engine.ballPos.y * scaleY - engine.ballHeight * 0.35f
      val ballRadius = 22f * scaleX

      // Ball Shadow
      drawOval(
        color = Color(0x66000000),
        topLeft = Offset(ballX - ballRadius, engine.ballPos.y * scaleY),
        size = Size(ballRadius * 2f, ballRadius * 1.2f)
      )
      // Ball
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color.White, Color(0xFFCBD5E1), Color(0xFF64748B)),
          center = Offset(ballX - 4f, ballY - 4f),
          radius = ballRadius
        ),
        center = Offset(ballX, ballY),
        radius = ballRadius
      )
      drawCircle(
        color = Color(0xFF0F172A),
        center = Offset(ballX, ballY),
        radius = ballRadius * 0.4f
      )
    }

    // 2. Penalty Scoreboard Header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(
          onClick = onExit,
          modifier = Modifier.testTag("penalty_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
        }

        Text(
          text = "PENALTY SHOOTOUT",
          fontWeight = FontWeight.Black,
          fontSize = 16.sp,
          letterSpacing = 1.5.sp,
          color = FloodlightCyan
        )

        Box(modifier = Modifier.size(48.dp))
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Score Card & Dots
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Home Team Row
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(homeTeam.primaryColor)
              )
              Text(
                homeTeam.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextWhitePrimary
              )
            }

            val totalPenaltyDots = maxOf(5, engine.homeAttempts.size, engine.awayAttempts.size)
            PenaltyDotsRow(attempts = engine.homeAttempts, totalMax = totalPenaltyDots)

            Text(
              text = "${engine.homeGoals}",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = FloodlightCyan
            )
          }

          // Away Team Row
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(awayTeam.primaryColor)
              )
              Text(
                awayTeam.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextWhitePrimary
              )
            }

            val totalPenaltyDots = maxOf(5, engine.homeAttempts.size, engine.awayAttempts.size)
            PenaltyDotsRow(attempts = engine.awayAttempts, totalMax = totalPenaltyDots)

            Text(
              text = "${engine.awayGoals}",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = TeamRedPrimary
            )
          }
        }
      }
    }

    // 3. Result Banner in Middle
    if (engine.turnState == ShootoutTurnState.RESULT_DISPLAY && engine.lastResult != null) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.align(Alignment.Center)
      ) {
        val (text, color) = when (engine.lastResult!!) {
          KickResult.GOAL -> "GOAL!" to NeonGold
          KickResult.SAVED -> "SAVED BY KEEPER!" to TeamRedPrimary
          KickResult.POST_MISS -> "OFF THE POST!" to Color(0xFFF59E0B)
          KickResult.WIDE_MISS -> "MISSED WIDE!" to Color(0xFFEF4444)
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xDD0F172A))
            .border(2.dp, color, RoundedCornerShape(16.dp))
            .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
          Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 2.sp
          )
        }
      }
    }

    // 4. Winner Modal
    if (engine.turnState == ShootoutTurnState.SHOOTOUT_OVER) {
      val winner = if (engine.winnerTeamId == 1) homeTeam else awayTeam
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xBB000000))
          .padding(24.dp)
      ) {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonGold)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text("🏆", fontSize = 48.sp)
            Text(
              text = "${winner.name.uppercase()} WINS!",
              fontSize = 22.sp,
              fontWeight = FontWeight.Black,
              color = NeonGold
            )
            Text(
              text = "Final Penalties: ${engine.homeGoals} - ${engine.awayGoals}",
              fontSize = 16.sp,
              color = TextWhitePrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = onExit,
              colors = ButtonDefaults.buttonColors(containerColor = FloodlightCyan),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("penalty_exit_button")
            ) {
              Text("Back to Main Menu", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 5. Striker Kick Controls (Bottom)
    if (engine.turnState == ShootoutTurnState.AIMING && engine.currentKickingTeam == 1) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Curve spin selector
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Curve:", fontSize = 12.sp, color = TextMuted)
          listOf(
            -0.6f to "↶ Curl Left",
            0f to "Straight",
            0.6f to "Curl Right ↷"
          ).forEach { (curl, label) ->
            val isSelected = selectedCurve == curl
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) NeonGold else StadiumSurfaceElevated)
                .clickable { selectedCurve = curl }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else TextWhitePrimary
              )
            }
          }
        }

        // Tap/Drag instruction & Shoot Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Tap or drag target reticle\nto place your strike",
            fontSize = 12.sp,
            color = TextMuted
          )

          Button(
            onClick = {
              engine.executeShot(aimX, aimHeight, powerValue, selectedCurve)
            },
            colors = ButtonDefaults.buttonColors(containerColor = FloodlightCyan),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .height(54.dp)
              .testTag("strike_penalty_button")
          ) {
            Text(
              "STRIKE SHOT",
              color = Color.Black,
              fontWeight = FontWeight.Black,
              fontSize = 15.sp
            )
          }
        }
      }
    }

    // AI Striker Preparation Banner
    if (engine.turnState == ShootoutTurnState.AIMING && engine.currentKickingTeam == 2) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 32.dp)
      ) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TeamRedPrimary))
        ) {
          Text(
            text = "⚡ ${awayTeam.starPlayer} is stepping up to strike...",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhitePrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PenaltyDotsRow(attempts: List<PenaltyAttempt>, totalMax: Int) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    for (i in 0 until totalMax) {
      val att = attempts.getOrNull(i)
      val bg = when (att?.result) {
        KickResult.GOAL -> Color(0xFF10B981)
        KickResult.SAVED, KickResult.POST_MISS, KickResult.WIDE_MISS -> Color(0xFFEF4444)
        null -> Color(0xFF334155)
      }

      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(18.dp)
          .clip(CircleShape)
          .background(bg)
      ) {
        if (att?.result == KickResult.GOAL) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        } else if (att?.result != null) {
          Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
      }
    }
  }
}
