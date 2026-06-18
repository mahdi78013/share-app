package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.SharingRepository
import com.example.ui.MainDashboard
import com.example.ui.ShareViewModel
import com.example.ui.ShareViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val database = remember { AppDatabase.getDatabase(context.applicationContext) }
                val repository = remember { SharingRepository(database.sharingDao()) }
                
                // Inject our custom Viewmodel with factory bindings
                val shareViewModel: ShareViewModel = viewModel(
                    factory = ShareViewModelFactory(
                        application = context.applicationContext as Application,
                        repository = repository
                    )
                )

                MainDashboard(
                    viewModel = shareViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
