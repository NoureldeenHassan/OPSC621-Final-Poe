package com.example.ebird

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ebird.MyApplication.Companion.auth
import com.example.ebird.navigation.NavigationItem
import com.example.ebird.screens.SignInScreen
import com.example.ebird.screens.SignUpScreen
import com.example.ebird.screens.ui.theme.EBirdTheme
import com.example.ebird.utils.userEmail
import com.example.ebird.utils.userName

class AuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EBirdTheme {

                if(auth.currentUser!=null){
                    startActivity(Intent(this,MainActivity::class.java))
                    userName = auth.currentUser?.displayName.toString()
                    userEmail =auth.currentUser?.email.toString()
                }
                else{
                    val navController = rememberNavController()
                    NavApp(navController)
                }
            }
        }
    }
    @Composable
    private fun NavApp(navController: NavHostController) {
        NavHost(
            navController = navController,
            startDestination = NavigationItem.SignUpScreen.route
        ) {
            composable(NavigationItem.SignUpScreen.route) {
                SignUpScreen(auth, navController)
            }
            composable(NavigationItem.SignInScreen.route) {
                SignInScreen(auth, navController)
            }
        }
    }
}