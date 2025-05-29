package com.yt8492.seihekiexposer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.documentfile.provider.DocumentFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.ws
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WebSocketService : Service() {

  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val client = HttpClient(OkHttp) {
    install(WebSockets)
    install(ContentNegotiation)
  }

  override fun onBind(intent: Intent): IBinder {
    error("Not implemented")
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    val uri = intent.data ?: error("URI is required")
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
      notificationManager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID,
          CHANNEL_NAME,
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = CHANNEL_DESCRIPTION
        },
      )
    }
    val notification = Notification.Builder(this, CHANNEL_ID)
      .setContentTitle(NOTIFICATION_TITLE)
      .setContentText(NOTIFICATION_TEXT)
      .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your actual icon
      .build()
    ServiceCompat.startForeground(
      this,
      NOTIFICATION_ID,
      notification,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      } else {
        0
      },
    )
    coroutineScope.launch {
      client.ws("ws://10.0.2.2:8080/ws") {
        // Receive a response from the WebSocket server
        while (true) {
          val frame = incoming.receive()
          if (frame is Frame.Text && frame.readText() == "get") {
            val response = with(Dispatchers.Main) {
              DocumentFile.fromTreeUri(this@WebSocketService, uri)
                ?.listFiles()
                ?.filter { it.isDirectory && it.name?.startsWith(".") == false }
                ?.joinToString(
                  separator = ",",
                  prefix = "{",
                  postfix = "}",
                ) {
                  "\"${it.name}\""
                }
                ?: "{}"
            }
            // Send the response back to the WebSocket server
            outgoing.send(Frame.Text(response))
          }
        }
      }
    }
    return Service.START_STICKY
  }

  companion object {
    private const val CHANNEL_ID = "WebSocketServiceChannel"
    private const val CHANNEL_NAME = "起動通知"
    private const val CHANNEL_DESCRIPTION = "起動通知のチャンネルです。"
    private const val NOTIFICATION_ID = 1
    private const val NOTIFICATION_TITLE = "WebSocket Service"
    private const val NOTIFICATION_TEXT = "WebSocket Service is running"
  }
}