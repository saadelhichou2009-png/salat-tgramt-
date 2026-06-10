package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.AdhanViewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.AdhanTaghramtTheme

class MainActivity : ComponentActivity() {
  private val viewModel: AdhanViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AdhanTaghramtTheme {
        MainAppScreen(viewModel = viewModel)
      }
    }
  }
}
