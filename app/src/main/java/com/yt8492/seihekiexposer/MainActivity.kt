package com.yt8492.seihekiexposer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yt8492.seihekiexposer.ui.theme.SeihekiExposerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SeihekiExposerTheme {
        PickerPage()
      }
    }
  }
}
