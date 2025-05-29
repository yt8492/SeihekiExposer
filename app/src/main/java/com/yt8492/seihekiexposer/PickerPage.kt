package com.yt8492.seihekiexposer

import android.content.Intent
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PickerPage() {
  val context = LocalContext.current
  val contentResolver = remember {
    context.contentResolver
  }
  val (directories, setDirectories) = remember {
    mutableStateOf(emptyList<String>())
  }
  val client = remember {
    HttpClient(OkHttp) {
      install(WebSockets)
      install(ContentNegotiation)
    }
  }
  val coroutineScope = rememberCoroutineScope()
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    uri?.let {
      contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
      DocumentFile.fromTreeUri(context, uri)?.let { documentFile ->
        documentFile.listFiles().forEach { file ->
          if (file.isDirectory) {
            file.name?.let {
              setDirectories(directories + it)
            }
          }
        }
      }
      coroutineScope.launch(Dispatchers.IO) {
        client.ws("ws://10.0.2.2:8080/ws") {
          // Receive a response from the WebSocket server
          while (true) {
            val frame = incoming.receive()
            if (frame is Frame.Text && frame.readText() == "get") {
              val response = with(Dispatchers.Main) {
                DocumentFile.fromTreeUri(context, uri)
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
    }
  }
  LaunchedEffect(Unit) {
    launcher.launch(null)
  }
  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      directories.forEach {
        Text(text = it)
      }
    }
  }
}