package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.example.game.*
import com.example.model.KitStyle
import com.example.model.TeamKit
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FootballPitchCanvas(
  engine: MatchEngine,
  homeTeam: TeamKit,
  awayTeam: TeamKit,
  modifier: Modifier = Modifier
) {
  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("football_pitch_canvas")
  ) {
    val canvasW = size.width
    val canvasH = size.height

    val scaleX = canvasW / PitchConstants.WIDTH
    val scaleY = canvasH / PitchConstants.HEIGHT

    fun toScreenX(x: Float) = x * scaleX
    fun toScreenY(y: Float) = y * scaleY
    fun toScreenDist(d: Float) = d * ((scaleX + scaleY) / 2f)

    // 1. Draw Emerald Pitch Stripes
    val stripeCount = 10
    val stripeH = canvasH / stripeCount
    for (i in 0 until stripeCount) {
      val stripeColor = if (i % 2 == 0) StadiumPitchDark else StadiumPitchLight
      drawRect(
        color = stripeColor,
        topLeft = Offset(0f, i * stripeH),
        size = Size(canvasW, stripeH)
      )
    }

    // 2. Floodlight Corner Glows
    val floodlightRadius = canvasW * 0.45f
    // Top-Left
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(FloodlightCyanGlow, Color.Transparent),
        center = Offset(0f, 0f),
        radius = floodlightRadius
      ),
      radius = floodlightRadius,
      center = Offset(0f, 0f)
    )
    // Top-Right
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(FloodlightCyanGlow, Color.Transparent),
        center = Offset(canvasW, 0f),
        radius = floodlightRadius
      ),
      radius = floodlightRadius,
      center = Offset(canvasW, 0f)
    )
    // Bottom-Left
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(FloodlightCyanGlow, Color.Transparent),
        center = Offset(0f, canvasH),
        radius = floodlightRadius
      ),
      radius = floodlightRadius,
      center = Offset(0f, canvasH)
    )
    // Bottom-Right
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(FloodlightCyanGlow, Color.Transparent),
        center = Offset(canvasW, canvasH),
        radius = floodlightRadius
      ),
      radius = floodlightRadius,
      center = Offset(canvasW, canvasH)
    )

    // 3. Draw Pitch Lines
    val lineStroke = toScreenDist(3.5f)
    val pitchPadX = toScreenX(PitchConstants.PADDING)
    val pitchPadY = toScreenY(PitchConstants.TOP_GOAL_Y)
    val pitchW = toScreenX(PitchConstants.WIDTH - PitchConstants.PADDING) - pitchPadX
    val pitchH = toScreenY(PitchConstants.BOTTOM_GOAL_Y) - pitchPadY

    // Pitch Outer Boundary
    drawRect(
      color = PitchLineWhite.copy(alpha = 0.9f),
      topLeft = Offset(pitchPadX, pitchPadY),
      size = Size(pitchW, pitchH),
      style = Stroke(width = lineStroke)
    )

    // Halfway Line
    val midY = toScreenY(PitchConstants.CENTER_Y)
    drawLine(
      color = PitchLineWhite.copy(alpha = 0.9f),
      start = Offset(pitchPadX, midY),
      end = Offset(pitchPadX + pitchW, midY),
      strokeWidth = lineStroke
    )

    // Center Circle & Spot
    val centerPt = Offset(toScreenX(PitchConstants.CENTER_X), midY)
    drawCircle(
      color = PitchLineWhite.copy(alpha = 0.9f),
      center = centerPt,
      radius = toScreenDist(PitchConstants.PENALTY_RADIUS),
      style = Stroke(width = lineStroke)
    )
    drawCircle(
      color = PitchLineWhite,
      center = centerPt,
      radius = toScreenDist(6f)
    )

    // Penalty Areas (Top and Bottom)
    val penBoxW = toScreenDist(440f)
    val penBoxH = toScreenDist(220f)
    val penBoxLeft = toScreenX(280f)

    // Top Penalty Box (Away)
    drawRect(
      color = PitchLineWhite.copy(alpha = 0.85f),
      topLeft = Offset(penBoxLeft, pitchPadY),
      size = Size(penBoxW, penBoxH),
      style = Stroke(width = lineStroke)
    )
    // Top Goal Area (6-yard)
    drawRect(
      color = PitchLineWhite.copy(alpha = 0.75f),
      topLeft = Offset(toScreenX(380f), pitchPadY),
      size = Size(toScreenDist(240f), toScreenDist(90f)),
      style = Stroke(width = lineStroke)
    )
    // Top Penalty Spot
    drawCircle(
      color = PitchLineWhite,
      center = Offset(toScreenX(500f), toScreenY(220f)),
      radius = toScreenDist(5f)
    )

    // Bottom Penalty Box (Home)
    drawRect(
      color = PitchLineWhite.copy(alpha = 0.85f),
      topLeft = Offset(penBoxLeft, toScreenY(PitchConstants.BOTTOM_GOAL_Y) - penBoxH),
      size = Size(penBoxW, penBoxH),
      style = Stroke(width = lineStroke)
    )
    // Bottom Goal Area (6-yard)
    drawRect(
      color = PitchLineWhite.copy(alpha = 0.75f),
      topLeft = Offset(toScreenX(380f), toScreenY(PitchConstants.BOTTOM_GOAL_Y) - toScreenDist(90f)),
      size = Size(toScreenDist(240f), toScreenDist(90f)),
      style = Stroke(width = lineStroke)
    )
    // Bottom Penalty Spot
    drawCircle(
      color = PitchLineWhite,
      center = Offset(toScreenX(500f), toScreenY(1380f)),
      radius = toScreenDist(5f)
    )

    // Corner Arcs
    val cornerRadius = toScreenDist(24f)
    drawArc(
      color = PitchLineWhite.copy(alpha = 0.8f),
      startAngle = 0f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(pitchPadX - cornerRadius, pitchPadY - cornerRadius),
      size = Size(cornerRadius * 2, cornerRadius * 2),
      style = Stroke(width = lineStroke)
    )
    drawArc(
      color = PitchLineWhite.copy(alpha = 0.8f),
      startAngle = 90f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(pitchPadX + pitchW - cornerRadius, pitchPadY - cornerRadius),
      size = Size(cornerRadius * 2, cornerRadius * 2),
      style = Stroke(width = lineStroke)
    )
    drawArc(
      color = PitchLineWhite.copy(alpha = 0.8f),
      startAngle = 180f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(pitchPadX + pitchW - cornerRadius, pitchPadY + pitchH - cornerRadius),
      size = Size(cornerRadius * 2, cornerRadius * 2),
      style = Stroke(width = lineStroke)
    )
    drawArc(
      color = PitchLineWhite.copy(alpha = 0.8f),
      startAngle = 270f,
      sweepAngle = 90f,
      useCenter = false,
      topLeft = Offset(pitchPadX - cornerRadius, pitchPadY + pitchH - cornerRadius),
      size = Size(cornerRadius * 2, cornerRadius * 2),
      style = Stroke(width = lineStroke)
    )

    // 4. Draw Goal Nets & Crossbars
    drawGoalNet(
      goalLeft = toScreenX(PitchConstants.GOAL_LEFT),
      goalRight = toScreenX(PitchConstants.GOAL_RIGHT),
      lineY = pitchPadY,
      depthY = toScreenY(PitchConstants.TOP_GOAL_Y - PitchConstants.GOAL_DEPTH),
      isTop = true
    )
    drawGoalNet(
      goalLeft = toScreenX(PitchConstants.GOAL_LEFT),
      goalRight = toScreenX(PitchConstants.GOAL_RIGHT),
      lineY = toScreenY(PitchConstants.BOTTOM_GOAL_Y),
      depthY = toScreenY(PitchConstants.BOTTOM_GOAL_Y + PitchConstants.GOAL_DEPTH),
      isTop = false
    )

    // 5. Draw Goalkeepers
    drawGoalkeeper(engine.homeGK, homeTeam, scaleX, scaleY)
    drawGoalkeeper(engine.awayGK, awayTeam, scaleX, scaleY)

    // 6. Draw Players
    drawPlayer(engine.p1, homeTeam, scaleX, scaleY, isActive = true)
    drawPlayer(engine.p2, awayTeam, scaleX, scaleY, isActive = engine.isTwoPlayer)

    // 7. Draw Ball & Trail
    drawBall(engine.ball, scaleX, scaleY)

    // 8. Draw Particles
    for (p in engine.particles) {
      drawCircle(
        color = Color(p.colorHex).copy(alpha = p.alpha),
        center = Offset(toScreenX(p.x), toScreenY(p.y)),
        radius = toScreenDist(p.size)
      )
    }
  }
}

private fun DrawScope.drawGoalNet(
  goalLeft: Float,
  goalRight: Float,
  lineY: Float,
  depthY: Float,
  isTop: Boolean
) {
  // Goal Net background shadow
  drawRect(
    color = Color(0x66000000),
    topLeft = Offset(goalLeft, if (isTop) depthY else lineY),
    size = Size(goalRight - goalLeft, kotlin.math.abs(depthY - lineY))
  )

  // Net Grid lines
  val netColumns = 10
  val netRows = 5
  val colW = (goalRight - goalLeft) / netColumns
  val rowH = kotlin.math.abs(depthY - lineY) / netRows

  for (c in 0..netColumns) {
    val x = goalLeft + c * colW
    drawLine(
      color = Color(0x77FFFFFF),
      start = Offset(x, lineY),
      end = Offset(x, depthY),
      strokeWidth = 1f
    )
  }
  for (r in 0..netRows) {
    val y = if (isTop) lineY - r * rowH else lineY + r * rowH
    drawLine(
      color = Color(0x77FFFFFF),
      start = Offset(goalLeft, y),
      end = Offset(goalRight, y),
      strokeWidth = 1f
    )
  }

  // Crossbar & Posts (Bright White)
  val postStroke = 6f
  drawLine(
    color = Color.White,
    start = Offset(goalLeft, lineY),
    end = Offset(goalRight, lineY),
    strokeWidth = postStroke
  )
  drawCircle(color = Color.White, center = Offset(goalLeft, lineY), radius = 5f)
  drawCircle(color = Color.White, center = Offset(goalRight, lineY), radius = 5f)
}

private fun DrawScope.drawGoalkeeper(
  gk: Goalkeeper,
  team: TeamKit,
  scaleX: Float,
  scaleY: Float
) {
  val x = gk.pos.x * scaleX
  val y = gk.pos.y * scaleY
  val radius = gk.radius * scaleX

  // Shadow
  drawCircle(
    color = Color(0x55000000),
    center = Offset(x + 4f, y + 6f),
    radius = radius
  )

  // GK Jersey (Gold/Yellow/Neon)
  val gkColor = Color(0xFFFFD700)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(gkColor, gkColor.copy(alpha = 0.7f)),
      center = Offset(x, y),
      radius = radius
    ),
    center = Offset(x, y),
    radius = radius
  )
  drawCircle(
    color = Color.White,
    center = Offset(x, y),
    radius = radius,
    style = Stroke(width = 2.5f)
  )

  // Gloves (Left and Right)
  val gloveRadius = radius * 0.35f
  drawCircle(
    color = Color(0xFFFF9100),
    center = Offset(x - radius * 0.85f, y),
    radius = gloveRadius
  )
  drawCircle(
    color = Color(0xFFFF9100),
    center = Offset(x + radius * 0.85f, y),
    radius = gloveRadius
  )
}

private fun DrawScope.drawPlayer(
  player: Player,
  team: TeamKit,
  scaleX: Float,
  scaleY: Float,
  isActive: Boolean
) {
  val x = player.pos.x * scaleX
  val y = player.pos.y * scaleY
  val radius = player.radius * scaleX

  // Directional shadow from stadium lighting
  drawOval(
    color = Color(0x66000000),
    topLeft = Offset(x - radius * 0.9f, y + radius * 0.2f),
    size = Size(radius * 1.8f, radius * 0.9f)
  )

  // Tackle or sprint blur effect
  if (player.isTackling) {
    drawCircle(
      color = team.primaryColor.copy(alpha = 0.4f),
      center = Offset(x - cos(player.facing) * 20f, y - sin(player.facing) * 20f),
      radius = radius * 1.25f
    )
  }

  // Player Jersey Circle
  val fillBrush = when (team.style) {
    KitStyle.VERTICAL_STRIPES -> {
      Brush.horizontalGradient(
        colors = listOf(team.primaryColor, team.secondaryColor, team.primaryColor, team.secondaryColor),
        startX = x - radius,
        endX = x + radius
      )
    }
    KitStyle.HOOPS -> {
      Brush.verticalGradient(
        colors = listOf(team.primaryColor, team.secondaryColor, team.primaryColor, team.secondaryColor),
        startY = y - radius,
        endY = y + radius
      )
    }
    else -> {
      Brush.radialGradient(
        colors = listOf(team.primaryColor, team.primaryColor.copy(alpha = 0.85f)),
        center = Offset(x, y),
        radius = radius
      )
    }
  }

  drawCircle(
    brush = fillBrush,
    center = Offset(x, y),
    radius = radius
  )

  // Border (Secondary Color / White)
  drawCircle(
    color = if (player.isTackling) Color.Yellow else Color.White,
    center = Offset(x, y),
    radius = radius,
    style = Stroke(width = if (player.isTackling) 4f else 2.5f)
  )

  // Aiming Direction Arrow / Cone
  val arrowDist = radius * 1.45f
  val arrowEnd = Offset(x + cos(player.facing) * arrowDist, y + sin(player.facing) * arrowDist)
  drawLine(
    color = if (isActive) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
    start = Offset(x, y),
    end = arrowEnd,
    strokeWidth = 3f
  )
  drawCircle(
    color = if (isActive) Color(0xFFFFD700) else Color.White,
    center = arrowEnd,
    radius = 3.5f
  )

  // Stamina ring around player
  if (player.stamina < 98f) {
    val staminaSweep = (player.stamina / 100f) * 360f
    drawArc(
      color = if (player.stamina < 30f) Color(0xFFFF3B30) else Color(0xFF34C759),
      startAngle = -90f,
      sweepAngle = staminaSweep,
      useCenter = false,
      topLeft = Offset(x - radius * 1.3f, y - radius * 1.3f),
      size = Size(radius * 2.6f, radius * 2.6f),
      style = Stroke(width = 2.5f)
    )
  }
}

private fun DrawScope.drawBall(ball: Ball, scaleX: Float, scaleY: Float) {
  val x = ball.pos.x * scaleX
  val y = ball.pos.y * scaleY
  val radius = ball.radius * scaleX

  // High-speed Trail
  for (i in ball.trail.indices) {
    val tp = ball.trail[i]
    val alpha = ((8 - i) / 8f) * 0.45f
    drawCircle(
      color = Color(0xFFFFD700).copy(alpha = alpha),
      center = Offset(tp.x * scaleX, tp.y * scaleY),
      radius = radius * (1f - i * 0.08f)
    )
  }

  // Ball Height Shadow
  val shadowOffset = (ball.heightZ * 0.15f) * scaleY
  drawOval(
    color = Color(0x66000000),
    topLeft = Offset(x - radius + shadowOffset, y - radius * 0.7f + shadowOffset + 4f),
    size = Size(radius * 2f, radius * 1.4f)
  )

  // Ball Sphere (elevated by ball.heightZ)
  val ballCenter = Offset(x, y - (ball.heightZ * 0.3f) * scaleY)

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8)),
      center = ballCenter - Offset(radius * 0.25f, radius * 0.25f),
      radius = radius
    ),
    center = ballCenter,
    radius = radius
  )
  drawCircle(
    color = Color(0xFF1E293B),
    center = ballCenter,
    radius = radius,
    style = Stroke(width = 1.5f)
  )

  // Classic football pentagon center
  drawCircle(
    color = Color(0xFF0F172A),
    center = ballCenter,
    radius = radius * 0.38f
  )
}
