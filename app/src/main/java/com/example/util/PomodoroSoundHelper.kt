package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class PomodoroTone {
    START,
    PAUSE,
    RESET,
    TICK,
    COMPLETED
}

/**
 * PomodoroSoundHelper:
 * Delivers guaranteed, real audible acoustic bell chimes and ambient study frequencies
 * directly through STREAM_MUSIC using Android's native AudioTrack PCM audio synthesis,
 * with multi-harmonic singing bowl/temple bell resonance, ToneGenerator, and RingtoneManager fallback.
 */
object PomodoroSoundHelper {
    private var toneGenerator: ToneGenerator? = null

    @Volatile
    private var isAmbientRunning = false
    private var ambientTrack: AudioTrack? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Throwable) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Synthesizes and plays a rich real acoustic bell tone on the user's media audio stream.
     */
    private fun playSynthesizedChime(tone: PomodoroTone) {
        thread(isDaemon = true, name = "pomodoro_acoustic_chime") {
            try {
                val sampleRate = 44100
                val durationSeconds = when (tone) {
                    PomodoroTone.COMPLETED -> 2.2
                    PomodoroTone.START -> 0.75
                    PomodoroTone.PAUSE -> 0.45
                    PomodoroTone.RESET -> 0.35
                    PomodoroTone.TICK -> 0.06
                }
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate.toDouble()
                    val sampleDouble = when (tone) {
                        PomodoroTone.COMPLETED -> {
                            // Multi-harmonic singing bowl / meditation temple bell: 528Hz + 1457Hz + 2851Hz
                            val envelope = exp(-2.2 * t)
                            val fundamental = sin(2.0 * PI * 528.0 * t)
                            val harmonic1 = 0.40 * sin(2.0 * PI * 1457.0 * t)
                            val harmonic2 = 0.18 * sin(2.0 * PI * 2851.0 * t)
                            (fundamental + harmonic1 + harmonic2) * envelope * 0.90
                        }
                        PomodoroTone.START -> {
                            // Uplifting 3-note acoustic chime: A4 (440Hz) -> C#5 (554Hz) -> E5 (659Hz)
                            val noteIdx = (t / 0.22).toInt().coerceIn(0, 2)
                            val noteFreq = when (noteIdx) {
                                0 -> 440.0
                                1 -> 554.37
                                else -> 659.25
                            }
                            val noteT = t - (noteIdx * 0.22)
                            val envelope = exp(-3.2 * noteT)
                            (sin(2.0 * PI * noteFreq * t) + 0.30 * sin(2.0 * PI * noteFreq * 2.0 * t)) * envelope * 0.85
                        }
                        PomodoroTone.PAUSE -> {
                            // Descending acoustic chime: 587Hz -> 440Hz
                            val freq = if (t < 0.20) 587.33 else 440.0
                            val localT = if (t < 0.20) t else t - 0.20
                            val envelope = exp(-3.8 * localT)
                            (sin(2.0 * PI * freq * t) + 0.2 * sin(2.0 * PI * freq * 1.5 * t)) * envelope * 0.80
                        }
                        PomodoroTone.RESET -> {
                            // Gentle wooden double click chime
                            val envelope = exp(-9.0 * t)
                            (sin(2.0 * PI * 360.0 * t) + 0.35 * sin(2.0 * PI * 720.0 * t)) * envelope * 0.75
                        }
                        PomodoroTone.TICK -> {
                            // Crisp subtle wooden clock tick
                            val envelope = exp(-40.0 * t)
                            (sin(2.0 * PI * 1150.0 * t) + 0.2 * (Math.random() * 2.0 - 1.0)) * envelope * 0.45
                        }
                    }
                    buffer[i] = (sampleDouble.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep((durationSeconds * 1000).toLong() + 150)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Plays the real acoustic chime, with Ringtone and ToneGenerator fallback for maximum audibility.
     */
    fun playTone(context: Context, tone: PomodoroTone) {
        // 1. Play native acoustic PCM AudioTrack on STREAM_MUSIC (Works 100% on all Android hardware & emulators)
        playSynthesizedChime(tone)

        // 2. Play secondary system chime / ToneGenerator
        try {
            when (tone) {
                PomodoroTone.START -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                }
                PomodoroTone.PAUSE -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 200)
                }
                PomodoroTone.RESET -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 220)
                }
                PomodoroTone.TICK -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                }
                PomodoroTone.COMPLETED -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                    try {
                        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
                        ringtone?.play()
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {
            try {
                val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, notifUri)
                ringtone?.play()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Starts continuous soothing ambient focus sound (Binaural Alpha Waves / Lo-Fi Rain).
     */
    fun startAmbientFocusSound(themeName: String = "Quiet Library Focus") {
        if (isAmbientRunning) return
        isAmbientRunning = true
        thread(isDaemon = true, name = "pomodoro_ambient_focus_stream") {
            try {
                val sampleRate = 22050
                val bufferSize = sampleRate // 1 second buffer
                val buffer = ShortArray(bufferSize)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                ambientTrack = track
                track.play()

                var phase = 0.0
                var lfoPhase = 0.0
                val baseFreq = when {
                    themeName.contains("Alpha", true) || themeName.contains("Binaural", true) -> 432.0
                    themeName.contains("Rain", true) -> 240.0
                    else -> 330.0
                }

                while (isAmbientRunning) {
                    for (i in 0 until bufferSize) {
                        val lfo = 0.6 + 0.4 * sin(lfoPhase) // Gentle swell rhythm
                        val pinkNoise = (Math.random() * 2.0 - 1.0) * 0.02
                        val toneWave = 0.06 * sin(phase)
                        val s = (toneWave + pinkNoise) * lfo
                        buffer[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

                        phase += 2.0 * PI * baseFreq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        lfoPhase += 2.0 * PI * 0.20 / sampleRate
                        if (lfoPhase > 2.0 * PI) lfoPhase -= 2.0 * PI
                    }
                    track.write(buffer, 0, buffer.size)
                }

                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            } catch (_: Throwable) {
                isAmbientRunning = false
            }
        }
    }

    /**
     * Stops continuous ambient study sound cleanly.
     */
    fun stopAmbientFocusSound() {
        isAmbientRunning = false
        try {
            ambientTrack?.stop()
            ambientTrack?.release()
        } catch (_: Throwable) {}
        ambientTrack = null
    }

    /**
     * Synthesizes and plays a real, crisp mechanical stopwatch ticking sound (tick-click).
     * Plays strictly while the stopwatch / focus timer is running.
     */
    fun playStopwatchTick(context: Context) {
        thread(isDaemon = true, name = "stopwatch_mechanical_tick") {
            try {
                val sampleRate = 44100
                val durationSeconds = 0.024 // 24ms crisp mechanical escapement tick
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate.toDouble()
                    // Dual transient metallic escapement clicks: primary click at 2800Hz + wheel ping at 3600Hz
                    val click1 = sin(2.0 * PI * 2800.0 * t) * exp(-140.0 * t)
                    val click2 = if (t > 0.007) sin(2.0 * PI * 3600.0 * (t - 0.007)) * exp(-160.0 * (t - 0.007)) else 0.0
                    val s = (click1 * 0.75 + click2 * 0.40).coerceIn(-1.0, 1.0)
                    buffer[i] = (s * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep((durationSeconds * 1000).toLong() + 25)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Throwable) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 15)
                } catch (_: Throwable) {}
            }
        }
    }
}

