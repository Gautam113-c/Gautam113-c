package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import com.example.game.FunnyVocalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(private val context: Context) : TextToSpeech.OnInitListener {

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
    var voiceEnabled: Boolean = true
    var vibrationEnabled: Boolean = true
    var voiceVolume: Float = 1.0f
    var sfxVolume: Float = 0.9f
    var musicVolume: Float = 0.35f

    @Volatile
    private var isDuckingMusic = false
    private fun duckMusicForVoice(durationMs: Long = 2000L) {
        if (isDuckingMusic || !musicEnabled) return
        isDuckingMusic = true
        val baseMusic = musicVolume
        musicVolume = (baseMusic * 0.18f).coerceAtLeast(0.06f)
        scope.launch {
            delay(durationMs)
            musicVolume = baseMusic
            isDuckingMusic = false
        }
    }

    private var tts: TextToSpeech? = null
    var isTtsReady: Boolean = false
        private set

    private val _ttsStatus = MutableStateFlow("Initializing Cartoon Voice...")
    val ttsStatus: StateFlow<String> = _ttsStatus.asStateFlow()

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val scope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null
    private val random = Random(System.currentTimeMillis())

    // Audio synthesis parameters
    private val sampleRate = 22050

    // Funny Voice Previews Pool
    private val runnerPreviewQuotes = listOf(
        "Mitron! 56-inch sprint speed!",
        "Chalo chalo! Vikas express ready!",
        "Mitron, catch me if you can!",
        "Wah! Full speed ahead!"
    )

    private val chaserPreviewQuotes = listOf(
        "Arey ruko bhai! Stop running!",
        "Wait for me! Why so fast?!",
        "Khatam... Tata... Bye bye!",
        "Pakad loonga aaj toh!"
    )

    init {
        // Optimize device media stream volume so voices are loud & audible
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (current < max * 0.70f) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * 0.90f).toInt(), 0)
                }
            }
        } catch (_: Exception) {}

        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {
            isTtsReady = false
            _ttsStatus.value = "Procedural Cartoon Synth Active"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val candidateLocales = listOf(
                    Locale("en", "IN"),
                    Locale.US,
                    Locale.UK,
                    Locale("hi", "IN"),
                    Locale.getDefault()
                )

                var matchedLocale: Locale? = null
                for (loc in candidateLocales) {
                    try {
                        val avail = engine.isLanguageAvailable(loc)
                        if (avail != TextToSpeech.LANG_MISSING_DATA && avail != TextToSpeech.LANG_NOT_SUPPORTED) {
                            engine.language = loc
                            matchedLocale = loc
                            break
                        }
                    } catch (_: Exception) {}
                }

                if (matchedLocale == null) {
                    try {
                        engine.language = Locale.US
                        matchedLocale = Locale.US
                    } catch (_: Exception) {}
                }

                // Default cartoon pitch & speech rate
                engine.setPitch(1.45f)
                engine.setSpeechRate(1.3f)
                isTtsReady = true
                _ttsStatus.value = "TTS Active (${matchedLocale?.displayCountry?.ifBlank { "Cartoon Pitch" } ?: "Cartoon Pitch"})"
            }
        } else {
            isTtsReady = false
            _ttsStatus.value = "Procedural Cartoon Synth Active"
        }
    }

    // ==========================================
    // 1. FUNNY CARTOON VOCAL SYNTHESIS (100% RELIABLE)
    // ==========================================

    fun playCartoonVocal(type: FunnyVocalType) {
        if (!voiceEnabled) return
        scope.launch {
            val buffer = when (type) {
                FunnyVocalType.RUNNER_TALK -> generateRunnerBanterTone()
                FunnyVocalType.RUNNER_JUMP -> generateUpwardVocalWhoop(280.0, 720.0, 160)
                FunnyVocalType.RUNNER_SLIDE -> generateDownwardVocalWhoop(640.0, 220.0, 180)
                FunnyVocalType.RUNNER_CHEER -> generateCelebratoryCheer()
                FunnyVocalType.RUNNER_GIGGLE -> generateCartoonGiggle()
                FunnyVocalType.RUNNER_OUCH -> generateComedicBonkGroan()
                FunnyVocalType.CHASER_SHOUT -> generateFranticChaserSqueak()
                FunnyVocalType.CHASER_GASP -> generateChaserGasp()
                FunnyVocalType.CHASER_LAUGH -> generateChaserMischiefChuckle()
                FunnyVocalType.CHASER_TAUNT -> generateUpwardVocalWhoop(380.0, 680.0, 150)
                FunnyVocalType.SAD_TROMBONE -> generateSadTrombone()
                FunnyVocalType.CHA_CHING -> generatePaisaChaChingVocal()
                FunnyVocalType.POWERUP_FANFARE -> generatePowerUpFanfareVocal()
            }
            playPcm(buffer)
        }
    }

    // ==========================================
    // 2. UNIFIED VOICE LINE TRIGGER (SYNTH + TTS)
    // ==========================================

    fun speakVoiceLine(line: String, isChaser: Boolean = false, vocalType: FunnyVocalType? = null) {
        if (!voiceEnabled) return

        // Duck background music for 2.4s so voice speech is loud and clear
        duckMusicForVoice(2400L)

        // 1. Always play the hilarious procedural cartoon vocal tone
        val vocal = vocalType ?: if (isChaser) FunnyVocalType.CHASER_SHOUT else FunnyVocalType.RUNNER_TALK
        playCartoonVocal(vocal)

        // 2. If TTS is available, speak line with funny character pitch at MAX volume
        if (isTtsReady && tts != null) {
            scope.launch {
                try {
                    tts?.let { engine ->
                        if (isChaser) {
                            // High-pitched squeaky cartoon chaser voice
                            engine.setPitch(1.85f)
                            engine.setSpeechRate(1.35f)
                        } else {
                            // Energetic, confident, humorous runner voice
                            engine.setPitch(1.30f)
                            engine.setSpeechRate(1.20f)
                        }
                        val params = Bundle().apply {
                            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                        }
                        engine.speak(line, TextToSpeech.QUEUE_FLUSH, params, "v_${System.currentTimeMillis()}")
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // ==========================================
    // 3. UI SOUNDBOARD PREVIEW METHODS
    // ==========================================

    fun previewRunnerVoice() {
        triggerHaptic(30)
        val quote = runnerPreviewQuotes.random(random)
        speakVoiceLine(quote, isChaser = false, vocalType = FunnyVocalType.RUNNER_CHEER)
    }

    fun previewChaserVoice() {
        triggerHaptic(30)
        val quote = chaserPreviewQuotes.random(random)
        speakVoiceLine(quote, isChaser = true, vocalType = FunnyVocalType.CHASER_SHOUT)
    }

    fun previewCartoonBabble() {
        triggerHaptic(20)
        playCartoonVocal(FunnyVocalType.RUNNER_GIGGLE)
    }

    fun previewSadTrombone() {
        triggerHaptic(50)
        playCartoonVocal(FunnyVocalType.SAD_TROMBONE)
    }

    fun previewCoinVoice() {
        triggerHaptic(25)
        speakVoiceLine("Paisa hi paisa! Balle balle!", isChaser = false, vocalType = FunnyVocalType.CHA_CHING)
    }

    // ==========================================
    // 4. MUSIC & SOUND EFFECTS
    // ==========================================

    fun startBackgroundMusic() {
        if (musicJob != null && musicJob?.isActive == true) return
        musicJob = scope.launch {
            // Energetic, joyful Indian Bhupali/Bilawal pentatonic loop with rhythmic dhol beats
            val melody = intArrayOf(
                262, 330, 392, 440, 523, 392, 330, 262,
                294, 330, 392, 523, 440, 392, 330, 294,
                330, 392, 523, 659, 523, 392, 440, 330,
                262, 294, 330, 392, 330, 294, 262, 262
            )
            var noteIndex = 0

            while (isActive) {
                if (musicEnabled) {
                    val freq = melody[noteIndex % melody.size]
                    val isBeat = noteIndex % 4 == 0
                    playSynthesizedNote(freq, if (isBeat) 120 else 80, isBeat)
                    noteIndex++
                    delay(115)
                } else {
                    delay(400)
                }
            }
        }
    }

    fun stopBackgroundMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun playCoinSound() {
        if (!soundEnabled) return
        triggerHaptic(15)
        scope.launch {
            playTone(880, 40, sfxVolume * 0.45f)
            delay(30)
            playTone(1320, 70, sfxVolume * 0.6f)
        }
    }

    fun playJumpSound() {
        if (!soundEnabled) return
        triggerHaptic(20)
        scope.launch {
            playSweep(260, 780, 120, sfxVolume * 0.45f)
        }
    }

    fun playSlideSound() {
        if (!soundEnabled) return
        triggerHaptic(20)
        scope.launch {
            playSweep(600, 200, 110, sfxVolume * 0.4f)
        }
    }

    fun playPowerUpSound() {
        if (!soundEnabled) return
        triggerHaptic(60)
        scope.launch {
            val notes = intArrayOf(523, 659, 784, 1046)
            for (n in notes) {
                playTone(n, 55, sfxVolume * 0.5f)
                delay(50)
            }
        }
    }

    fun playShieldHitSound() {
        if (!soundEnabled) return
        triggerHaptic(80)
        scope.launch {
            playSweep(400, 850, 90, sfxVolume * 0.55f)
            delay(35)
            playSweep(750, 220, 130, sfxVolume * 0.45f)
        }
    }

    fun playGameOverSound() {
        if (!soundEnabled) return
        triggerHaptic(140)
        scope.launch {
            playCartoonVocal(FunnyVocalType.SAD_TROMBONE)
        }
    }

    // ==========================================
    // 5. PROCEDURAL AUDIO WAVEFORM GENERATORS
    // ==========================================

    private fun generateRunnerBanterTone(): ShortArray {
        // 3-syllable bouncy cartoon chatter ("Dah-da-doo!")
        val syllableFreqs = listOf(320.0, 440.0, 360.0)
        val syllableDurations = listOf(0.08f, 0.07f, 0.11f)
        val numSamples = (0.32f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        var sampleOffset = 0
        for (i in syllableFreqs.indices) {
            val freq = syllableFreqs[i]
            val duration = syllableDurations[i]
            val sylSamples = (duration * sampleRate).toInt()

            for (s in 0 until sylSamples) {
                if (sampleOffset + s >= numSamples) break
                val t = s.toDouble() / sampleRate
                // Glottal fundamental + formant harmonic (human-like vowel resonance)
                val fundamental = sin(2.0 * PI * freq * t)
                val formant = sin(2.0 * PI * (freq * 2.3) * t) * 0.4
                val wave = fundamental * 0.65 + formant

                val env = sin(s.toDouble() / sylSamples * PI).toFloat()
                val finalSample = (wave * env * 32767.0 * voiceVolume * 1.25f).toInt()
                buffer[sampleOffset + s] = finalSample.coerceIn(-32768, 32767).toShort()
            }
            sampleOffset += sylSamples + (0.02f * sampleRate).toInt() // small gap
        }
        return buffer
    }

    private fun generateFranticChaserSqueak(): ShortArray {
        // High-pitched frantic comic squeak ("A-re-re-re!")
        val duration = 0.28f
        val numSamples = (duration * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            // Rapid pitch flutter 580 Hz to 780 Hz
            val flutter = sin(progress * 45.0) * 100.0
            val freq = 660.0 + flutter
            phase += 2.0 * PI * freq / sampleRate

            val wave = sin(phase) + sin(phase * 2.0) * 0.25
            val envelope = (1.0 - progress).toFloat()
            val sample = (wave * envelope * 32767.0 * voiceVolume * 1.25f).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateUpwardVocalWhoop(startFreq: Double, endFreq: Double, durationMs: Int): ShortArray {
        val durationSec = durationMs / 1000.0
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * (progress * progress)
            phase += 2.0 * PI * freq / sampleRate

            val wave = sin(phase) + sin(phase * 1.8) * 0.3
            val envelope = when {
                progress < 0.15 -> (progress / 0.15).toFloat()
                else -> (1.0 - (progress - 0.15) / 0.85).toFloat()
            }
            val sample = (wave * envelope * 32767.0 * voiceVolume * 1.25f).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateDownwardVocalWhoop(startFreq: Double, endFreq: Double, durationMs: Int): ShortArray {
        val durationSec = durationMs / 1000.0
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / sampleRate

            val wave = sin(phase) + sin(phase * 2.1) * 0.2
            val envelope = sin(progress * PI).toFloat()
            val sample = (wave * envelope * 32767.0 * voiceVolume * 1.20f).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateCelebratoryCheer(): ShortArray {
        // "Woo-hoo!" Two distinct joyful rising peaks
        val numSamples = (0.34f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val half = numSamples / 2
        var p1 = 0.0
        for (i in 0 until half) {
            val prog = i.toDouble() / half
            val f = 340.0 + (prog * 220.0) // 340 -> 560
            p1 += 2.0 * PI * f / sampleRate
            val env = sin(prog * PI).toFloat()
            buffer[i] = (sin(p1) * env * 32767.0 * voiceVolume * 1.25f).toInt().coerceIn(-32768, 32767).toShort()
        }
        var p2 = 0.0
        for (i in half until numSamples) {
            val prog = (i - half).toDouble() / half
            val f = 460.0 + (prog * 360.0) // 460 -> 820
            p2 += 2.0 * PI * f / sampleRate
            val env = sin(prog * PI).toFloat()
            buffer[i] = (sin(p2) * env * 32767.0 * voiceVolume * 1.30f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateCartoonGiggle(): ShortArray {
        // Four rapid staccato chuckle bursts ("He-he-he-he!")
        val numSamples = (0.36f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val freqs = listOf(520.0, 570.0, 500.0, 550.0)
        val chunkLen = numSamples / 4

        for (c in freqs.indices) {
            val baseFreq = freqs[c]
            var phase = 0.0
            val startIdx = c * chunkLen
            for (i in 0 until (chunkLen * 0.75).toInt()) {
                val idx = startIdx + i
                if (idx >= numSamples) break
                val prog = i.toDouble() / (chunkLen * 0.75)
                phase += 2.0 * PI * baseFreq / sampleRate
                val env = sin(prog * PI).toFloat()
                buffer[idx] = (sin(phase) * env * 32767.0 * voiceVolume * 1.30f).toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return buffer
    }

    private fun generateComedicBonkGroan(): ShortArray {
        // Comedic bonk with a low pitched "Ooof-arey" descending wobble
        val numSamples = (0.32f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val wobble = sin(progress * 35.0) * 25.0
            val freq = (320.0 - (160.0 * progress)) + wobble
            phase += 2.0 * PI * freq / sampleRate

            val wave = sin(phase) + sin(phase * 0.5) * 0.35
            val envelope = (1.0 - progress).toFloat()
            buffer[i] = (wave * envelope * 32767.0 * voiceVolume * 1.30f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateChaserGasp(): ShortArray {
        // Breathless cartoon panting chatter
        val numSamples = (0.24f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val prog = i.toDouble() / numSamples
            val freq = 480.0 + sin(prog * 60.0) * 120.0
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(prog * PI * 2.0).toFloat().coerceAtLeast(0f)
            buffer[i] = (sin(phase) * env * 32767.0 * voiceVolume * 1.20f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateChaserMischiefChuckle(): ShortArray {
        // Mischievous cartoon villain chuckle ("Ha-ha-ha-ha!")
        val numSamples = (0.35f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val freqs = listOf(480.0, 440.0, 400.0, 360.0)
        val chunkLen = numSamples / 4

        for (c in freqs.indices) {
            val baseFreq = freqs[c]
            var phase = 0.0
            val startIdx = c * chunkLen
            for (i in 0 until (chunkLen * 0.7).toInt()) {
                val idx = startIdx + i
                if (idx >= numSamples) break
                val prog = i.toDouble() / (chunkLen * 0.7)
                phase += 2.0 * PI * baseFreq / sampleRate
                val env = sin(prog * PI).toFloat()
                buffer[idx] = (sin(phase) * env * 32767.0 * voiceVolume * 1.25f).toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return buffer
    }

    private fun generateSadTrombone(): ShortArray {
        // The legendary comedy meme riff: 4 descending notes (Wah-wah-wah-waaaah)
        // F#4 (370Hz), F4 (349Hz), E4 (330Hz), Eb4/D4 (293->260Hz with heavy vibrato)
        val noteDurations = listOf(0.18f, 0.18f, 0.18f, 0.45f)
        val totalSec = noteDurations.sum()
        val numSamples = (totalSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val noteFreqs = listOf(370.0, 349.2, 329.6, 293.7)
        var offset = 0

        for (n in noteFreqs.indices) {
            val baseFreq = noteFreqs[n]
            val duration = noteDurations[n]
            val noteSamples = (duration * sampleRate).toInt()
            var phase = 0.0

            for (s in 0 until noteSamples) {
                if (offset + s >= numSamples) break
                val prog = s.toDouble() / noteSamples

                // Last note slides down with dramatic comic vibrato
                val freq = if (n == 3) {
                    (baseFreq - (prog * 35.0)) + (sin(prog * 30.0) * 8.0)
                } else {
                    baseFreq
                }

                phase += 2.0 * PI * freq / sampleRate
                // Brassy cartoon timbre: rich harmonics
                val wave = sin(phase) * 0.6 + sin(phase * 2.0) * 0.25 + sin(phase * 3.0) * 0.15

                val env = when {
                    prog < 0.1 -> (prog / 0.1).toFloat()
                    else -> (1.0 - (prog - 0.1) / 0.9).toFloat()
                }

                val sample = (wave * env * 32767.0 * voiceVolume * 1.35f).toInt()
                buffer[offset + s] = sample.coerceIn(-32768, 32767).toShort()
            }
            offset += noteSamples
        }
        return buffer
    }

    private fun generatePaisaChaChingVocal(): ShortArray {
        // Double bright ding + quick vocal chirp
        val numSamples = (0.28f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        var p1 = 0.0
        var p2 = 0.0
        val half = numSamples / 2

        for (i in 0 until half) {
            val prog = i.toDouble() / half
            p1 += 2.0 * PI * 1320.0 / sampleRate
            val env = (1.0 - prog).toFloat()
            buffer[i] = (sin(p1) * env * 32767.0 * voiceVolume * 1.25f).toInt().coerceIn(-32768, 32767).toShort()
        }
        for (i in half until numSamples) {
            val prog = (i - half).toDouble() / half
            val f = 600.0 + (prog * 400.0)
            p2 += 2.0 * PI * f / sampleRate
            val env = sin(prog * PI).toFloat()
            buffer[i] = (sin(p2) * env * 32767.0 * voiceVolume * 1.30f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generatePowerUpFanfareVocal(): ShortArray {
        // Ascending triumphant 4-note arpeggio (C5, E5, G5, High C6)
        val notes = listOf(523.25, 659.25, 783.99, 1046.50)
        val noteSamples = (0.075f * sampleRate).toInt()
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        for (n in notes.indices) {
            val freq = notes[n]
            var phase = 0.0
            for (i in 0 until noteSamples) {
                val prog = i.toDouble() / noteSamples
                phase += 2.0 * PI * freq / sampleRate
                val wave = sin(phase) + sin(phase * 2.0) * 0.3
                val env = (1.0 - prog * 0.3).toFloat()
                buffer[n * noteSamples + i] = (wave * env * 32767.0 * voiceVolume * 1.30f).toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return buffer
    }

    private fun playTone(freq: Int, durationMs: Int, volume: Float) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val wave = sin(2.0 * PI * freq * t)
                val envelope = 1.0 - (i.toDouble() / numSamples)
                buffer[i] = (wave * envelope * volume * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playSweep(startFreq: Int, endFreq: Int, durationMs: Int, volume: Float) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * PI * currentFreq / sampleRate
                val wave = sin(phase)
                val envelope = when {
                    progress < 0.1 -> progress / 0.1
                    else -> 1.0 - progress
                }
                buffer[i] = (wave * envelope * volume * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer)
        } catch (_: Exception) {}
    }

    private fun playSynthesizedNote(freq: Int, durationMs: Int, hasDrum: Boolean) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                var wave = sin(2.0 * PI * freq * t) * 0.45 + sin(4.0 * PI * freq * t) * 0.15

                if (hasDrum && i < numSamples / 2) {
                    val drumDecay = 1.0 - (i.toDouble() / (numSamples / 2))
                    val drumWave = sin(2.0 * PI * 90.0 * (1.0 - drumDecay * 0.3) * t) * drumDecay * 0.4
                    wave += drumWave
                }

                val envelope = when {
                    i < numSamples * 0.1 -> i / (numSamples * 0.1)
                    else -> 1.0 - ((i - numSamples * 0.1) / (numSamples * 0.9))
                }

                buffer[i] = (wave * envelope * (musicVolume * 8000.0)).toInt().coerceIn(-32767, 32767).toShort()
            }
            playPcm(buffer, isMusic = true)
        } catch (_: Exception) {}
    }

    private fun playPcm(buffer: ShortArray, isMusic: Boolean = false) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(if (isMusic) AudioAttributes.CONTENT_TYPE_MUSIC else AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

            track.setVolume(if (isMusic) 1.0f else 1.0f)
            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                delay((buffer.size * 1000L / sampleRate) + 50)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun triggerHaptic(durationMs: Long) {
        if (!vibrationEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stopBackgroundMusic()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
