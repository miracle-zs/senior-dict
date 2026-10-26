package com.seniordict.app.engine.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.seniordict.app.data.preferences.SeniorPreferences
import java.util.Locale

class AudioSpeaker(
    private val context: Context,
    private val preferences: SeniorPreferences = SeniorPreferences(context)
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("AudioSpeaker", "Chinese language not supported by current TTS engine")
                tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
            }
            applySpeechRate(preferences.speechRate)
            tts?.setPitch(1.0f)
            isInitialized = true
        } else {
            Log.e("AudioSpeaker", "TextToSpeech initialization failed with status: $status")
        }
    }

    fun applySpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    /**
     * Provide gentle haptic confirmation when mom taps on a character box
     */
    fun triggerHapticFeedback() {
        if (!preferences.isHapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (e: Exception) {
            Log.e("AudioSpeaker", "Vibration failed", e)
        }
    }

    /**
     * Speak character with clear pronunciation and mnemonic word.
     * e.g., "柏，拼音是 bǎi，柏树的柏"
     */
    fun speakCharacter(char: String, pinyin: String, examples: String?) {
        triggerHapticFeedback()

        val sampleWord = examples?.split("、", ",")?.firstOrNull()?.trim() ?: ""
        val speechText = when {
            preferences.isMnemonicEnabled && sampleWord.isNotBlank() && sampleWord.contains(char) && pinyin.isNotBlank() ->
                "${char}，拼音是 ${pinyin}，${sampleWord}的${char}。"
            pinyin.isNotBlank() ->
                "${char}，拼音是 ${pinyin}。"
            sampleWord.isNotBlank() && sampleWord.contains(char) ->
                "${char}，${sampleWord}的${char}。"
            else ->
                "${char}。"
        }

        speak(speechText)
    }

    /**
     * Speak full definition in plain Chinese
     */
    fun speakText(text: String) {
        triggerHapticFeedback()
        speak(text)
    }

    /**
     * Detect if system media volume is muted or too low, and gently boost for seniors.
     */
    private fun ensureAdequateVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVolume > 0 && (currentVolume == 0 || (currentVolume.toFloat() / maxVolume) < 0.25f)) {
                val targetVolume = (maxVolume * 0.7f).toInt().coerceAtLeast(1)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context.applicationContext, "已为您调大朗读音量 🔊", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioSpeaker", "Volume adjustment failed", e)
        }
    }

    private fun speak(text: String) {
        ensureAdequateVolume()
        if (!isInitialized || tts == null) {
            Log.w("AudioSpeaker", "TTS not ready yet")
            return
        }
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SENIOR_DICT_SPEECH")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("AudioSpeaker", "TTS shutdown error", e)
        }
    }
}
