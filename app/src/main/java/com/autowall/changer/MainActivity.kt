package com.autowall.changer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autowall.changer.ui.HomeScreen
import com.autowall.changer.ui.HomeViewModel
import com.autowall.changer.ui.theme.AutoWallTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            AutoWallTheme {
                val viewModel: HomeViewModel = viewModel()
                HomeScreen(viewModel)
            }
        }
    }
}
