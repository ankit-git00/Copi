package com.example.lansocketapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lansocketapp.ui.theme.LANSocketAppTheme
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentIntegrator.*
import com.google.zxing.integration.android.IntentResult
import java.net.URL
import androidx.core.content.edit


var ignoreNextClipboardChange = false
var lastClipboardUpdateTime: Long = 0L


class MainActivity : ComponentActivity() {

    private var openedFromNotification: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LANSocketAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SelectionContainer  { // 👈 Enables text selection and copy
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding),
                            {startQRCodeScanner()}
                        )
                    }
                }
            }
        }

        Log.d("MainActivity", "MainActivity OnCreate")
        Log.d("MainActivity", "Intent Extras: ${intent.extras}")


        openedFromNotification = intent.getBooleanExtra("opened_from_notification", false)
        if (openedFromNotification) {
            Log.d("MainActivity", "Launched from notification")
//            handleClipboardSyncIfNeeded()
        }

//        if (!isAccessibilityServiceEnabled(this, "com.example.lansocketapp/.ClipboardAccessibilityService")) {
//            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
//            startActivity(intent)
//        }

    }




    private fun startQRCodeScanner() {
        val integrator = IntentIntegrator(this).apply {
            setPrompt("Scan the clipboard sync QR")
            setOrientationLocked(true)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            captureActivity = CaptureActivityPortrait::class.java
        }
        qrScanLauncher.launch(integrator.createScanIntent())
    }


    private val qrScanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val intentResult = parseActivityResult(
                result.resultCode,
                result.data
            )

            if (intentResult != null && intentResult.contents != null) {
                val qrContent = intentResult.contents  // Example: ws://192.168.1.10:8765?token=abc123
                Toast.makeText(this, "Scanned: $qrContent", Toast.LENGTH_SHORT).show()
                Log.d("qrScanLauncher", qrContent)


                val sharedPrefs = getSharedPreferences("LANSocketPrefs", MODE_PRIVATE)
                sharedPrefs.edit { putString("websocket_url", qrContent) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            1001
                        )
                    }
                }

                // restart foreground service
                val stopIntent = Intent(this, ClipboardSyncService::class.java)
                stopService(stopIntent)

                val startIntent = Intent(this, ClipboardSyncService::class.java)
                ContextCompat.startForegroundService(this, startIntent)

            } else {
                Toast.makeText(this, "QR Scan cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    fun isAccessibilityServiceEnabled(context: Context, serviceId: String): Boolean {
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(serviceId)
    }





}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    onScanClicked: () -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Hello $name!")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onScanClicked) {
            Text("📷 Scan QR Code to Connect")
            }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    LANSocketAppTheme {
//        Greeting("Android")
//    }
//}

fun startClipboardMonitoring(context: Context, socketManager: WebSocketManager, clipboardManager: ClipboardManager) {
    val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        monitorClipboard(context, socketManager, clipboardManager)
    }

    // Register the listener
    clipboardManager.addPrimaryClipChangedListener(clipboardListener)
}

var lastClipboardText: String? = null

fun monitorClipboard(context: Context, socketManager: WebSocketManager, clipboardManager: ClipboardManager) {
    if (ignoreNextClipboardChange) {
        Log.d("MonitorClipboard", "Ignoring Change, text received from websocket")
        ignoreNextClipboardChange = false
        return
    }

    val clipText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
    val label = clipboardManager.primaryClipDescription?.label
    val currentTime = System.currentTimeMillis()

    // Minimum time difference in milliseconds (e.g., 1000 ms = 1 second)
    val MIN_INTERVAL = 1000


    if (!clipText.isNullOrEmpty()
//        && clipText != lastClipboardText
        && label != "WebSocket"
        && (currentTime - lastClipboardUpdateTime > MIN_INTERVAL)
    ) {
        Log.d("MonitorClipboard", "label : $label")
        Log.d("MonitorClipboard", "Change detected: $clipText")

        lastClipboardText = clipText
        lastClipboardUpdateTime = currentTime

        socketManager.sendMessage(clipText)
    } else {
        Log.d("MonitorClipboard", "Duplicate or frequent clipboard change ignored.")
    }

}



fun copyToClipboardFromSocket(context: Context, text : String, clipboard: ClipboardManager){
    try {
        val clipData = ClipData.newPlainText("WebSocket", text)
        ignoreNextClipboardChange = true
        clipboard.setPrimaryClip(clipData)
        Log.d("copyToClipboardFromSocket", "Updated Clipboard with text: $text")

        val latestLabel = clipboard.primaryClipDescription?.label
        Log.d("copyToClipboardFromSocket", "Clipboard verification - label: $latestLabel")

        val latestClip = clipboard.primaryClip
        val latestText = latestClip?.getItemAt(0)?.text
        Log.d("copyToClipboardFromSocket", "Clipboard verification: $latestText")

        Handler(Looper.getMainLooper()).post{
            Toast.makeText(context,"Clipboard updated:\n$text", Toast.LENGTH_SHORT).show()
        }


    }
    catch (e: Exception) {
        Log.e("WebSocket", "Failed to update clipboard: ${e.message}")
    }

}



