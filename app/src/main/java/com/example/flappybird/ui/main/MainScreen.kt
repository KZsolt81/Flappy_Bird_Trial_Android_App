package com.example.flappybird.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.flappybird.data.DefaultDataRepository
import com.example.flappybird.theme.FlappyBirdTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (state) {
    MainScreenUiState.Loading -> {
      // Blank
    }
    is MainScreenUiState.Success -> {
      MainScreen(data = (state as MainScreenUiState.Success).data, modifier = modifier)
    }
    is MainScreenUiState.Error -> {
      Text("Error loading data: ${(state as MainScreenUiState.Error).throwable.message}")
    }
  }
}

@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
  FlappyBirdGame(modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
  FlappyBirdTheme { MainScreen(listOf("Android")) }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun MainScreenPortraitPreview() {
  FlappyBirdTheme { MainScreen(listOf("Android")) }
}
