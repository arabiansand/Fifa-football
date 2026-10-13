package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AiDifficulty
import com.example.model.GameMode
import com.example.model.TeamKit
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
  selectedHomeTeam: TeamKit,
  selectedAwayTeam: TeamKit,
  aiDifficulty: AiDifficulty,
  matchDuration: Int,
  isAudioEnabled: Boolean,
  onDifficultyChanged: (AiDifficulty) -> Unit,
  onDurationChanged: (Int) -> Unit,
  onToggleAudio: () -> Unit,
  onStartMatch: (GameMode) -> Unit,
  onOpenTeamCustomizer: () -> Unit,
  onOpenStats: () -> Unit,
  onOpenProfile: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(StadiumNightBg)
      .testTag("main_menu_screen"),
    contentPadding = PaddingValues(bottom = 32.dp)
  ) {
    // 1. Hero Stadium Night Banner
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(240.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.stadium_pitch_banner_1791855479718),
          contentDescription = "Stadium Pitch Floodlights",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay for text readability
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  StadiumNightBg.copy(alpha = 0.5f),
                  StadiumNightBg
                )
              )
            )
        )

        // Title & Badges
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(FloodlightCyan.copy(alpha = 0.2f))
                .border(1.dp, FloodlightCyan, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "STADIUM NIGHT 1v1",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = FloodlightCyan
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NeonGold.copy(alpha = 0.2f))
                .border(1.dp, NeonGold, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "WORLD CUP 2026",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGold
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "FOOTBALL DUEL",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color.White
          )
          Text(
            text = "${selectedHomeTeam.name} vs ${selectedAwayTeam.name}",
            fontSize = 14.sp,
            color = TextMuted
          )
        }
      }
    }

    // 2. Game Mode Selection Cards
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "SELECT MATCH MODE",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
          color = FloodlightCyan
        )

        ModeCard(
          title = "Quick Match (1v1 vs AI)",
          subtitle = "Face off against smart AI with curve shots & slide tackles",
          icon = Icons.Default.SportsSoccer,
          accentColor = FloodlightCyan,
          testTag = "mode_quick_match",
          onClick = { onStartMatch(GameMode.QUICK_MATCH) }
        )

        ModeCard(
          title = "Local 2-Player Duel",
          subtitle = "Pass & Play split-touch controls on this same screen",
          icon = Icons.Default.People,
          accentColor = TeamRedPrimary,
          testTag = "mode_local_2p",
          onClick = { onStartMatch(GameMode.TWO_PLAYER_LOCAL) }
        )

        ModeCard(
          title = "World Cup Knockout",
          subtitle = "Tournament bracket with England vs Norway & top giants",
          icon = Icons.Default.EmojiEvents,
          accentColor = NeonGold,
          testTag = "mode_tournament",
          onClick = { onStartMatch(GameMode.TOURNAMENT) }
        )

        ModeCard(
          title = "Penalty Shootout Duel",
          subtitle = "Dramatic 5-round sudden death penalty showdown",
          icon = Icons.Default.Adjust,
          accentColor = Color(0xFF10B981),
          testTag = "mode_penalty",
          onClick = { onStartMatch(GameMode.PENALTY_SHOOTOUT) }
        )
      }
    }

    // 3. Match Customization (Difficulty & Duration)
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
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
            Text(
              text = "MATCH SETTINGS",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp,
              color = TextMuted
            )

            // AI Difficulty Selector
            Column {
              Text("AI Difficulty", fontSize = 13.sp, color = TextWhitePrimary, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                AiDifficulty.values().forEach { diff ->
                  val isSelected = aiDifficulty == diff
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (isSelected) FloodlightCyan else StadiumSurfaceElevated)
                      .border(
                        1.dp,
                        if (isSelected) FloodlightCyan else StadiumSurfaceBorder,
                        RoundedCornerShape(10.dp)
                      )
                      .clickable { onDifficultyChanged(diff) }
                      .padding(vertical = 10.dp)
                      .testTag("difficulty_${diff.name.lowercase()}")
                  ) {
                    Text(
                      text = diff.label,
                      color = if (isSelected) Color.Black else TextWhitePrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }

            // Duration Selector
            Column {
              Text("Match Duration", fontSize = 13.sp, color = TextWhitePrimary, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                listOf(90 to "90s Blitz", 180 to "3 Mins", 300 to "5 Mins").forEach { (secs, label) ->
                  val isSelected = matchDuration == secs
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (isSelected) NeonGold else StadiumSurfaceElevated)
                      .border(
                        1.dp,
                        if (isSelected) NeonGold else StadiumSurfaceBorder,
                        RoundedCornerShape(10.dp)
                      )
                      .clickable { onDurationChanged(secs) }
                      .padding(vertical = 10.dp)
                      .testTag("duration_$secs")
                  ) {
                    Text(
                      text = label,
                      color = if (isSelected) Color.Black else TextWhitePrimary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 4. Player Profile, Team Kits & Leaderboard Action Row
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onOpenProfile,
          colors = ButtonDefaults.buttonColors(containerColor = StadiumSurfaceElevated),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, StadiumSurfaceBorder),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("open_profile_button")
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = FloodlightCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Profile", color = TextWhitePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
          onClick = onOpenTeamCustomizer,
          colors = ButtonDefaults.buttonColors(containerColor = StadiumSurfaceElevated),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, StadiumSurfaceBorder),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("open_team_customizer_button")
        ) {
          Icon(Icons.Default.Palette, contentDescription = null, tint = NeonGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Kits", color = TextWhitePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
          onClick = onOpenStats,
          colors = ButtonDefaults.buttonColors(containerColor = StadiumSurfaceElevated),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, StadiumSurfaceBorder),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("open_stats_button")
        ) {
          Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Stats", color = TextWhitePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun ModeCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(16.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.15f))
          .border(1.5.dp, accentColor, CircleShape)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(26.dp)
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = TextWhitePrimary
        )
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = TextMuted
        )
      }

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = TextMuted
      )
    }
  }
}
