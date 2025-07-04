package com.example.recordwithme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.recordwithme.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            var drawerOpen by remember { mutableStateOf(false) }
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            DrawerContainer(
                drawerOpen = drawerOpen,
                onDrawerClose = { drawerOpen = false }
            ) {
                if (currentRoute == "login" || currentRoute == "signup") {
                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                navController = navController
                            )
                        }
                        composable("signup") {
                            SignUpScreen(navController = navController)
                        }
                        composable("home") { HomeScreen() }
                        composable("profile") { ProfileScreen() }
                        composable("notification") { NotificationScreen() }
                        composable("group") { GroupScreen() }
                    }
                } else {
                    Scaffold(
                        topBar = {
                            TopBar(onMenuClick = { drawerOpen = !drawerOpen })
                        },
                        bottomBar = {
                            BottomNavigationBar(navController)
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            NavHost(
                                navController = navController,
                                startDestination = "login"
                            ) {
                                composable("login") {
                                    LoginScreen(
                                        onLoginSuccess = {
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        },
                                        navController = navController
                                    )
                                }
                                composable("signup") {
                                    SignUpScreen(navController = navController)
                                }
                                composable("home") { HomeScreen() }
                                composable("profile") { ProfileScreen() }
                                composable("notification") { NotificationScreen() }
                                composable("group") { GroupScreen() }
                            }
                        }
                    }
                }
            }
        }
    }
}
