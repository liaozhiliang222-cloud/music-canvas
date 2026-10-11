package com.musiccanvas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.musiccanvas.app.ui.AppViewModel
import com.musiccanvas.app.ui.screens.HomeScreen
import com.musiccanvas.app.ui.screens.SettingsScreen
import com.musiccanvas.app.ui.screens.SourcesScreen
import com.musiccanvas.app.ui.screens.ThemesScreen
import com.musiccanvas.app.ui.screens.UnlockScreen
import com.musiccanvas.app.ui.theme.MusicCanvasTheme

class MainActivity : ComponentActivity() {

    private val viewModel by lazy { AppViewModel(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicCanvasTheme {
                AppNav(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onForeground()
    }

    override fun onPause() {
        viewModel.onBackground()
        super.onPause()
    }
}

@Composable
private fun AppNav(viewModel: AppViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenSources = { nav.navigate("sources") },
                onOpenThemes = { nav.navigate("themes") },
                onOpenSettings = { nav.navigate("settings") },
                onOpenUnlock = { nav.navigate("unlock") },
                context = nav.context,
            )
        }
        composable("sources") {
            SourcesScreen(viewModel = viewModel, onBack = { nav.popBackStack() })
        }
        composable("themes") {
            ThemesScreen(
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
                onOpenUnlock = { nav.navigate("unlock") },
            )
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel, onBack = { nav.popBackStack() })
        }
        composable("unlock") {
            UnlockScreen(onBack = { nav.popBackStack() })
        }
    }
}
