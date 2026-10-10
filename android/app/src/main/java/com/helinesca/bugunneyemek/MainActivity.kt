package com.helinesca.bugunneyemek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helinesca.bugunneyemek.ui.AboutScreen
import com.helinesca.bugunneyemek.ui.AppTheme
import com.helinesca.bugunneyemek.ui.AppViewModel
import com.helinesca.bugunneyemek.ui.AtHomeResultsScreen
import com.helinesca.bugunneyemek.ui.AtHomeScreen
import com.helinesca.bugunneyemek.ui.BrowseScreen
import com.helinesca.bugunneyemek.ui.EditScreen
import com.helinesca.bugunneyemek.ui.HistoryScreen
import com.helinesca.bugunneyemek.ui.HomeScreen
import com.helinesca.bugunneyemek.ui.OnboardingScreen
import com.helinesca.bugunneyemek.ui.RecipeScreen
import com.helinesca.bugunneyemek.ui.ResultsScreen
import com.helinesca.bugunneyemek.ui.Screen
import com.helinesca.bugunneyemek.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val vm: AppViewModel = viewModel()
                BackHandler(enabled = vm.nav.size > 1) { vm.back() }
                when (val s = vm.screen) {
                    Screen.Onboarding -> OnboardingScreen(vm)
                    Screen.Home -> HomeScreen(vm)
                    Screen.Results -> ResultsScreen(vm)
                    Screen.AtHome -> AtHomeScreen(vm)
                    Screen.AtHomeResults -> AtHomeResultsScreen(vm)
                    is Screen.Recipe -> RecipeScreen(vm, s.dishId)
                    Screen.Browse -> BrowseScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                    is Screen.Edit -> EditScreen(vm, s.what)
                    Screen.History -> HistoryScreen(vm)
                    Screen.About -> AboutScreen(vm)
                }
            }
        }
    }
}
