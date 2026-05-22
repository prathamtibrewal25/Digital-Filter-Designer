package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.FilterDatabase
import com.example.data.FilterRepository
import com.example.ui.screens.FilterAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FilterViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Let's configure the edge-to-edge full bleed drawing support
        enableEdgeToEdge()

        // 1. Initialize local cache Room database & repository
        val database = FilterDatabase.getDatabase(applicationContext)
        val repository = FilterRepository(database.filterDao())

        // 2. Instantiate our ViewModel using our custom Provider Factory
        val viewModel: FilterViewModel by viewModels {
            FilterViewModel.provideFactory(repository)
        }

        // 3. Mount the modern Compose application screen layout
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FilterAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
