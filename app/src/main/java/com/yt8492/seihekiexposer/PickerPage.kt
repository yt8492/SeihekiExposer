package com.yt8492.seihekiexposer

import android.content.Intent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile

@Composable
fun PickerPage() {
  val context = LocalContext.current
  val contentResolver = remember {
    context.contentResolver
  }
  val (directories, setDirectories) = remember {
    mutableStateOf(emptyList<String>())
  }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    uri?.let {
      contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
      val intent = Intent(context, WebSocketService::class.java).apply {
        data = uri
      }
      ContextCompat.startForegroundService(context, intent)
      DocumentFile.fromTreeUri(context, uri)?.let { documentFile ->
        documentFile.listFiles().forEach { file ->
          if (file.isDirectory) {
            file.name?.let {
              setDirectories(directories + it)
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