package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ActionButton(
  label: String,
  color: Color,
  modifier: Modifier = Modifier,
  sizeDp: Dp = 64.dp,
  isHoldToCharge: Boolean = false,
  isToggle: Boolean = false,
  isToggledOn: Boolean = false,
  cooldownFraction: Float = 0f, // 0 = ready, 1 = on full cooldown
  testTag: String,
  onTap: () -> Unit = {},
  onReleaseCharge: (power: Float) -> Unit = {},
  onToggleChanged: (Boolean) -> Unit = {}
) {
  var isPressed by remember { mutableStateOf(false) }
  var chargeAmount by remember { mutableFloatStateOf(0.2f) }
  val scope = rememberCoroutineScope()

  val scale by animateFloatAsState(
    targetValue = if (isPressed || isToggledOn) 0.92f else 1f,
    label = "btnScale"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(sizeDp)
      .testTag(testTag)
      .pointerInput(isHoldToCharge, isToggle, isToggledOn, cooldownFraction) {
        if (cooldownFraction > 0.05f) return@pointerInput

        detectTapGestures(
          onPress = {
            if (isToggle) {
              onToggleChanged(!isToggledOn)
              return@detectTapGestures
            }

            isPressed = true
            chargeAmount = 0.2f

            if (isHoldToCharge) {
              val job = scope.launch {
                val startTime = System.currentTimeMillis()
                while (isActive && isPressed) {
                  val elapsed = (System.currentTimeMillis() - startTime) / 750f
                  chargeAmount = (0.2f + elapsed * 0.8f).coerceAtMost(1.0f)
                  delay(16)
                }
              }
              tryAwaitRelease()
              job.cancel()
              isPressed = false
              onReleaseCharge(chargeAmount)
            } else {
              tryAwaitRelease()
              isPressed = false
              onTap()
            }
          }
        )
      }
  ) {
    Canvas(modifier = Modifier.size(sizeDp * scale)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.width / 2f - 2.dp.toPx()

      // Outer glow and fill
      val fillGradient = Brush.radialGradient(
        colors = listOf(
          if (isToggledOn) color else color.copy(alpha = 0.85f),
          if (isToggledOn) color.copy(alpha = 0.7f) else color.copy(alpha = 0.4f)
        ),
        center = center,
        radius = radius
      )

      drawCircle(
        brush = fillGradient,
        radius = radius,
        center = center
      )

      // Ring border
      drawCircle(
        color = if (isToggledOn) Color.White else color,
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // Hold to charge radial sweep
      if (isHoldToCharge && isPressed) {
        val sweep = chargeAmount * 360f
        drawArc(
          color = Color(0xFFFFD700),
          startAngle = -90f,
          sweepAngle = sweep,
          useCenter = false,
          topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
          size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
          style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
      }

      // Cooldown sweep overlay
      if (cooldownFraction > 0.01f) {
        drawArc(
          color = Color(0xAA000000),
          startAngle = -90f,
          sweepAngle = cooldownFraction * 360f,
          useCenter = true,
          topLeft = Offset(0f, 0f),
          size = size
        )
      }
    }

    Text(
      text = label,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp
    )
  }
}
