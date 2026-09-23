package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class GameAudioManager {
    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
    var sfxVolume: Float = 0.8f
    var musicVolume: Float = 0.5f

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private var bgmJob: Job? = null

    companion object {
        private const val SAMPLE_RATE = 22050
    }

    fun playCoin() {
        if (!soundEnabled) return
        audioScope.launch {
            // High cheerful chime: two quick notes (B5 -> E6)
            val note1 = generateTone(988.0, 0.06f, sfxVolume * 0.7f)
            val note2 = generateTone(1318.5, 0.10f, sfxVolume * 0.9f)
            val buffer = ShortArray(note1.size + note2.size)
            System.arraycopy(note1, 0, buffer, 0, note1.size)
            System.arraycopy(note2, 0, buffer, note1.size, note2.size)
            playPcm(buffer)
        }
    }

    fun playJump() {
        if (!soundEnabled) return
        audioScope.launch {
            // Rising spring cartoon sweep (220 Hz -> 580 Hz)
            val duration = 0.18f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toFloat() / numSamples
                val freq = 220.0 + (360.0 * t * t)
                phase += 2 * PI * freq / SAMPLE_RATE
                val envelope = 1.0f - (t * 0.5f)
                val sample = (sin(phase) * 32767 * sfxVolume * 0.65f * envelope).toInt()
                buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playSlide() {
        if (!soundEnabled) return
        audioScope.launch {
            // Whoosh sound: downward pitch sweep with slight grit
            val duration = 0.22f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toFloat() / numSamples
                val freq = 480.0 * (1.0 - t * 0.7)
                phase += 2 * PI * freq / SAMPLE_RATE
                val envelope = sin(t * PI).toFloat()
                val sample = (sin(phase) * 32767 * sfxVolume * 0.5f * envelope).toInt()
                buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playPowerUp() {
        if (!soundEnabled) return
        audioScope.launch {
            // Triumphant cartoon fanfare: C5, E5, G5, C6 arpeggio
            val notes = listOf(523.25, 659.25, 783.99, 1046.50)
            val noteDuration = 0.08f
            val noteSamples = (noteDuration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(noteSamples * notes.size)
            for (n in notes.indices) {
                val freq = notes[n]
                var phase = 0.0
                for (i in 0 until noteSamples) {
                    val t = i.toFloat() / noteSamples
                    phase += 2 * PI * freq / SAMPLE_RATE
                    val envelope = 1.0f - (t * 0.2f)
                    val sample = (sin(phase) * 32767 * sfxVolume * 0.75f * envelope).toInt()
                    buffer[n * noteSamples + i] = sample.coerceIn(-32768, 32767).toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playShieldHit() {
        if (!soundEnabled) return
        audioScope.launch {
            // Metallic resonant "ping" deflection
            val duration = 0.25f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            var p1 = 0.0
            var p2 = 0.0
            for (i in 0 until numSamples) {
                val t = i.toFloat() / numSamples
                p1 += 2 * PI * 880.0 / SAMPLE_RATE
                p2 += 2 * PI * 1320.0 / SAMPLE_RATE
                val envelope = (1.0f - t) * (1.0f - t)
                val sample = ((sin(p1) * 0.6 + sin(p2) * 0.4) * 32767 * sfxVolume * 0.8f * envelope).toInt()
                buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun playCrash() {
        if (!soundEnabled) return
        audioScope.launch {
            // Cartoon bonk + wobble descending trombone slide
            val duration = 0.45f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toFloat() / numSamples
                val wobble = sin(t * 30.0) * 20.0
                val freq = (320.0 - (180.0 * t)) + wobble
                phase += 2 * PI * freq / SAMPLE_RATE
                val envelope = 1.0f - t
                val sample = (sin(phase) * 32767 * sfxVolume * 0.85f * envelope).toInt()
                buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            }
            playPcm(buffer)
        }
    }

    fun startBgm() {
        if (bgmJob?.isActive == true) return
        bgmJob = audioScope.launch {
            // Cheerful, bouncy cartoon melodic rhythm loop
            // Pentatonic scale notes (C4, D4, E4, G4, A4, C5)
            val melodyNotes = listOf(
                523.25, 659.25, 783.99, 659.25,
                587.33, 523.25, 659.25, 783.99,
                880.00, 783.99, 659.25, 523.25,
                587.33, 659.25, 523.25, 0.0
            )
            val noteDurationMs = 180L

            while (isActive) {
                if (!musicEnabled) {
                    delay(300)
                    continue
                }
                for ((index, freq) in melodyNotes.withIndex()) {
                    if (!isActive || !musicEnabled) break
                    if (freq > 0) {
                        // Play melody tone + percussive beat
                        launch {
                            val durationSec = (noteDurationMs.toFloat() / 1000f) * 0.85f
                            val tone = generateMelodyTone(freq, durationSec, musicVolume * 0.35f, hasDholBeat = (index % 2 == 0))
                            playPcm(tone)
                        }
                    } else {
                        // Rest / rhythmic tap
                        launch {
                            val tap = generateDholTap(musicVolume * 0.4f)
                            playPcm(tap)
                        }
                    }
                    delay(noteDurationMs)
                }
            }
        }
    }

    fun stopBgm() {
        bgmJob?.cancel()
        bgmJob = null
    }

    private fun generateTone(freq: Double, durationSec: Float, vol: Float): ShortArray {
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            phase += 2 * PI * freq / SAMPLE_RATE
            val envelope = (1.0f - t).coerceIn(0f, 1f)
            val sample = (sin(phase) * 32767 * vol * envelope).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateMelodyTone(freq: Double, durationSec: Float, vol: Float, hasDholBeat: Boolean): ShortArray {
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)
        var phaseMelody = 0.0
        var phaseBeat = 0.0
        val beatFreq = 85.0 // low dhol thud

        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            phaseMelody += 2 * PI * freq / SAMPLE_RATE
            val envMelody = (1.0f - t * 0.4f)

            var sampleTotal = sin(phaseMelody) * vol * envMelody
            if (hasDholBeat && t < 0.35f) {
                phaseBeat += 2 * PI * beatFreq / SAMPLE_RATE
                val envBeat = (1.0f - (t / 0.35f)) * (1.0f - (t / 0.35f))
                sampleTotal += sin(phaseBeat) * vol * 0.7f * envBeat
            }

            buffer[i] = (sampleTotal * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateDholTap(vol: Float): ShortArray {
        val numSamples = (0.08f * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            phase += 2 * PI * 110.0 / SAMPLE_RATE
            val env = (1.0f - t) * (1.0f - t)
            buffer[i] = (sin(phase) * 32767 * vol * env).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
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
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2.coerceAtLeast(minBufSize))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            // Release after playback finished
            audioScope.launch {
                delay((buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong() + 100)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // AudioTrack allocation fallback
        }
    }
}
