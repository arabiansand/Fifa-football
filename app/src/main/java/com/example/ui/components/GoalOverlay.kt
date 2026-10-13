package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TeamKit
import com.example.ui.theme.NeonGold

@Composable
fun GoalOverlay(
  isVisible: Boolean,
  scoringTeam: TeamKit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + scaleIn(initialScale = 0.6f),
    exit = fadeOut() + scaleOut(targetScale = 1.2f),
    modifier = modifier.fillMaxSize()
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0x88000000))
        .testTag("goal_overlay")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .padding(24.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(
                scoringTeam.primaryColor.copy(alpha = 0.95f),
                Color(0xFF0F172A).copy(alpha = 0.95f)
              )
            )
          )
          .border(2.dp, NeonGold, RoundedCornerShape(24.dp))
          .padding(horizontal = 32.dp, vertical = 24.dp)
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, NeonGold, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "⚽",
            fontSize = 24.sp
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "GOAL!!!",
          fontSize = 42.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 4.sp,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "${scoringTeam.name.uppercase()} SCORES!",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = NeonGold
        )

        Text(
          text = "Scored by ${scoringTeam.starPlayer}",
          fontSize = 13.sp,
          color = Color(0xFFE2E8F0)
        )
      }
    }
  }
}
