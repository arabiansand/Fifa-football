package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.FootballDatabase
import com.example.database.MatchHistoryEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsLeaderboardScreen(
  database: FootballDatabase,
  onExit: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val matches by database.matchHistoryDao().getAllMatches().collectAsState(initial = emptyList())

  val totalMatches = matches.size
  val wins = matches.count { it.winner == "HOME" }
  val goalsScored = matches.sumOf { it.homeScore }
  val winRate = if (totalMatches == 0) 0 else ((wins * 100) / totalMatches)

  var showClearConfirm by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = StadiumNightBg,
    topBar = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        IconButton(
          onClick = onExit,
          modifier = Modifier.testTag("stats_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
        }

        Text(
          text = "CAREER STATS & RECORDS",
          fontWeight = FontWeight.Black,
          fontSize = 16.sp,
          letterSpacing = 1.5.sp,
          color = NeonGold
        )

        IconButton(
          onClick = { showClearConfirm = true },
          enabled = matches.isNotEmpty(),
          modifier = Modifier.testTag("clear_history_button")
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Clear", tint = if (matches.isNotEmpty()) Color(0xFFEF4444) else TextMuted)
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
        .testTag("stats_leaderboard_screen"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 32.dp)
    ) {
      // Career Metrics Grid
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonGold)
              Text(
                text = "TROPHY CABINET & CAREER TOTALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = NeonGold
              )
            }

            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              StatCard(title = "Matches", value = "$totalMatches", modifier = Modifier.weight(1f))
              Spacer(modifier = Modifier.width(8.dp))
              StatCard(title = "Victories", value = "$wins", modifier = Modifier.weight(1f))
              Spacer(modifier = Modifier.width(8.dp))
              StatCard(title = "Win Rate", value = "$winRate%", modifier = Modifier.weight(1f))
              Spacer(modifier = Modifier.width(8.dp))
              StatCard(title = "Goals", value = "$goalsScored", modifier = Modifier.weight(1f))
            }
          }
        }
      }

      // Match History Header
      item {
        Text(
          text = "RECENT MATCHES HISTORY",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
          color = FloodlightCyan
        )
      }

      if (matches.isEmpty()) {
        item {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp)
          ) {
            Text(
              text = "No matches played yet.\nJump into Quick Match or Tournament to record your stats!",
              color = TextMuted,
              fontSize = 14.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        items(matches) { match ->
          MatchHistoryCard(match = match)
        }
      }
    }

    if (showClearConfirm) {
      AlertDialog(
        onDismissRequest = { showClearConfirm = false },
        title = { Text("Clear Match Records?", fontWeight = FontWeight.Bold) },
        text = { Text("This will reset all your match history and career statistics.") },
        confirmButton = {
          Button(
            onClick = {
              scope.launch {
                database.matchHistoryDao().clearHistory()
                showClearConfirm = false
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
          ) {
            Text("Clear All")
          }
        },
        dismissButton = {
          TextButton(onClick = { showClearConfirm = false }) {
            Text("Cancel", color = TextWhitePrimary)
          }
        }
      )
    }
  }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .background(StadiumSurfaceElevated, RoundedCornerShape(12.dp))
      .padding(vertical = 12.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextWhitePrimary)
      Text(title, fontSize = 10.sp, color = TextMuted)
    }
  }
}

@Composable
private fun MatchHistoryCard(match: MatchHistoryEntity) {
  val isWin = match.winner == "HOME"
  val isDraw = match.winner == "DRAW"
  val badgeColor = when {
    isWin -> Color(0xFF10B981)
    isDraw -> Color(0xFFF59E0B)
    else -> Color(0xFFEF4444)
  }
  val badgeText = when {
    isWin -> "WIN"
    isDraw -> "DRAW"
    else -> "LOSS"
  }

  val dateStr = remember(match.timestamp) {
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(match.timestamp))
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(14.dp)
    ) {
      // Outcome badge
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Black, color = badgeColor)
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${match.homeTeamName} ${match.homeScore} - ${match.awayScore} ${match.awayTeamName}",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = TextWhitePrimary
        )
        Text(
          text = "${match.mode} · $dateStr · ${match.homePossessionPct}% Poss.",
          fontSize = 11.sp,
          color = TextMuted
        )
      }
    }
  }
}
