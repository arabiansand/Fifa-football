package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(context: Context) {
  private val appContext = context.applicationContext
  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  private val audioScope = CoroutineScope(Dispatchers.Default)
  var isAudioEnabled = true
  var isHapticsEnabled = true

  fun playKick() {
    vibrate(30)
    if (!isAudioEnabled) return
    audioScope.launch {
      playTone(freqStart = 140f, freqEnd = 60f, durationMs = 90, gain = 0.8f)
    }
  }

  fun playPowerShot() {
    vibrate(70)
    if (!isAudioEnabled) return
    audioScope.launch {
      playTone(freqStart = 220f, freqEnd = 50f, durationMs = 150, gain = 1.0f)
    }
  }

  fun playTackle() {
    vibrate(60)
    if (!isAudioEnabled) return
    audioScope.launch {
      playNoise(durationMs = 120, gain = 0.6f)
    }
  }

  fun playPostHit() {
    vibrate(80)
    if (!isAudioEnabled) return
    audioScope.launch {
      playTone(freqStart = 880f, freqEnd = 800f, durationMs = 200, gain = 0.9f)
    }
  }

  fun playWhistle() {
    vibrate(100)
    if (!isAudioEnabled) return
    audioScope.launch {
      playTone(freqStart = 2600f, freqEnd = 2900f, durationMs = 350, gain = 0.7f)
    }
  }

  fun playGoalCheer() {
    vibratePattern(longArrayOf(0, 100, 80, 150, 100, 300))
    if (!isAudioEnabled) return
    audioScope.launch {
      // Fanfare sequence
      playTone(freqStart = 523f, freqEnd = 523f, durationMs = 180, gain = 0.7f)
      playTone(freqStart = 659f, freqEnd = 659f, durationMs = 180, gain = 0.75f)
      playTone(freqStart = 783f, freqEnd = 783f, durationMs = 250, gain = 0.85f)
      playTone(freqStart = 1046f, freqEnd = 1046f, durationMs = 450, gain = 0.9f)
    }
  }

  private fun vibrate(durationMs: Long) {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
      }
    } catch (_: Exception) {}
  }

  private fun vibratePattern(timings: LongArray) {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(timings, -1)
      }
    } catch (_: Exception) {}
  }

  private fun playTone(freqStart: Float, freqEnd: Float, durationMs: Int, gain: Float) {
    val sampleRate = 22050
    val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
    if (numSamples <= 0) return
    val buffer = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val progress = i.toFloat() / numSamples
      val currentFreq = freqStart + (freqEnd - freqStart) * progress
      val envelope = (1f - progress) // fade out
      val angle = 2.0 * PI * i * currentFreq / sampleRate
      buffer[i] = (sin(angle) * envelope * gain * Short.MAX_VALUE).toInt().toShort()
    }

    sendToAudioTrack(buffer, sampleRate)
  }

  private fun playNoise(durationMs: Int, gain: Float) {
    val sampleRate = 22050
    val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
    if (numSamples <= 0) return
    val buffer = ShortArray(numSamples)

    var lastVal = 0f
    for (i in 0 until numSamples) {
      val progress = i.toFloat() / numSamples
      val white = (Math.random() * 2 - 1).toFloat()
      lastVal = (lastVal + (0.15f * white)) / 1.15f // brown noise filtering
      val envelope = (1f - progress)
      buffer[i] = (lastVal * envelope * gain * Short.MAX_VALUE).toInt().toShort()
    }

    sendToAudioTrack(buffer, sampleRate)
  }

  private fun sendToAudioTrack(buffer: ShortArray, sampleRate: Int) {
    try {
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(buffer.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(buffer, 0, buffer.size)
      track.play()

      val durationMs = (buffer.size * 1000L / sampleRate) + 80
      audioScope.launch {
        kotlinx.coroutines.delay(durationMs)
        try {
          track.stop()
          track.release()
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
  }
}
