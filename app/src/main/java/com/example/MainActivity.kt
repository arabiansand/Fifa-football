package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.audio.SoundManager
import com.example.database.FootballDatabase
import com.example.database.TournamentMatchEntity
import com.example.database.TournamentRepository
import com.example.model.AiDifficulty
import com.example.model.GameMode
import com.example.model.KitStyle
import com.example.model.TeamKit
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StadiumNightBg
import kotlinx.coroutines.launch

enum class AppScreen {
  MAIN_MENU,
  MATCH,
  PENALTY,
  TOURNAMENT,
  TEAM_CUSTOMIZER,
  STATS,
  PROFILE
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val soundManager = SoundManager(this)
    val database = FootballDatabase.getDatabase(this)

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = StadiumNightBg
        ) {
          FootballDuelApp(
            soundManager = soundManager,
            database = database
          )
        }
      }
    }
  }
}

@Composable
fun FootballDuelApp(
  soundManager: SoundManager,
  database: FootballDatabase
) {
  val scope = rememberCoroutineScope()
  var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }

  // User Teams setup (England vs Norway featured marquee duel default)
  var homeTeam by remember { mutableStateOf(TeamKit.ENGLAND) }
  var awayTeam by remember { mutableStateOf(TeamKit.NORWAY) }

  // Observe saved profile from Room to apply custom team colors & name
  val savedProfile by database.playerProfileDao().getPlayerProfile().collectAsState(initial = null)
  LaunchedEffect(savedProfile) {
    savedProfile?.let { prof ->
      val style = runCatching { KitStyle.valueOf(prof.kitStyle) }.getOrDefault(KitStyle.SOLID)
      homeTeam = TeamKit(
        id = "custom_profile",
        name = "${prof.playerName} FC",
        shortName = prof.playerName.take(3).uppercase().ifBlank { "FC" },
        primaryColorHex = prof.primaryColorHex,
        secondaryColorHex = prof.secondaryColorHex,
        accentColorHex = prof.accentColorHex,
        style = style,
        starPlayer = prof.playerName
      )
    }
  }

  // Active match settings
  var activeMode by remember { mutableStateOf(GameMode.QUICK_MATCH) }
  var aiDifficulty by remember { mutableStateOf(AiDifficulty.PRO) }
  var matchDuration by remember { mutableIntStateOf(180) } // 3 minutes default

  // Room-backed Tournament repository
  val tournamentRepository = remember {
    TournamentRepository(database.tournamentDao(), database.matchHistoryDao())
  }
  var activeTournamentMatch by remember { mutableStateOf<TournamentMatchEntity?>(null) }

  when (currentScreen) {
    AppScreen.MAIN_MENU -> {
      MainMenuScreen(
        selectedHomeTeam = homeTeam,
        selectedAwayTeam = awayTeam,
        aiDifficulty = aiDifficulty,
        matchDuration = matchDuration,
        isAudioEnabled = soundManager.isAudioEnabled,
        onDifficultyChanged = { aiDifficulty = it },
        onDurationChanged = { matchDuration = it },
        onToggleAudio = { soundManager.isAudioEnabled = !soundManager.isAudioEnabled },
        onStartMatch = { mode ->
          activeMode = mode
          when (mode) {
            GameMode.QUICK_MATCH, GameMode.TWO_PLAYER_LOCAL -> {
              currentScreen = AppScreen.MATCH
            }
            GameMode.PENALTY_SHOOTOUT -> {
              currentScreen = AppScreen.PENALTY
            }
            GameMode.TOURNAMENT -> {
              currentScreen = AppScreen.TOURNAMENT
            }
          }
        },
        onOpenTeamCustomizer = { currentScreen = AppScreen.TEAM_CUSTOMIZER },
        onOpenStats = { currentScreen = AppScreen.STATS },
        onOpenProfile = { currentScreen = AppScreen.PROFILE }
      )
    }

    AppScreen.MATCH -> {
      BackHandler {
        currentScreen = if (activeMode == GameMode.TOURNAMENT) AppScreen.TOURNAMENT else AppScreen.MAIN_MENU
      }

      val matchHome = if (activeMode == GameMode.TOURNAMENT && activeTournamentMatch != null) {
        val tm = activeTournamentMatch!!
        TeamKit(
          id = tm.teamAId,
          name = tm.teamAName,
          shortName = tm.teamAShortName,
          primaryColorHex = tm.teamAColorHex,
          secondaryColorHex = 0xFFFFFFFF,
          accentColorHex = 0xFFFFD700,
          starPlayer = tm.teamAStarPlayer
        )
      } else {
        homeTeam
      }

      val matchAway = if (activeMode == GameMode.TOURNAMENT && activeTournamentMatch != null) {
        val tm = activeTournamentMatch!!
        TeamKit(
          id = tm.teamBId,
          name = tm.teamBName,
          shortName = tm.teamBShortName,
          primaryColorHex = tm.teamBColorHex,
          secondaryColorHex = 0xFFFFFFFF,
          accentColorHex = 0xFFFFD700,
          starPlayer = tm.teamBStarPlayer
        )
      } else {
        awayTeam
      }

      MatchScreen(
        homeTeam = matchHome,
        awayTeam = matchAway,
        mode = activeMode,
        aiDifficulty = aiDifficulty,
        matchDuration = matchDuration,
        soundManager = soundManager,
        database = database,
        onFinishMatch = {
          currentScreen = if (activeMode == GameMode.TOURNAMENT) AppScreen.TOURNAMENT else AppScreen.MAIN_MENU
        },
        onTournamentAdvance = { hScore, aScore ->
          activeTournamentMatch?.let { match ->
            scope.launch {
              tournamentRepository.recordPlayerMatchResult(
                tournamentId = match.tournamentId,
                stageOrder = match.stageOrder,
                scoreA = hScore,
                scoreB = aScore
              )
            }
          }
        }
      )
    }

    AppScreen.PENALTY -> {
      BackHandler {
        currentScreen = AppScreen.MAIN_MENU
      }

      PenaltyScreen(
        homeTeam = homeTeam,
        awayTeam = awayTeam,
        aiDifficulty = aiDifficulty,
        soundManager = soundManager,
        database = database,
        onExit = { currentScreen = AppScreen.MAIN_MENU }
      )
    }

    AppScreen.TOURNAMENT -> {
      BackHandler {
        currentScreen = AppScreen.MAIN_MENU
      }

      TournamentScreen(
        tournamentRepository = tournamentRepository,
        playerTeam = homeTeam,
        onPlayMatch = { match ->
          activeTournamentMatch = match
          activeMode = GameMode.TOURNAMENT
          currentScreen = AppScreen.MATCH
        },
        onExit = { currentScreen = AppScreen.MAIN_MENU }
      )
    }

    AppScreen.TEAM_CUSTOMIZER -> {
      BackHandler {
        currentScreen = AppScreen.MAIN_MENU
      }

      TeamCustomizerScreen(
        currentHomeTeam = homeTeam,
        currentAwayTeam = awayTeam,
        onTeamsUpdated = { newHome, newAway ->
          homeTeam = newHome
          awayTeam = newAway
        },
        onExit = { currentScreen = AppScreen.MAIN_MENU }
      )
    }

    AppScreen.STATS -> {
      BackHandler {
        currentScreen = AppScreen.MAIN_MENU
      }

      StatsLeaderboardScreen(
        database = database,
        onExit = { currentScreen = AppScreen.MAIN_MENU }
      )
    }

    AppScreen.PROFILE -> {
      BackHandler {
        currentScreen = AppScreen.MAIN_MENU
      }

      PlayerProfileScreen(
        database = database,
        soundManager = soundManager,
        onProfileUpdated = { customTeam ->
          homeTeam = customTeam
        },
        onExit = { currentScreen = AppScreen.MAIN_MENU }
      )
    }
  }
}
