package com.example.lansocketapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService

class ClipboardSyncService: Service() {
    private lateinit var socketManager: WebSocketManager
    private lateinit var clipboardManager: ClipboardManager
    private val CHANNEL_ID = "clipboard_sync_channel"

    companion object {
        const val ACTION_NOTIFICATION_CLICKED = "com.example.lansocketapp.ACTION_NOTIFICATION_CLICKED"
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val sharedPreferences = getSharedPreferences("LANSocketPrefs", MODE_PRIVATE)
        val url = sharedPreferences.getString("websocket_url", null)
        Log.d("ClipboardSyncService", "Trying to connect to $url")

        if (url != null) {
            socketManager = WebSocketManager(applicationContext, clipboardManager)
            socketManager.url = url
            socketManager.startWebSocket()
            startClipboardMonitoring(applicationContext, socketManager, clipboardManager)
            Log.d("ClipboardSyncService", "Started the service")

        } else {
            Log.e("ClipboardSyncService", "WebSocket url not found in shared preferences")
        }
        startForegroundService()
    }


    private fun startForegroundService() {




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Sync Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }


        val clickIntent = Intent(this, ClipboardSyncService::class.java).apply {
            action = ACTION_NOTIFICATION_CLICKED
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Clipboard Sync Running")
            .setContentText("Syncing clipboard in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent) // 👈 this now points to the Service itself
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_NOTIFICATION_CLICKED) {
            Log.d("ClipboardSyncService", "Notification clicked. Handling logic here.")
            // 🔁 Handle your sync logic, or show a toast/log, or anything else
            handleClipboardSyncIfNeeded()

            Toast.makeText(this, "Notification clicked", Toast.LENGTH_SHORT).show()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ClipboardSyncService", "Service stopped.")
    }

    private fun handleClipboardSyncIfNeeded() {
        if (!clipboardManager.hasPrimaryClip()) {
            Log.w("ClipboardSyncService", "Clipboard is empty")
            return
        }

        val clipData = clipboardManager.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val item = clipData.getItemAt(0)

            val clipText = item.text?.toString()?.trim()

            Log.d("ClipboardSyncService", "Extracted clipboard: $clipText")

            if (!clipText.isNullOrEmpty()) {
                socketManager.sendMessage(clipText)
                Log.d("ClipboardSyncService", "Sent clipboard: $clipText")
            } else {
                Log.w("ClipboardSyncService", "Clipboard content is not text or is empty.")
            }
        } else {
            Log.w("ClipboardSyncService", "Clipboard has no items")
        }
    }


}
