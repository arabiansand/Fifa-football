package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.TournamentMatchEntity
import com.example.database.TournamentPlayerStatsEntity
import com.example.database.TournamentRepository
import com.example.model.TeamKit
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class TournamentTab {
  BRACKET, GOLDEN_BOOT
}

@Composable
fun TournamentScreen(
  tournamentRepository: TournamentRepository,
  playerTeam: TeamKit,
  onPlayMatch: (TournamentMatchEntity) -> Unit,
  onExit: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val currentTournament by tournamentRepository.observeLatestTournament().collectAsState(initial = null)
  val tournamentId = currentTournament?.id ?: 0L

  val matches by tournamentRepository.observeMatches(tournamentId).collectAsState(initial = emptyList())
  val playerStats by tournamentRepository.observePlayerStats(tournamentId).collectAsState(initial = emptyList())

  var selectedTab by remember { mutableStateOf(TournamentTab.BRACKET) }

  // Ensure an active tournament exists in Room
  LaunchedEffect(currentTournament) {
    if (currentTournament == null) {
      tournamentRepository.createNewTournament(playerTeam, "World Cup 2026")
    }
  }

  val qfMatches = matches.filter { it.roundCategory == "QF" }
  val sfMatches = matches.filter { it.roundCategory == "SF" }
  val finalMatch = matches.firstOrNull { it.roundCategory == "FINAL" }

  val nextPlayerMatch = matches.firstOrNull { it.isPlayerMatch && !it.isCompleted }

  Scaffold(
    containerColor = StadiumNightBg,
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
          IconButton(
            onClick = onExit,
            modifier = Modifier.testTag("tournament_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = currentTournament?.name ?: "WORLD CUP 2026",
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              letterSpacing = 1.sp,
              color = NeonGold
            )
            Text(
              text = "Representing ${playerTeam.name}",
              fontSize = 11.sp,
              color = FloodlightCyan
            )
          }

          IconButton(
            onClick = {
              scope.launch {
                tournamentRepository.createNewTournament(playerTeam, "World Cup 2026")
              }
            },
            modifier = Modifier.testTag("new_tournament_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = "New Cup", tint = FloodlightCyan)
          }
        }

        // Tab Selector (Bracket vs Golden Boot Stats)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (selectedTab == TournamentTab.BRACKET) FloodlightCyan else StadiumSurfaceDark)
              .border(
                1.dp,
                if (selectedTab == TournamentTab.BRACKET) FloodlightCyan else StadiumSurfaceBorder,
                RoundedCornerShape(10.dp)
              )
              .clickable { selectedTab = TournamentTab.BRACKET }
              .padding(vertical = 8.dp)
              .testTag("tab_bracket")
          ) {
            Text(
              text = "CUP BRACKET",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (selectedTab == TournamentTab.BRACKET) Color.Black else TextWhitePrimary
            )
          }

          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (selectedTab == TournamentTab.GOLDEN_BOOT) NeonGold else StadiumSurfaceDark)
              .border(
                1.dp,
                if (selectedTab == TournamentTab.GOLDEN_BOOT) NeonGold else StadiumSurfaceBorder,
                RoundedCornerShape(10.dp)
              )
              .clickable { selectedTab = TournamentTab.GOLDEN_BOOT }
              .padding(vertical = 8.dp)
              .testTag("tab_player_stats")
          ) {
            Text(
              text = "GOLDEN BOOT STATS",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (selectedTab == TournamentTab.GOLDEN_BOOT) Color.Black else TextWhitePrimary
            )
          }
        }
      }
    },
    bottomBar = {
      if (nextPlayerMatch != null && currentTournament?.isCompleted != true) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
        ) {
          Button(
            onClick = { onPlayMatch(nextPlayerMatch) },
            colors = ButtonDefaults.buttonColors(containerColor = FloodlightCyan),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .testTag("play_next_tournament_match_button")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "PLAY ${nextPlayerMatch.roundName.uppercase()}",
              color = Color.Black,
              fontWeight = FontWeight.Black,
              fontSize = 15.sp
            )
          }
        }
      }
    }
  ) { paddingValues ->
    if (selectedTab == TournamentTab.BRACKET) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp)
          .testTag("tournament_bracket_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
      ) {
        // Progression Stepper
        item {
          CupProgressionStepper(
            hasCompletedQf = qfMatches.isNotEmpty() && qfMatches.all { it.isCompleted },
            hasCompletedSf = sfMatches.isNotEmpty() && sfMatches.all { it.isCompleted },
            hasCompletedFinal = finalMatch?.isCompleted == true
          )
        }

        // Champion Banner
        if (currentTournament?.isCompleted == true && currentTournament?.championTeamName != null) {
          item {
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
              border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonGold)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text("🏆", fontSize = 42.sp)
                Text(
                  text = "WORLD CUP CHAMPION!",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonGold
                )
                Text(
                  text = "${currentTournament?.championTeamName} lifted the trophy!",
                  fontSize = 14.sp,
                  color = TextWhitePrimary
                )
              }
            }
          }
        }

        // Quarter-Finals
        item {
          Text(
            text = "QUARTER-FINALS (8 TEAMS)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = FloodlightCyan
          )
        }

        items(qfMatches) { match ->
          MatchBracketCard(match = match)
        }

        // Semi-Finals
        if (sfMatches.isNotEmpty()) {
          item {
            Text(
              text = "SEMI-FINALS (4 TEAMS)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp,
              color = FloodlightCyan
            )
          }

          items(sfMatches) { match ->
            MatchBracketCard(match = match)
          }
        }

        // Grand Final
        finalMatch?.let { fMatch ->
          item {
            Text(
              text = "WORLD CUP GRAND FINAL",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp,
              color = NeonGold
            )
          }

          item {
            MatchBracketCard(match = fMatch, isFinal = true)
          }
        }
      }
    } else {
      // Golden Boot / Player Stats View
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp)
          .testTag("tournament_player_stats_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
      ) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonGold)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(16.dp)
            ) {
              Text("🥇", fontSize = 32.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "GOLDEN BOOT RACE",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonGold
                )
                Text(
                  text = "Tournament Top Scorers & Player Stats",
                  fontSize = 12.sp,
                  color = TextMuted
                )
              }
            }
          }
        }

        items(playerStats.sortedByDescending { it.goalsScored }) { stat ->
          PlayerStatsCard(stat = stat)
        }
      }
    }
  }
}

@Composable
private fun CupProgressionStepper(
  hasCompletedQf: Boolean,
  hasCompletedSf: Boolean,
  hasCompletedFinal: Boolean
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(StadiumSurfaceDark)
      .border(1.dp, StadiumSurfaceBorder, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    StepChip(label = "Quarter-Finals", isDone = hasCompletedQf, isActive = !hasCompletedQf)
    Text("→", color = TextMuted, fontSize = 12.sp)
    StepChip(label = "Semi-Finals", isDone = hasCompletedSf, isActive = hasCompletedQf && !hasCompletedSf)
    Text("→", color = TextMuted, fontSize = 12.sp)
    StepChip(label = "Final", isDone = hasCompletedFinal, isActive = hasCompletedSf && !hasCompletedFinal)
  }
}

@Composable
private fun StepChip(label: String, isDone: Boolean, isActive: Boolean) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(
        when {
          isDone -> Color(0xFF10B981).copy(alpha = 0.2f)
          isActive -> FloodlightCyan.copy(alpha = 0.2f)
          else -> Color.Transparent
        }
      )
      .border(
        1.dp,
        when {
          isDone -> Color(0xFF10B981)
          isActive -> FloodlightCyan
          else -> Color(0xFF334155)
        },
        RoundedCornerShape(8.dp)
      )
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = when {
        isDone -> Color(0xFF10B981)
        isActive -> FloodlightCyan
        else -> TextMuted
      }
    )
  }
}

@Composable
private fun MatchBracketCard(match: TournamentMatchEntity, isFinal: Boolean = false) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (match.isPlayerMatch) StadiumSurfaceElevated else StadiumSurfaceDark
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = androidx.compose.ui.graphics.SolidColor(
        if (isFinal) NeonGold else if (match.isPlayerMatch) FloodlightCyan else StadiumSurfaceBorder
      )
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = match.roundName,
          fontSize = 11.sp,
          color = if (match.isPlayerMatch) FloodlightCyan else TextMuted,
          fontWeight = FontWeight.Bold
        )
        if (match.isCompleted) {
          Text("FULL TIME", fontSize = 10.sp, color = TextMuted)
        } else if (match.isPlayerMatch) {
          Text("YOUR MATCH", fontSize = 10.sp, color = NeonGold, fontWeight = FontWeight.Bold)
        } else if (match.teamAId == "tbd" || match.teamBId == "tbd") {
          Text("AWAITING ROUND", fontSize = 10.sp, color = TextMuted)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Team A
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
              .size(18.dp)
              .clip(CircleShape)
              .background(Color(match.teamAColorHex))
              .border(1.dp, Color.White, CircleShape)
          )
          Text(
            text = match.teamAName,
            fontWeight = if (match.winnerTeamId == match.teamAId) FontWeight.Black else FontWeight.Normal,
            fontSize = 14.sp,
            color = TextWhitePrimary
          )
        }
        Text(
          text = match.scoreA?.toString() ?: "-",
          fontWeight = FontWeight.Black,
          fontSize = 16.sp,
          color = if (match.winnerTeamId == match.teamAId) NeonGold else TextWhitePrimary
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Team B
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
              .size(18.dp)
              .clip(CircleShape)
              .background(Color(match.teamBColorHex))
              .border(1.dp, Color.White, CircleShape)
          )
          Text(
            text = match.teamBName,
            fontWeight = if (match.winnerTeamId == match.teamBId) FontWeight.Black else FontWeight.Normal,
            fontSize = 14.sp,
            color = TextWhitePrimary
          )
        }
        Text(
          text = match.scoreB?.toString() ?: "-",
          fontWeight = FontWeight.Black,
          fontSize = 16.sp,
          color = if (match.winnerTeamId == match.teamBId) NeonGold else TextWhitePrimary
        )
      }
    }
  }
}

@Composable
private fun PlayerStatsCard(stat: TournamentPlayerStatsEntity) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(14.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stat.playerName,
          fontWeight = FontWeight.Black,
          fontSize = 15.sp,
          color = TextWhitePrimary
        )
        Text(
          text = "${stat.teamName} · ${stat.matchesPlayed} Matches",
          fontSize = 11.sp,
          color = TextMuted
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "${stat.goalsScored}",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = NeonGold
          )
          Text("Goals", fontSize = 10.sp, color = TextMuted)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "${stat.shotsOnTarget}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = FloodlightCyan
          )
          Text("Shots", fontSize = 10.sp, color = TextMuted)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "${stat.tacklesWon}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextWhitePrimary
          )
          Text("Tackles", fontSize = 10.sp, color = TextMuted)
        }
      }
    }
  }
}
