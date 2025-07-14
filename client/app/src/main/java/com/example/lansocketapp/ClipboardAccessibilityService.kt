package com.example.lansocketapp

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ClipboardAccessibilityService : AccessibilityService() {

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var socketManager: WebSocketManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ClipboardAccessibilityService", "Accessibility Service connected")

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val url = getSharedPreferences("LANSocketPrefs", MODE_PRIVATE)
            .getString("websocket_url", null)

        if (url != null) {
            socketManager = WebSocketManager(applicationContext, clipboardManager)
            socketManager.url = url
            socketManager.startWebSocket()
            startClipboardMonitoring(applicationContext, socketManager, clipboardManager)
        } else {
            Log.e("ClipboardAccessibilityService", "No WebSocket URL found")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            Log.d("onAccessibilityEvent", "clipboard event recieved")
            val clipData = clipboardManager.primaryClip
            val copiedText = clipData?.getItemAt(0)?.text?.toString()

            if (!copiedText.isNullOrEmpty()) {
                Log.d("ClipboardAccessibilityService", "Clipboard changed: $copiedText")
//                socketManager.sendClipboard(copiedText)
            }
        }
    }

    override fun onInterrupt() {
        Log.w("ClipboardAccessibilityService", "Service interrupted")
    }
}
