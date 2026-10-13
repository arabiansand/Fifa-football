package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.Vec2
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun VirtualJoystick(
  modifier: Modifier = Modifier,
  sizeDp: Dp = 130.dp,
  stickColor: Color = Color(0xFF00E5FF),
  testTag: String = "virtual_joystick",
  onMove: (Vec2) -> Unit
) {
  var thumbOffset by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier = modifier
      .size(sizeDp)
      .testTag(testTag)
      .pointerInput(Unit) {
        val maxRadius = size.width / 2f
        detectDragGestures(
          onDragStart = { offset ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val diff = offset - center
            val dist = hypot(diff.x, diff.y)
            val clampedDist = min(dist, maxRadius)
            val norm = if (dist > 0.001f) diff / dist else Offset.Zero
            thumbOffset = norm * clampedDist
            onMove(Vec2(norm.x * (clampedDist / maxRadius), norm.y * (clampedDist / maxRadius)))
          },
          onDrag = { change, _ ->
            change.consume()
            val center = Offset(size.width / 2f, size.height / 2f)
            val currentPos = change.position
            val diff = currentPos - center
            val dist = hypot(diff.x, diff.y)
            val clampedDist = min(dist, maxRadius)
            val norm = if (dist > 0.001f) diff / dist else Offset.Zero
            thumbOffset = norm * clampedDist
            onMove(Vec2(norm.x * (clampedDist / maxRadius), norm.y * (clampedDist / maxRadius)))
          },
          onDragEnd = {
            thumbOffset = Offset.Zero
            onMove(Vec2(0f, 0f))
          },
          onDragCancel = {
            thumbOffset = Offset.Zero
            onMove(Vec2(0f, 0f))
          }
        )
      }
  ) {
    Canvas(modifier = Modifier.size(sizeDp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.width / 2f

      // Outer joystick base ring
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x33000000), Color(0x7705121F)),
          center = center,
          radius = radius
        ),
        radius = radius,
        center = center
      )
      drawCircle(
        color = Color(0x6600E5FF),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // Inner guidelines
      drawCircle(
        color = Color(0x22FFFFFF),
        radius = radius * 0.5f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )

      // Joystick thumbstick
      val thumbPos = center + thumbOffset
      val thumbRadius = radius * 0.38f

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(stickColor, stickColor.copy(alpha = 0.5f)),
          center = thumbPos,
          radius = thumbRadius
        ),
        radius = thumbRadius,
        center = thumbPos
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = thumbRadius,
        center = thumbPos,
        style = Stroke(width = 2.dp.toPx())
      )
    }
  }
}
