package com.example.game

import kotlin.math.*

data class Vec2(var x: Float, var y: Float) {
  operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
  operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
  operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)

  fun length(): Float = hypot(x, y)

  fun normalized(): Vec2 {
    val l = length()
    return if (l > 0.0001f) Vec2(x / l, y / l) else Vec2(0f, 0f)
  }

  fun distanceTo(other: Vec2): Float = hypot(x - other.x, y - other.y)
}

enum class MatchState {
  COUNTDOWN, PLAYING, GOAL_CELEBRATION, HALFTIME, FULLTIME, PAUSED
}

data class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var colorHex: Long,
  var size: Float,
  var alpha: Float = 1f,
  var life: Float = 1f
)

data class Player(
  val id: Int,
  val teamId: Int, // 1 = Home (Bottom), 2 = Away (Top)
  var pos: Vec2,
  var vel: Vec2 = Vec2(0f, 0f),
  var targetVel: Vec2 = Vec2(0f, 0f),
  var facing: Float = 0f, // radians
  val radius: Float = 34f,
  var stamina: Float = 100f,
  var isSprinting: Boolean = false,
  var isTackling: Boolean = false,
  var tackleTimer: Float = 0f,
  var tackleCooldown: Float = 0f,
  var stunnedTimer: Float = 0f,
  var hasBall: Boolean = false,
  var isExhausted: Boolean = false
)

data class Goalkeeper(
  val teamId: Int, // 1 = Home (Bottom, protects y=1600), 2 = Away (Top, protects y=0)
  var pos: Vec2,
  var vel: Vec2 = Vec2(0f, 0f),
  val defaultY: Float,
  val radius: Float = 30f,
  var diveTimer: Float = 0f,
  var diveDirection: Float = 0f // -1 left, 1 right
)

data class Ball(
  var pos: Vec2 = Vec2(500f, 800f),
  var vel: Vec2 = Vec2(0f, 0f),
  var spin: Float = 0f, // -1 to 1 for curve
  val radius: Float = 20f,
  var heightZ: Float = 0f,
  var velZ: Float = 0f,
  var lastTouchTeam: Int = 1,
  var trail: MutableList<Vec2> = mutableListOf()
)

object PitchConstants {
  const val WIDTH = 1000f
  const val HEIGHT = 1600f
  const val PADDING = 60f

  const val GOAL_LEFT = 350f
  const val GOAL_RIGHT = 650f
  const val GOAL_WIDTH = 300f
  const val GOAL_DEPTH = 90f

  const val TOP_GOAL_Y = 60f
  const val BOTTOM_GOAL_Y = 1540f

  const val PENALTY_RADIUS = 150f
  const val CENTER_X = 500f
  const val CENTER_Y = 800f

  // Speeds
  const val BASE_PLAYER_SPEED = 340f // px per second
  const val SPRINT_MULTIPLIER = 1.45f
  const val TACKLE_SPEED = 750f
  const val BALL_FRICTION = 0.982f
  const val AIR_FRICTION = 0.992f
  const val GRAVITY = -750f
}
