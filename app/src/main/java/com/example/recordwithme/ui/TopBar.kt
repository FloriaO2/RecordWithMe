package com.example.recordwithme.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recordwithme.R

@Composable
fun TopBar(onMenuClick: () -> Unit) {
    Surface(
        color = Color(0xFF1A7BCF), // 배경
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp) // 탑바 높이
        ) {
            // 왼쪽: 로고
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "앱 로고",
                    modifier = Modifier.height(50.dp)
                )
            }

            // 가운데: 텍스트
            Text(
                text = "RecordWithMe",
                fontSize = 20.sp,
                color = Color(0xFFFFFFFF), // 진한 갈색 텍스트
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )

            // 오른쪽: 메뉴 버튼
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = Color(0xFFFFFFFF)) // 진한 갈색 아이콘
            }
        }
    }
}
