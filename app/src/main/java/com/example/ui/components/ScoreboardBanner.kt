package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TeamKit
import com.example.ui.theme.*

@Composable
fun ScoreboardBanner(
  homeTeam: TeamKit,
  awayTeam: TeamKit,
  homeScore: Int,
  awayScore: Int,
  timeRemainingSeconds: Float,
  isPaused: Boolean,
  isAudioEnabled: Boolean,
  isGoalCelebration: Boolean = false,
  scoringTeamId: Int = 0,
  isMatchOver: Boolean = false,
  onPauseClick: () -> Unit,
  onToggleAudio: () -> Unit,
  modifier: Modifier = Modifier
) {
  val minutes = (timeRemainingSeconds / 60).toInt()
  val seconds = (timeRemainingSeconds % 60).toInt()
  val timeFormatted = if (isMatchOver) "00:00" else String.format("%02d:%02d", minutes, seconds)

  // Infinite breathing transition for match end pulse
  val infiniteTransition = rememberInfiniteTransition(label = "scoreboardInfinite")
  val timerEndPulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "timerEndPulseAlpha"
  )
  val timerEndScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "timerEndScale"
  )

  // Goal celebration banner scale bump
  val bannerScale by animateFloatAsState(
    targetValue = if (isGoalCelebration) 1.025f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "bannerScale"
  )

  // Outer container animated border color
  val targetBorderColor = when {
    isGoalCelebration && scoringTeamId == 1 -> homeTeam.primaryColor
    isGoalCelebration && scoringTeamId == 2 -> awayTeam.primaryColor
    isGoalCelebration -> NeonGold
    isMatchOver -> NeonGold.copy(alpha = timerEndPulseAlpha)
    else -> StadiumSurfaceBorder
  }
  val animatedBorderColor by animateColorAsState(
    targetValue = targetBorderColor,
    animationSpec = tween(durationMillis = 350),
    label = "bannerBorderColor"
  )

  // Home Score scale pop animation on goal
  val homeScoreScale by animateFloatAsState(
    targetValue = if (isGoalCelebration && scoringTeamId == 1) 1.35f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "homeScoreScale"
  )

  // Away Score scale pop animation on goal
  val awayScoreScale by animateFloatAsState(
    targetValue = if (isGoalCelebration && scoringTeamId == 2) 1.35f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "awayScoreScale"
  )

  // Timer box animated background and border
  val timerBorderColor by animateColorAsState(
    targetValue = when {
      isGoalCelebration -> NeonGold
      isMatchOver -> NeonGold.copy(alpha = timerEndPulseAlpha)
      else -> Color(0xFF1E293B)
    },
    animationSpec = tween(durationMillis = 300),
    label = "timerBorderColor"
  )

  val timerLedColor by animateColorAsState(
    targetValue = when {
      isMatchOver -> NeonGold
      isGoalCelebration -> Color(0xFFFBBF24) // warm amber gold
      timeRemainingSeconds <= 30f -> Color(0xFFEF4444) // last 30s tension red
      else -> StadiumScoreboardLed
    },
    animationSpec = tween(durationMillis = 300),
    label = "timerLedColor"
  )

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier
      .fillMaxWidth()
      .scale(bannerScale)
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            if (isGoalCelebration) StadiumSurfaceElevated.copy(alpha = 0.95f) else StadiumSurfaceElevated,
            StadiumSurfaceDark
          )
        )
      )
      .border(
        width = if (isGoalCelebration || isMatchOver) 2.dp else 1.dp,
        color = animatedBorderColor,
        shape = RoundedCornerShape(16.dp)
      )
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    // Home Team Info
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      val isHomeWinner = isMatchOver && homeScore > awayScore
      Box(
        modifier = Modifier
          .size(if (isHomeWinner) 28.dp else 24.dp)
          .scale(if (isHomeWinner) timerEndScale else 1f)
          .clip(CircleShape)
          .background(homeTeam.primaryColor)
          .border(
            width = if (isHomeWinner) 2.dp else 1.dp,
            color = if (isHomeWinner) NeonGold else Color.White,
            shape = CircleShape
          )
      )
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = homeTeam.shortName,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = TextWhitePrimary
          )
          if (isHomeWinner) {
            Spacer(modifier = Modifier.width(4.dp))
            Text("👑", fontSize = 11.sp)
          }
        }
        Text(
          text = homeTeam.starPlayer,
          fontSize = 9.sp,
          color = TextMuted,
          maxLines = 1
        )
      }
    }

    // Center Score & Clock LED
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Home Score
      Text(
        text = "$homeScore",
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        color = if (isGoalCelebration && scoringTeamId == 1) NeonGold else FloodlightCyan,
        modifier = Modifier.scale(homeScoreScale)
      )

      // Center Timer & Status LED
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(if (isMatchOver) timerEndScale else 1.0f)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF030712))
            .border(
              width = if (isGoalCelebration || isMatchOver) 1.5.dp else 1.dp,
              color = timerBorderColor,
              shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = timeFormatted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = timerLedColor
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Dynamic Tag: STADIUM NIGHT vs GOAL! vs FULL TIME
        AnimatedContent(
          targetState = when {
            isMatchOver -> "FULL TIME"
            isGoalCelebration -> "⚡ GOAL! ⚡"
            timeRemainingSeconds <= 30f -> "FINAL SECONDS"
            else -> "STADIUM NIGHT"
          },
          transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
          },
          label = "scoreboardSubtext"
        ) { labelText ->
          Text(
            text = labelText,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = when {
              isMatchOver -> NeonGold
              isGoalCelebration -> NeonGold
              timeRemainingSeconds <= 30f -> Color(0xFFEF4444)
              else -> FloodlightCyan.copy(alpha = 0.8f)
            }
          )
        }
      }

      // Away Score
      Text(
        text = "$awayScore",
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        color = if (isGoalCelebration && scoringTeamId == 2) NeonGold else TeamRedPrimary,
        modifier = Modifier.scale(awayScoreScale)
      )
    }

    // Away Team Info & Controls
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.weight(1f)
    ) {
      val isAwayWinner = isMatchOver && awayScore > homeScore
      Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isAwayWinner) {
            Text("👑", fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
          }
          Text(
            text = awayTeam.shortName,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = TextWhitePrimary
          )
        }
        Text(
          text = awayTeam.starPlayer,
          fontSize = 9.sp,
          color = TextMuted,
          maxLines = 1
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .size(if (isAwayWinner) 28.dp else 24.dp)
          .scale(if (isAwayWinner) timerEndScale else 1f)
          .clip(CircleShape)
          .background(awayTeam.primaryColor)
          .border(
            width = if (isAwayWinner) 2.dp else 1.dp,
            color = if (isAwayWinner) NeonGold else Color.White,
            shape = CircleShape
          )
      )

      IconButton(
        onClick = onToggleAudio,
        modifier = Modifier
          .size(32.dp)
          .testTag("toggle_audio_button")
      ) {
        Icon(
          imageVector = if (isAudioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
          contentDescription = "Audio Toggle",
          tint = TextMuted,
          modifier = Modifier.size(16.dp)
        )
      }

      IconButton(
        onClick = onPauseClick,
        modifier = Modifier
          .size(32.dp)
          .testTag("pause_button")
      ) {
        Icon(
          imageVector = Icons.Default.Pause,
          contentDescription = "Pause",
          tint = FloodlightCyan,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
