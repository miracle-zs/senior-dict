package com.seniordict.app

import android.app.Application
import com.seniordict.app.data.db.SeniorDictDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeniorDictApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-warm Room database in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SeniorDictDatabase.getDatabase(this@SeniorDictApplication).dictDao().getCharacter("柏")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
