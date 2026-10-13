package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KitStyle
import com.example.model.TeamKit
import com.example.ui.theme.*

@Composable
fun TeamCustomizerScreen(
  currentHomeTeam: TeamKit,
  currentAwayTeam: TeamKit,
  onTeamsUpdated: (home: TeamKit, away: TeamKit) -> Unit,
  onExit: () -> Unit
) {
  var homeTeam by remember { mutableStateOf(currentHomeTeam) }
  var awayTeam by remember { mutableStateOf(currentAwayTeam) }
  var editingSlot by remember { mutableIntStateOf(1) } // 1 = Home, 2 = Away

  val activeTeam = if (editingSlot == 1) homeTeam else awayTeam

  val availableTeams = remember {
    TeamKit.DEFAULT_TEAMS + listOf(
      TeamKit(
        id = "fra",
        name = "France",
        shortName = "FRA",
        primaryColorHex = 0xFF002395,
        secondaryColorHex = 0xFFED2939,
        accentColorHex = 0xFFFFFFFF,
        starPlayer = "Mbappé"
      ),
      TeamKit(
        id = "arg",
        name = "Argentina",
        shortName = "ARG",
        primaryColorHex = 0xFF75AADB,
        secondaryColorHex = 0xFFFFFFFF,
        accentColorHex = 0xFFF6B40E,
        starPlayer = "Messi"
      )
    )
  }

  val paletteColors = listOf(
    0xFFFFFFFF, 0xFF0055FF, 0xFFFF1744, 0xFF00E676,
    0xFFFFC107, 0xFF9C27B0, 0xFF00E5FF, 0xFF212121
  )

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
          onClick = {
            onTeamsUpdated(homeTeam, awayTeam)
            onExit()
          },
          modifier = Modifier.testTag("customizer_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
        }

        Text(
          text = "TEAM KITS & DUEL SETUP",
          fontWeight = FontWeight.Black,
          fontSize = 16.sp,
          letterSpacing = 1.5.sp,
          color = FloodlightCyan
        )

        IconButton(
          onClick = {
            onTeamsUpdated(homeTeam, awayTeam)
            onExit()
          },
          modifier = Modifier.testTag("customizer_save_button")
        ) {
          Icon(Icons.Default.Check, contentDescription = "Save", tint = FloodlightCyan)
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
        .testTag("team_customizer_screen"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 32.dp)
    ) {
      // Slot selector (Home vs Away)
      item {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(if (editingSlot == 1) FloodlightCyan else StadiumSurfaceDark)
              .border(1.dp, if (editingSlot == 1) FloodlightCyan else StadiumSurfaceBorder, RoundedCornerShape(14.dp))
              .clickable { editingSlot = 1 }
              .padding(vertical = 12.dp)
              .testTag("select_home_slot")
          ) {
            Text(
              text = "HOME: ${homeTeam.name}",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = if (editingSlot == 1) Color.Black else TextWhitePrimary
            )
          }

          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(if (editingSlot == 2) TeamRedPrimary else StadiumSurfaceDark)
              .border(1.dp, if (editingSlot == 2) TeamRedPrimary else StadiumSurfaceBorder, RoundedCornerShape(14.dp))
              .clickable { editingSlot = 2 }
              .padding(vertical = 12.dp)
              .testTag("select_away_slot")
          ) {
            Text(
              text = "AWAY: ${awayTeam.name}",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = if (editingSlot == 2) Color.White else TextWhitePrimary
            )
          }
        }
      }

      // Live 2D Kit Preview Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp)
          ) {
            Text(
              text = "KIT PREVIEW: ${activeTeam.name.uppercase()}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player Kit Model Canvas
            Canvas(modifier = Modifier.size(110.dp)) {
              val center = Offset(size.width / 2f, size.height / 2f)
              val radius = size.width / 2f - 4.dp.toPx()

              // Shadow
              drawOval(
                color = Color(0x66000000),
                topLeft = Offset(center.x - radius * 0.9f, center.y + radius * 0.4f),
                size = androidx.compose.ui.geometry.Size(radius * 1.8f, radius * 0.8f)
              )

              // Jersey fill based on style
              val brush = when (activeTeam.style) {
                KitStyle.VERTICAL_STRIPES -> Brush.horizontalGradient(
                  colors = listOf(activeTeam.primaryColor, activeTeam.secondaryColor, activeTeam.primaryColor),
                  startX = center.x - radius,
                  endX = center.x + radius
                )
                KitStyle.HOOPS -> Brush.verticalGradient(
                  colors = listOf(activeTeam.primaryColor, activeTeam.secondaryColor, activeTeam.primaryColor),
                  startY = center.y - radius,
                  endY = center.y + radius
                )
                else -> Brush.radialGradient(
                  colors = listOf(activeTeam.primaryColor, activeTeam.primaryColor.copy(alpha = 0.8f)),
                  center = center,
                  radius = radius
                )
              }

              drawCircle(brush = brush, radius = radius, center = center)
              drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))

              // Star initial
              drawCircle(color = Color.White, radius = radius * 0.4f, center = center)
              drawCircle(color = activeTeam.primaryColor, radius = radius * 0.35f, center = center)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Star: ${activeTeam.starPlayer}",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = TextWhitePrimary
            )
          }
        }
      }

      // Presets
      item {
        Text(
          text = "OFFICIAL NATIONS & TEAMS",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
          color = FloodlightCyan
        )
      }

      items(availableTeams) { team ->
        val isSelected = activeTeam.id == team.id
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) StadiumSurfaceElevated else StadiumSurfaceDark
          ),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
              if (isSelected) FloodlightCyan else StadiumSurfaceBorder
            )
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (editingSlot == 1) homeTeam = team else awayTeam = team
            }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(team.primaryColor)
                .border(1.5.dp, Color.White, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(team.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextWhitePrimary)
              Text("Star: ${team.starPlayer}", fontSize = 11.sp, color = TextMuted)
            }

            if (isSelected) {
              Icon(Icons.Default.Check, contentDescription = null, tint = FloodlightCyan)
            }
          }
        }
      }

      // Custom Color Palette picker
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "CUSTOM COLOR ACCENT",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = TextMuted
            )

            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              paletteColors.forEach { colorHex ->
                val c = Color(colorHex)
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(
                      2.dp,
                      if (activeTeam.primaryColorHex == colorHex) FloodlightCyan else Color.Transparent,
                      CircleShape
                    )
                    .clickable {
                      val updated = activeTeam.copy(primaryColorHex = colorHex)
                      if (editingSlot == 1) homeTeam = updated else awayTeam = updated
                    }
                )
              }
            }

            // Kit Style Selector
            Text("Stripe Style", fontSize = 12.sp, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              KitStyle.values().forEach { style ->
                val isSelected = activeTeam.style == style
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) FloodlightCyan else StadiumSurfaceElevated)
                    .clickable {
                      val updated = activeTeam.copy(style = style)
                      if (editingSlot == 1) homeTeam = updated else awayTeam = updated
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = style.name.replace("_", " "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else TextWhitePrimary
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
