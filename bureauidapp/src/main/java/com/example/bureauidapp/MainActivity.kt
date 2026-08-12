package com.example.bureauidapp

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bureau.devicefingerprint.BureauAPI
import com.bureau.devicefingerprint.models.ErrorResponse
import com.bureau.devicefingerprint.models.SubmitResponse
import com.bureau.devicefingerprint.tools.DataCallback
import com.example.bureauidapp.ui.theme.TestOAuthTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

data class SubmitResultUi(
    val isSuccess: Boolean,
    val eventId: String?,
    val message: String?
)

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            result.forEach { (permission, granted) ->
                Log.d("BureauSDK", "Permission result: $permission = $granted")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissionsIfNeeded()
        enableEdgeToEdge()
        setContent {
            var isLoading by remember { mutableStateOf(false) }
            var sdkIsInitialized by remember { mutableStateOf(MyApplication.sdkIsInitialized) }
            var sdkInitCallDurationMs by remember { mutableStateOf(MyApplication.sdkInitCallDurationMs) }
            var sdkInitReadyDurationMs by remember { mutableStateOf(MyApplication.sdkInitReadyDurationMs) }
            var submitDurationMs by remember { mutableStateOf<Long?>(null) }
            var submitCallDurationMs by remember { mutableStateOf<Double?>(null) }
            var submitResultUi by remember { mutableStateOf<SubmitResultUi?>(null) }
            LaunchedEffect(Unit) {
                while (true) {
                    sdkIsInitialized = BureauAPI.isInitialized()
                    sdkInitCallDurationMs = MyApplication.sdkInitCallDurationMs
                    sdkInitReadyDurationMs = MyApplication.sdkInitReadyDurationMs
                    if (sdkInitReadyDurationMs != null && sdkIsInitialized == true) {
                        break
                    }
                    delay(50.milliseconds)
                }
            }
            TestOAuthTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        isLoading = isLoading,
                        sdkInitCallDurationMs = sdkInitCallDurationMs,
                        sdkInitReadyDurationMs = sdkInitReadyDurationMs,
                        sdkIsInitialized = sdkIsInitialized,
                        submitDurationMs = submitDurationMs,
                        submitCallDurationMs = submitCallDurationMs,
                        submitResultUi = submitResultUi,
                        onSubmitClick = {
                            if (!isLoading) {
                                sdkIsInitialized = BureauAPI.isInitialized()
                                isLoading = true
                                submitDurationMs = null
                                submitCallDurationMs = null
                                submitResultUi = null
                                submitCallDurationMs = submitDeviceIntelligence { elapsedMs, resultUi ->
                                    isLoading = false
                                    submitDurationMs = elapsedMs
                                    submitResultUi = resultUi
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissionsToRequest.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun submitDeviceIntelligence(onComplete: (Long, SubmitResultUi) -> Unit): Double {
        val submitStartTimeMs = SystemClock.elapsedRealtime()
        val submitCallStartTimeNs = SystemClock.elapsedRealtimeNanos()
        BureauAPI.submit(
            object : DataCallback {
                override fun onResult(message: SubmitResponse) {
                    val eventId = message.eventId
                    val responseMessage = message.message
                    Log.d("BureauSDK", "Data submitted successfully. Event ID: $eventId, Message: $responseMessage")
                    val elapsedMs = SystemClock.elapsedRealtime() - submitStartTimeMs
                    runOnUiThread {
                        onComplete(
                            elapsedMs,
                            SubmitResultUi(
                                isSuccess = true,
                                eventId = eventId,
                                message = responseMessage
                            )
                        )
                    }
                }

                override fun onError(errorMessage: ErrorResponse) {
                    val eventId = errorMessage.eventId
                    val responseMessage = errorMessage.message
                    Log.e("BureauSDK", "Error submitting data. Event ID: $eventId, Message: $responseMessage")
                    val elapsedMs = SystemClock.elapsedRealtime() - submitStartTimeMs
                    runOnUiThread {
                        onComplete(
                            elapsedMs,
                            SubmitResultUi(
                                isSuccess = false,
                                eventId = eventId,
                                message = responseMessage
                            )
                        )
                    }
                }
            }
        )
        return (SystemClock.elapsedRealtimeNanos() - submitCallStartTimeNs) / 1_000_000.0
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    sdkInitCallDurationMs: Double? = null,
    sdkInitReadyDurationMs: Long? = null,
    sdkIsInitialized: Boolean? = null,
    submitDurationMs: Long? = null,
    submitCallDurationMs: Double? = null,
    submitResultUi: SubmitResultUi? = null,
    onSubmitClick: () -> Unit = {}
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SDK init call (main thread) time: ${
                    sdkInitCallDurationMs?.let { String.format(Locale.US, "%.3f", it) } ?: "N/A"
                } ms",
                modifier = Modifier.padding(8.dp)
            )
            Text(text = "SDK init ready time: ${sdkInitReadyDurationMs ?: "N/A"} ms", modifier = Modifier.padding(8.dp))
            Text(text = "SDK initialized: ${sdkIsInitialized ?: "N/A"}", modifier = Modifier.padding(8.dp))
            if (isLoading) {
                CircularProgressIndicator()
            }
            submitResultUi?.let { result ->
                val statusText = if (result.isSuccess) "Success" else "Error"
                val fullText = "$statusText | Event ID: ${result.eventId ?: "N/A"} | Message: ${result.message ?: "N/A"}"
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = fullText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 28.dp)
                    )
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(fullText)) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_content_copy_24),
                            contentDescription = "Copy result",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            submitDurationMs?.let { elapsedMs ->
                Text(text = "Submit time: $elapsedMs ms")
            }
            submitCallDurationMs?.let { elapsedMs ->
                Text(text = "Submit call (main thread) time: ${String.format(Locale.US, "%.3f", elapsedMs)} ms")
            }
        }
        Button(
            onClick = onSubmitClick,
            enabled = !isLoading
        ) {
            Text(
                text = "Submit Device Intelligence"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TestOAuthTheme {
        Greeting("Android")
    }
}