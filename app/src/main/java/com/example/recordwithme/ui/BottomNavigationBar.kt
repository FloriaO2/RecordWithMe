package com.example.recordwithme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("홈", "home", Icons.Default.Home),
        BottomNavItem("프로필", "profile", Icons.Default.Person),
        BottomNavItem("알림", "notification", Icons.Default.Notifications),
        BottomNavItem("그룹", "group", Icons.Default.DateRange)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 알림 개수 상태
    var notificationCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    // 알림 개수 실시간 구독 (중복 제거)
    var firestoreListener by remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }
    
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val firestore = FirebaseFirestore.getInstance()
            
            // Firestore 알림만 구독 (친구 요청, 그룹 초대는 notifications 컬렉션에 포함됨)
            firestoreListener = firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .addSnapshotListener { snapshot, _ ->
                    notificationCount = snapshot?.size() ?: 0
                }
        }
    }
    
    // 리스너 정리
    DisposableEffect(currentUserId) {
        onDispose {
            firestoreListener?.remove()
        }
    }

    Column {
        Divider(color = Color(0xFFE3ECF7), thickness = 1.dp)
        BottomNavigation(backgroundColor = Color(0xFFFFFFFF), elevation = 8.dp) {
            items.forEach { item ->
                BottomNavigationItem(
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            // 그룹 버튼을 눌렀을 때는 강제로 group 라우트로 이동
                            if (item.route == "group") {
                                navController.navigate("group") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                    icon = {
                        Box {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(29.dp),
                                tint = if (currentRoute == item.route) Color(0xFF5A6CEA) else Color(0xFFB0B8C1)
                            )
                            
                            // 알림 아이콘에만 뱃지 표시
                            if (item.route == "notification" && notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.Red, CircleShape)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

data class BottomNavItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
