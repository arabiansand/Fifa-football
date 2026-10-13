package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
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
import com.example.audio.SoundManager
import com.example.database.FootballDatabase
import com.example.database.PlayerProfileEntity
import com.example.model.KitStyle
import com.example.model.TeamKit
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PlayerProfileScreen(
  database: FootballDatabase,
  soundManager: SoundManager,
  onProfileUpdated: (TeamKit) -> Unit,
  onExit: () -> Unit
) {
  val scope = rememberCoroutineScope()

  // Room Reactive Profile State
  val savedProfile by database.playerProfileDao().getPlayerProfile().collectAsState(initial = null)
  val matchHistory by database.matchHistoryDao().getAllMatches().collectAsState(initial = emptyList())
  val allTournaments by database.tournamentDao().getAllTournaments().collectAsState(initial = emptyList())

  // Career Metrics computation
  val totalMatches = matchHistory.size
  val totalWins = matchHistory.count { it.winner == "HOME" }
  val totalGoals = matchHistory.sumOf { it.homeScore }
  val winRate = if (totalMatches == 0) 0 else ((totalWins * 100) / totalMatches)
  val trophiesWon = allTournaments.count { it.isCompleted && it.championTeamName != null }

  // Editable Profile States
  var playerName by remember { mutableStateOf("Captain Striker") }
  var playerTitle by remember { mutableStateOf("World Cup Legend") }
  var selectedAvatar by remember { mutableStateOf("⚽") }
  var primaryColorHex by remember { mutableLongStateOf(0xFF0055FF) }
  var secondaryColorHex by remember { mutableLongStateOf(0xFF00D4FF) }
  var accentColorHex by remember { mutableLongStateOf(0xFFFFFFFF) }
  var selectedKitStyle by remember { mutableStateOf(KitStyle.SOLID) }
  var jerseyNumber by remember { mutableIntStateOf(10) }

  var hasInitializedFromDb by remember { mutableStateOf(false) }
  LaunchedEffect(savedProfile) {
    savedProfile?.let { prof ->
      if (!hasInitializedFromDb) {
        playerName = prof.playerName
        playerTitle = prof.playerTitle
        selectedAvatar = prof.avatarIcon
        primaryColorHex = prof.primaryColorHex
        secondaryColorHex = prof.secondaryColorHex
        accentColorHex = prof.accentColorHex
        selectedKitStyle = runCatching { KitStyle.valueOf(prof.kitStyle) }.getOrDefault(KitStyle.SOLID)
        jerseyNumber = prof.jerseyNumber
        hasInitializedFromDb = true
      }
    }
  }

  // Pre-configured vibrant sports color swatches
  val paletteColors = listOf(
    0xFF0055FF to "Blue",
    0xFFFF1744 to "Crimson",
    0xFF00E676 to "Emerald",
    0xFFFFC107 to "Gold",
    0xFFFFFFFF to "White",
    0xFF1E293B to "Dark Navy",
    0xFF8B5CF6 to "Purple",
    0xFFFF6B00 to "Orange"
  )

  val avatarOptions = listOf("⚽", "👑", "⚡", "🦁", "🔥", "🦅", "⭐")

  fun saveProfileToDb() {
    val updated = PlayerProfileEntity(
      id = 1,
      playerName = playerName.ifBlank { "Striker" },
      playerTitle = playerTitle,
      favoriteTeamId = "custom",
      favoriteTeamName = "${playerName.ifBlank { "Striker" }} XI",
      primaryColorHex = primaryColorHex,
      secondaryColorHex = secondaryColorHex,
      accentColorHex = accentColorHex,
      kitStyle = selectedKitStyle.name,
      avatarIcon = selectedAvatar,
      jerseyNumber = jerseyNumber
    )
    scope.launch {
      database.playerProfileDao().savePlayerProfile(updated)
      soundManager.playKick()

      // Convert to TeamKit and notify parent
      val customKit = TeamKit(
        id = "custom",
        name = "${playerName.ifBlank { "Striker" }} FC",
        shortName = playerName.take(3).uppercase().ifBlank { "FC" },
        primaryColorHex = primaryColorHex,
        secondaryColorHex = secondaryColorHex,
        accentColorHex = accentColorHex,
        style = selectedKitStyle,
        starPlayer = playerName.ifBlank { "Striker" }
      )
      onProfileUpdated(customKit)
      onExit()
    }
  }

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
          modifier = Modifier.testTag("profile_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
        }

        Text(
          text = "PLAYER PROFILE & KIT STUDIO",
          fontWeight = FontWeight.Black,
          fontSize = 15.sp,
          letterSpacing = 1.sp,
          color = FloodlightCyan
        )

        IconButton(
          onClick = { saveProfileToDb() },
          modifier = Modifier.testTag("profile_save_button")
        ) {
          Icon(Icons.Default.Check, contentDescription = "Save Profile", tint = FloodlightCyan)
        }
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
        .testTag("player_profile_screen"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 40.dp)
    ) {
      // 1. Profile Header Card (Avatar + Nickname + Title)
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              // Big Avatar Badge
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(68.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.radialGradient(
                      colors = listOf(Color(primaryColorHex), Color(secondaryColorHex).copy(alpha = 0.7f))
                    )
                  )
                  .border(2.dp, NeonGold, CircleShape)
              ) {
                Text(selectedAvatar, fontSize = 32.sp)
              }

              Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                  value = playerName,
                  onValueChange = { playerName = it },
                  label = { Text("Player Nickname", fontSize = 11.sp) },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhitePrimary,
                    unfocusedTextColor = TextWhitePrimary,
                    focusedBorderColor = FloodlightCyan,
                    unfocusedBorderColor = StadiumSurfaceBorder
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_nickname_input")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = playerTitle,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonGold
                )
              }
            }

            // Avatar picker row
            Text("Select Player Badge Icon:", fontSize = 11.sp, color = TextMuted)
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              avatarOptions.forEach { av ->
                val isSelected = selectedAvatar == av
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) FloodlightCyan.copy(alpha = 0.25f) else StadiumSurfaceElevated)
                    .border(1.dp, if (isSelected) FloodlightCyan else StadiumSurfaceBorder, CircleShape)
                    .clickable { selectedAvatar = av }
                ) {
                  Text(av, fontSize = 18.sp)
                }
              }
            }
          }
        }
      }

      // 2. Career Statistics Section
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StadiumSurfaceBorder)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonGold, modifier = Modifier.size(20.dp))
              Text(
                text = "CAREER STATS & PERFORMANCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = NeonGold
              )
            }

            // Metric Tiles
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              MetricChip(title = "Matches", value = "$totalMatches", modifier = Modifier.weight(1f))
              MetricChip(title = "Victories", value = "$totalWins", modifier = Modifier.weight(1f))
              MetricChip(title = "Win Rate", value = "$winRate%", modifier = Modifier.weight(1f), highlightColor = FloodlightCyan)
              MetricChip(title = "Goals", value = "$totalGoals", modifier = Modifier.weight(1f), highlightColor = NeonGold)
              MetricChip(title = "Trophies", value = "$trophiesWon", modifier = Modifier.weight(1f), highlightColor = Color(0xFF10B981))
            }

            // Win Rate Progress Indicator
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Overall Win Percentage", fontSize = 11.sp, color = TextMuted)
                Text("$winRate%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FloodlightCyan)
              }
              Spacer(modifier = Modifier.height(4.dp))
              LinearProgressIndicator(
                progress = { (winRate / 100f).coerceIn(0f, 1f) },
                color = FloodlightCyan,
                trackColor = StadiumSurfaceElevated,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
              )
            }
          }
        }
      }

      // 3. Custom Team Color Studio & Live Jersey Preview
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = StadiumSurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FloodlightCyan.copy(alpha = 0.5f))),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = "CUSTOM TEAM KIT STUDIO",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = FloodlightCyan
            )

            // Live 2D Kit & Jersey Miniature Preview
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.size(110.dp)
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 4.dp.toPx()

                // Stadium night shadow
                drawOval(
                  color = Color(0x66000000),
                  topLeft = Offset(center.x - radius * 0.9f, center.y + radius * 0.35f),
                  size = androidx.compose.ui.geometry.Size(radius * 1.8f, radius * 0.8f)
                )

                // Fill gradient depending on kit style
                val primary = Color(primaryColorHex)
                val secondary = Color(secondaryColorHex)

                val brush = when (selectedKitStyle) {
                  KitStyle.VERTICAL_STRIPES -> Brush.horizontalGradient(
                    colors = listOf(primary, secondary, primary, secondary),
                    startX = center.x - radius,
                    endX = center.x + radius
                  )
                  KitStyle.HOOPS -> Brush.verticalGradient(
                    colors = listOf(primary, secondary, primary, secondary),
                    startY = center.y - radius,
                    endY = center.y + radius
                  )
                  KitStyle.SASH -> Brush.linearGradient(
                    colors = listOf(primary, secondary, primary),
                    start = Offset(center.x - radius, center.y - radius),
                    end = Offset(center.x + radius, center.y + radius)
                  )
                  KitStyle.SOLID -> Brush.radialGradient(
                    colors = listOf(primary, primary.copy(alpha = 0.85f)),
                    center = center,
                    radius = radius
                  )
                }

                drawCircle(brush = brush, radius = radius, center = center)
                drawCircle(color = Color(accentColorHex), radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))

                // Center number crest
                drawCircle(color = Color(accentColorHex).copy(alpha = 0.3f), radius = radius * 0.42f, center = center)
              }

              // Jersey number text
              Text(
                text = "$jerseyNumber",
                color = Color(accentColorHex),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
              )
            }

            Text(
              text = "${playerName.ifBlank { "Striker" }} · #${jerseyNumber}",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = TextWhitePrimary
            )

            // Primary Color Swatch Selection
            Column(modifier = Modifier.fillMaxWidth()) {
              Text("Primary Kit Color:", fontSize = 12.sp, color = TextWhitePrimary, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                paletteColors.forEach { (colorHex, _) ->
                  val isSelected = primaryColorHex == colorHex
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(Color(colorHex))
                      .border(
                        2.5.dp,
                        if (isSelected) FloodlightCyan else Color.Transparent,
                        CircleShape
                      )
                      .clickable { primaryColorHex = colorHex }
                  )
                }
              }
            }

            // Secondary Color Swatch Selection
            Column(modifier = Modifier.fillMaxWidth()) {
              Text("Secondary / Stripe Color:", fontSize = 12.sp, color = TextWhitePrimary, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                paletteColors.forEach { (colorHex, _) ->
                  val isSelected = secondaryColorHex == colorHex
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(Color(colorHex))
                      .border(
                        2.5.dp,
                        if (isSelected) NeonGold else Color.Transparent,
                        CircleShape
                      )
                      .clickable { secondaryColorHex = colorHex }
                  )
                }
              }
            }

            // Kit Pattern Style
            Column(modifier = Modifier.fillMaxWidth()) {
              Text("Kit Pattern Style:", fontSize = 12.sp, color = TextWhitePrimary, fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                KitStyle.values().forEach { style ->
                  val isSelected = selectedKitStyle == style
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelected) FloodlightCyan else StadiumSurfaceElevated)
                      .border(1.dp, if (isSelected) FloodlightCyan else StadiumSurfaceBorder, RoundedCornerShape(8.dp))
                      .clickable { selectedKitStyle = style }
                      .padding(vertical = 8.dp)
                  ) {
                    Text(
                      text = style.name.replace("_", " "),
                      fontSize = 10.sp,
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

      // 4. Save Button Action
      item {
        Button(
          onClick = { saveProfileToDb() },
          colors = ButtonDefaults.buttonColors(containerColor = FloodlightCyan),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("save_player_profile_btn")
        ) {
          Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "SAVE PROFILE & TEAM COLORS",
            color = Color.Black,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}

@Composable
private fun MetricChip(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
  highlightColor: Color = TextWhitePrimary
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(StadiumSurfaceElevated)
      .border(1.dp, StadiumSurfaceBorder, RoundedCornerShape(10.dp))
      .padding(vertical = 8.dp, horizontal = 4.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = value,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp,
        color = highlightColor
      )
      Text(
        text = title,
        fontSize = 9.sp,
        color = TextMuted
      )
    }
  }
}
