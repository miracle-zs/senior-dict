package com.seniordict.app.data.preferences

import android.content.Context
import android.content.SharedPreferences

class SeniorPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("senior_dict_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SPEECH_RATE = "pref_speech_rate"
        const val KEY_ENABLE_MNEMONIC = "pref_enable_mnemonic"
        const val KEY_ENABLE_HAPTIC = "pref_enable_haptic"

        const val RATE_VERY_SLOW = 0.60f
        const val RATE_MODERATE = 0.80f
        const val RATE_NORMAL = 1.00f
    }

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, RATE_MODERATE)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var isMnemonicEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_MNEMONIC, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_MNEMONIC, value).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_HAPTIC, value).apply()
}
