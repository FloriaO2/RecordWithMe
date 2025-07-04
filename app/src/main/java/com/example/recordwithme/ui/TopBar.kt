package com.example.recordwithme.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.recordwithme.R

@Composable
fun TopBar(onMenuClick: () -> Unit) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.background,
        elevation = 0.dp,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 왼쪽 로고
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "앱 로고",
                    modifier = Modifier
                        .height(40.dp)
                )

                // 오른쪽 메뉴 버튼
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴")
                }
            }
        },
        navigationIcon = null, // 왼쪽 메뉴 아이콘 제거
        actions = {} // 오른쪽 actions 제거
    )
}
