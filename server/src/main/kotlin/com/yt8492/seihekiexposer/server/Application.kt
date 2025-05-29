package com.yt8492.seihekiexposer.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun main() {
    embeddedServer(Netty, port = 8080) {
      val mutex = Mutex()
      var wsConnection: WebSocketSession? = null

      install(WebSockets)

      routing {
        // WebSocketエンドポイント
        webSocket("/ws") {
          mutex.withLock {
            wsConnection?.close()
            wsConnection = this
          }
        }

        // HTTP GETエンドポイント
        get("/sh") {
          val connection = mutex.withLock { wsConnection }
          if (connection == null) {
            call.respondText("WebSocket not connected", status = HttpStatusCode.ServiceUnavailable)
            return@get
          }

          try {
            connection.send(Frame.Text("get"))
            val responseText = (connection.incoming.receive() as? Frame.Text)?.readText()
            if (responseText != null) {
              call.respondText(responseText, contentType = ContentType.Application.Json)
            } else {
              call.respondText("No response from WebSocket", status = HttpStatusCode.InternalServerError)
            }
          } catch (e: Exception) {
            mutex.withLock {
              connection.close()
              wsConnection = null
            }
            call.respondText("WebSocket error", status = HttpStatusCode.InternalServerError)
          }
        }
      }
    }
}
