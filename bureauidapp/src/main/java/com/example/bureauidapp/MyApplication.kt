package com.example.bureauidapp

import android.app.Application
import android.os.SystemClock
import com.bureau.base.Environment
import com.bureau.base.models.BureauConfig
import com.bureau.devicefingerprint.BureauAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MyApplication : Application() {
    companion object {
        @Volatile
        var sdkInitCallDurationMs: Double? = null
            private set

        @Volatile
        var sdkInitReadyDurationMs: Long? = null
            private set

        @Volatile
        var sdkIsInitialized: Boolean? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val initStartTimeMs = SystemClock.elapsedRealtime()
        val initCallStartTimeNs = SystemClock.elapsedRealtimeNanos()
        BureauAPI.init(
            BureauConfig(
                credentialId = getString(R.string.credential_id),
                environment = Environment.ENV_PRODUCTION, // Switch to ENV_PRODUCTION for production
                application = this
            )
        )
        sdkInitCallDurationMs = (SystemClock.elapsedRealtimeNanos() - initCallStartTimeNs) / 1_000_000.0
        sdkIsInitialized = BureauAPI.isInitialized()
        if (sdkIsInitialized == true) {
            sdkInitReadyDurationMs = SystemClock.elapsedRealtime() - initStartTimeMs
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                while (!BureauAPI.isInitialized()) {
                    delay(10.milliseconds)
                }
                sdkInitReadyDurationMs = SystemClock.elapsedRealtime() - initStartTimeMs
                sdkIsInitialized = true
            }
        }
    }
}