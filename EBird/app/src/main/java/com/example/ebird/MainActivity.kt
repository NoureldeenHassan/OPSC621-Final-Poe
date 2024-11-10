package com.example.ebird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ebird.model.ObservationModel
import com.example.ebird.navigation.NavigationItem
import com.example.ebird.screens.BirdsGalleryScreen
import com.example.ebird.screens.BirdsListScreen
import com.example.ebird.screens.CurrentLocationMapView
import com.example.ebird.screens.FavBirdsScreen
import com.example.ebird.screens.SettingScreenLayout
import com.example.ebird.screens.SignInScreen
import com.example.ebird.screens.SignUpScreen
import com.example.ebird.ui.theme.EBirdTheme
import com.example.ebird.viewmodel.BirdObservationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    private val birdObservationViewModel: BirdObservationViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EBirdTheme {

                val navController = rememberNavController()
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Birds App") }) },
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    NavApp(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    private fun NavApp(navController: NavHostController, modifier: Modifier = Modifier) {
        NavHost(
            navController = navController,
            startDestination = NavigationItem.BirdsScreen.route,
            modifier = modifier
        ) {
            composable(
                "${NavigationItem.GoogleMap.route}?selectedBird={selectedBird}",
                arguments = listOf(
                    navArgument(
                        name = "selectedBird"
                    ) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                )
            ) {
                val selectedBirdJsonString = it.arguments?.getString("selectedBird")
                val selectedBird =
                    Gson().fromJson(selectedBirdJsonString, ObservationModel::class.java)
                CurrentLocationMapView(modifier = Modifier.fillMaxSize(), selectedBird)
            }
            composable(NavigationItem.BirdsScreen.route) {
                BirdsListScreen(navController, birdObservationViewModel)
            }
            composable(NavigationItem.BirdsGalleryScreen.route) {
                BirdsGalleryScreen(navController)
            }
            composable(NavigationItem.FavBirds.route) {
             FavBirdsScreen(navController)
            }
            composable(NavigationItem.SettingsScreen.route) {
                SettingScreenLayout()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem.BirdsScreen,
        NavigationItem.BirdsGalleryScreen,
        NavigationItem.FavBirds,
        NavigationItem.SettingsScreen
    )

    NavigationBar {
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (item) {
                            NavigationItem.BirdsScreen -> Icons.Filled.Home
                            NavigationItem.SettingsScreen -> Icons.Filled.Settings
                            NavigationItem.GoogleMap -> TODO()
                            NavigationItem.SignInScreen -> TODO()
                            NavigationItem.SignUpScreen -> TODO()
                            NavigationItem.BirdsGalleryScreen -> Icons.Filled.Favorite
                            NavigationItem.FavBirds -> Icons.Filled.Star
                        },
                        contentDescription = item.route.toString()
                    )
                },
                label = { Text(text = item.route) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            restoreState = true
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}




