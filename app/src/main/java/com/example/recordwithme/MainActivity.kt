package com.example.recordwithme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.recordwithme.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            var drawerOpen by remember { mutableStateOf(false) }

            DrawerContainer(
                drawerOpen = drawerOpen,
                onDrawerClose = { drawerOpen = false }
            ) {
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
                            startDestination = "home"
                        ) {
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
