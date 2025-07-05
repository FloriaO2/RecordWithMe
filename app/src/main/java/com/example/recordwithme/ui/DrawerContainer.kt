package com.example.recordwithme.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recordwithme.AuthViewModel

@Composable
fun DrawerContainer(
    drawerOpen: Boolean,
    onDrawerClose: () -> Unit,
    authViewModel: AuthViewModel? = null,
    content: @Composable () -> Unit
) {
    Box {
        // 메인 콘텐츠
        content()
        
        // 드로워 + 배경을 함께 슬라이드
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ),
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopEnd)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 반투명 오버레이
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable { onDrawerClose() }
                )

                // 드로워 본체
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(220.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.White)
                ) {
                    AppDrawer(onDrawerClose = onDrawerClose, authViewModel = authViewModel)
                }
            }
        }
    }
} 