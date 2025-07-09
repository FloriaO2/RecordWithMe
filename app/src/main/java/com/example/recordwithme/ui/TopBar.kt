package com.example.recordwithme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recordwithme.R

enum class TopBarIconType { Menu, Home }

@Composable
fun TopBar(onMenuClick: () -> Unit, iconType: TopBarIconType = TopBarIconType.Menu) {
    Surface(
        color = Color.White, // 배경 투명
        elevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp) // TopBar 높이 넉넉하게
        ) {
            // 1. 곡선 위쪽만 파란색으로 채우는 Canvas (맨 아래)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(110.dp) // Box와 동일하게
            ) {
                val width = size.width
                val height = size.height

                // 곡선 Path 정의 (TopBar 전체를 파란색으로 채우고, 아래쪽만 곡선)
                val curvePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width, 0f)
                    lineTo(width, height)
                    cubicTo(
                        width * 0.75f, height * 0.7f,
                        width * 0.25f, height * 0.7f,
                        0f, height
                    )
                    close()
                }

                // 곡선 위쪽을 TopBar 배경색으로 그라데이션 채우기
                drawPath(
                    path = curvePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0666B0), // 위쪽: 원래 색상
                            Color(0xFF72B6EF)  // 아래쪽: 살짝 연한 파랑
                        )
                    )
                )

                // 곡선 실선 다시 그리기 (맨 아래 곡선 경계)
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, height)
                        cubicTo(
                            width * 0.25f, height * 0.7f,
                            width * 0.75f, height * 0.7f,
                            width, height
                        )
                    },
                    color = Color(0xB7051A52), //곡선 색상
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                )
            }

            // 2. 나머지 UI는 Canvas 위에 배치
            // 왼쪽: 로고
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, bottom = 16.dp),
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
                fontSize = 22.sp,
                color = Color(0xFFFFFFFF), // 진한 갈색 텍스트
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 16.dp)
            )

            // 오른쪽: 메뉴/홈 버튼
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = 16.dp)
            ) {
                if (iconType == TopBarIconType.Home) {
                    Icon(Icons.Default.Home, contentDescription = "홈", tint = Color(0xFFFFFFFF), modifier = Modifier.size(30.dp))
                } else {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = Color(0xFFFFFFFF), modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}
