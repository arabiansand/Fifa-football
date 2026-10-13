package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MatchStats
import com.example.model.TeamKit
import com.example.ui.theme.*

@Composable
fun MatchStatsDialog(
  homeTeam: TeamKit,
  awayTeam: TeamKit,
  homeScore: Int,
  awayScore: Int,
  stats: MatchStats,
  title: String = "MATCH REPORT",
  onRematch: () -> Unit,
  onMainMenu: () -> Unit,
  onDismiss: (() -> Unit)? = null
) {
  Dialog(onDismissRequest = { onDismiss?.invoke() ?: onMainMenu() }) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
      border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .testTag("match_stats_dialog")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(20.dp)
      ) {
        Text(
          text = title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          color = FloodlightCyan
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Big Scoreboard
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(homeTeam.primaryColor)
                .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(homeTeam.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhitePrimary)
          }

          Text(
            text = "$homeScore - $awayScore",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(awayTeam.primaryColor)
                .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(awayTeam.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhitePrimary)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Match Stats Comparison
        StatComparisonRow(
          label = "Possession",
          homeVal = "${stats.homePossessionPct}%",
          awayVal = "${stats.awayPossessionPct}%",
          barProgress = stats.homePossessionPct / 100f
        )
        StatComparisonRow(
          label = "Total Shots",
          homeVal = "${stats.homeShots}",
          awayVal = "${stats.awayShots}",
          barProgress = if (stats.homeShots + stats.awayShots == 0) 0.5f else stats.homeShots.toFloat() / (stats.homeShots + stats.awayShots)
        )
        StatComparisonRow(
          label = "Shots on Target",
          homeVal = "${stats.homeShotsOnTarget}",
          awayVal = "${stats.awayShotsOnTarget}",
          barProgress = if (stats.homeShotsOnTarget + stats.awayShotsOnTarget == 0) 0.5f else stats.homeShotsOnTarget.toFloat() / (stats.homeShotsOnTarget + stats.awayShotsOnTarget)
        )
        StatComparisonRow(
          label = "GK Saves",
          homeVal = "${stats.homeSaves}",
          awayVal = "${stats.awaySaves}",
          barProgress = if (stats.homeSaves + stats.awaySaves == 0) 0.5f else stats.homeSaves.toFloat() / (stats.homeSaves + stats.awaySaves)
        )
        StatComparisonRow(
          label = "Tackles Won",
          homeVal = "${stats.homeTackles}",
          awayVal = "${stats.awayTackles}",
          barProgress = if (stats.homeTackles + stats.awayTackles == 0) 0.5f else stats.homeTackles.toFloat() / (stats.homeTackles + stats.awayTackles)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier
              .weight(1f)
              .testTag("main_menu_button")
          ) {
            Text("Main Menu", color = TextWhitePrimary)
          }

          Button(
            onClick = onRematch,
            colors = ButtonDefaults.buttonColors(containerColor = FloodlightCyan),
            modifier = Modifier
              .weight(1f)
              .testTag("rematch_button")
          ) {
            Text("Play Again", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun StatComparisonRow(
  label: String,
  homeVal: String,
  awayVal: String,
  barProgress: Float
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(homeVal, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
      Text(label, fontSize = 11.sp, color = TextMuted)
      Text(awayVal, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
    }
    Spacer(modifier = Modifier.height(3.dp))
    LinearProgressIndicator(
      progress = { barProgress.coerceIn(0.05f, 0.95f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp)),
      color = TeamBluePrimary,
      trackColor = TeamRedPrimary
    )
  }
}
