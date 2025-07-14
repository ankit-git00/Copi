package com.example.lansocketapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Handler
import android.os.Looper
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.w3c.dom.Text
import android.util.Log
import android.widget.Toast
import java.net.URL


class WebSocketManager(private val context : Context, public val clipboardManager: ClipboardManager) {
    private val client = OkHttpClient()
    private var shouldReconnect = false
    private val reConnectDelay : Long= 3000
    private var webSocket: WebSocket? = null
    var url : String = ""


    fun reConnect(){
        Handler(Looper.getMainLooper()).postDelayed({
            if(shouldReconnect){
                Log.d("WebSocket","Trying to reConnect to $url")
                startWebSocket()
            }
        }, reConnectDelay)
    }


    fun startWebSocket() {
//        val request = Request.Builder().url("ws://10.0.2.2:8765/").build() // for emulator
//        val request = Request.Builder().url("ws://192.168.1.10:8765?token=XFSkUfQY3L4LelwIBEeZxA").build()

        Log.d("WebSocket", "url Received : $url")

        val request = Request.Builder().url(url).build() // for emulator

        Log.d("WebSocket", "Trying to connect")
        val listener = EchoWebSocketListener(context)
        client.newWebSocket(request, listener)
    }

    fun sendMessage(text: String){
        webSocket?.send(text)
    }


    inner class EchoWebSocketListener(private val context: Context) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "WebSocket opened")
            this@WebSocketManager.webSocket = webSocket
            shouldReconnect = false
            webSocket.send("hello from android")
        }


        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket","Received: $text")
            copyToClipboardFromSocket(context,text, clipboardManager )

        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket","Closing: $code / $reason")
            shouldReconnect = true
            reConnect()
//            webSocket.close(1000, null)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocket","Error: ${t.message}")
            shouldReconnect = true
            reConnect()
        }
    }
}

